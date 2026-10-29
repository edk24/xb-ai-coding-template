package com.hrs.admin.service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * 登录失败限流：单实例内存实现。
 * key = 小写用户名 + "|" + 客户端 IP，同一 key 15 分钟内失败 5 次锁定 15 分钟。
 */
@Service
public class LoginThrottleService {
    static final int MAX_FAILURES = 5;
    static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final long CLEANUP_INTERVAL_MILLIS = 60_000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private volatile long lastCleanupAt = System.currentTimeMillis();

    public boolean isLocked(String username, String ip) {
        String key = key(username, ip);
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }
        synchronized (attempt) {
            long now = System.currentTimeMillis();
            if (attempt.lockedUntil > now) {
                return true;
            }
            if (attempt.lockedUntil > 0) {
                // 锁定期已过，重新计数
                attempts.remove(key);
            }
            return false;
        }
    }

    public void recordFailure(String username, String ip) {
        cleanupExpired();
        Attempt attempt = attempts.computeIfAbsent(key(username, ip), ignored -> new Attempt());
        synchronized (attempt) {
            long now = System.currentTimeMillis();
            if (attempt.firstFailureAt == 0 || now - attempt.firstFailureAt > FAILURE_WINDOW.toMillis()) {
                attempt.firstFailureAt = now;
                attempt.failures = 0;
                attempt.lockedUntil = 0;
            }
            attempt.failures++;
            if (attempt.failures >= MAX_FAILURES) {
                attempt.lockedUntil = now + LOCK_DURATION.toMillis();
            }
        }
    }

    public void clear(String username, String ip) {
        attempts.remove(key(username, ip));
    }

    /**
     * 剩余锁定分钟数（不足 1 分钟按 1 分钟计），未锁定返回 0。
     */
    public long remainingMinutes(String username, String ip) {
        Attempt attempt = attempts.get(key(username, ip));
        if (attempt == null) {
            return 0;
        }
        long remainingMillis;
        synchronized (attempt) {
            remainingMillis = attempt.lockedUntil - System.currentTimeMillis();
        }
        if (remainingMillis <= 0) {
            return 0;
        }
        return Math.max(1, (remainingMillis + 59_999) / 60_000);
    }

    /**
     * 限流只信任 remoteAddr，不取 X-Forwarded-For 首个值，避免伪造请求头绕过限流。
     */
    public String clientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "" : remoteAddr;
    }

    private String key(String username, String ip) {
        return (username == null ? "" : username.toLowerCase(Locale.ROOT)) + "|" + (ip == null ? "" : ip);
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupAt < CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        lastCleanupAt = now;
        Iterator<Map.Entry<String, Attempt>> iterator = attempts.entrySet().iterator();
        while (iterator.hasNext()) {
            Attempt attempt = iterator.next().getValue();
            boolean expired;
            synchronized (attempt) {
                expired = attempt.lockedUntil > 0
                    ? attempt.lockedUntil <= now
                    : now - attempt.firstFailureAt > FAILURE_WINDOW.toMillis();
            }
            if (expired) {
                iterator.remove();
            }
        }
    }

    private static final class Attempt {
        private long firstFailureAt;
        private int failures;
        private long lockedUntil;
    }
}
