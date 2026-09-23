package com.hrs.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrs.admin.dto.ScheduledJobRequest;
import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.scheduler.ScheduledJobContext;
import com.hrs.admin.scheduler.ScheduledJobHandler;
import com.hrs.admin.vo.ScheduledJobRow;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduledJobService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final LockProvider lockProvider;
    private final LogService logService;
    private final Map<String, ScheduledJobHandler> handlers = new HashMap<>();
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final String instanceId;

    public ScheduledJobService(JdbcTemplate jdbc, ObjectMapper objectMapper, ThreadPoolTaskScheduler taskScheduler,
                               LockProvider lockProvider, List<ScheduledJobHandler> jobHandlers, LogService logService) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.taskScheduler = taskScheduler;
        this.lockProvider = lockProvider;
        this.logService = logService;
        for (ScheduledJobHandler handler : jobHandlers) {
            handlers.put(handler.jobKey(), handler);
        }
        this.instanceId = resolveInstanceId();
    }

    @PostConstruct
    public void start() {
        reloadSchedules();
    }

    public List<ScheduledJobRow> jobs() {
        return jdbc.queryForList("SELECT * FROM scheduled_jobs ORDER BY id DESC").stream()
            .map(this::jobRow)
            .toList();
    }

    public List<Map<String, Object>> logs(Long jobId) {
        if (jobId == null) {
            return jdbc.queryForList("SELECT * FROM scheduled_job_logs ORDER BY id DESC LIMIT 100");
        }
        return jdbc.queryForList("""
            SELECT * FROM scheduled_job_logs WHERE scheduled_job_id = ? ORDER BY id DESC LIMIT 100
            """, jobId);
    }

    public List<Map<String, String>> handlerOptions() {
        return handlers.values().stream()
            .map(handler -> Map.of("label", handler.name(), "value", handler.jobKey()))
            .sorted((left, right) -> left.get("value").compareTo(right.get("value")))
            .toList();
    }

    @Transactional
    public ScheduledJobRow create(ScheduledJobRequest request, Long operatorId, HttpServletRequest httpRequest) {
        validateRequest(request, null);
        jdbc.update("""
            INSERT INTO scheduled_jobs (name, code, job_key, cron_expression, params, status, allow_manual,
              lock_at_most_seconds, remark, next_run_at, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, request.getName(), request.getCode(), request.getJobKey(), request.getCronExpression(),
            nz(request.getParams()), intValue(request.getStatus(), 1), intValue(request.getAllowManual(), 1),
            intValue(request.getLockAtMostSeconds(), 600), nz(request.getRemark()), nextRunAt(request.getCronExpression()),
            operatorId, operatorId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        reloadSchedules();
        logService.operation(operatorId, "定时任务", "新增任务", httpRequest, Map.of("code", request.getCode()), Map.of("id", id));
        return find(id);
    }

    @Transactional
    public ScheduledJobRow update(Long id, ScheduledJobRequest request, Long operatorId, HttpServletRequest httpRequest) {
        requireJob(id);
        validateRequest(request, id);
        jdbc.update("""
            UPDATE scheduled_jobs SET name = ?, code = ?, job_key = ?, cron_expression = ?, params = ?, status = ?,
              allow_manual = ?, lock_at_most_seconds = ?, remark = ?, next_run_at = ?, updated_by = ?, updated_at = NOW()
            WHERE id = ?
            """, request.getName(), request.getCode(), request.getJobKey(), request.getCronExpression(),
            nz(request.getParams()), intValue(request.getStatus(), 1), intValue(request.getAllowManual(), 1),
            intValue(request.getLockAtMostSeconds(), 600), nz(request.getRemark()), nextRunAt(request.getCronExpression()),
            operatorId, id);
        reloadSchedules();
        logService.operation(operatorId, "定时任务", "编辑任务", httpRequest, Map.of("id", id), Map.of("success", true));
        return find(id);
    }

    @Transactional
    public void delete(Long id, Long operatorId, HttpServletRequest httpRequest) {
        requireJob(id);
        cancel(id);
        jdbc.update("DELETE FROM scheduled_job_logs WHERE scheduled_job_id = ?", id);
        jdbc.update("DELETE FROM scheduled_jobs WHERE id = ?", id);
        logService.operation(operatorId, "定时任务", "删除任务", httpRequest, Map.of("id", id), Map.of("success", true));
    }

    @Transactional
    public ScheduledJobRow updateStatus(Long id, Integer status, Long operatorId, HttpServletRequest httpRequest) {
        requireJob(id);
        int nextStatus = status != null && status == 1 ? 1 : 0;
        jdbc.update("""
            UPDATE scheduled_jobs SET status = ?, updated_by = ?, updated_at = NOW() WHERE id = ?
            """, nextStatus, operatorId, id);
        reloadSchedules();
        logService.operation(operatorId, "定时任务", nextStatus == 1 ? "启用任务" : "停用任务",
            httpRequest, Map.of("id", id), Map.of("status", nextStatus));
        return find(id);
    }

    public Map<String, Object> runManual(Long id, Long operatorId) {
        ScheduledJobRow job = find(id);
        if (job.getAllowManual() == null || job.getAllowManual() != 1) {
            throw new BusinessException("该任务不允许手动执行");
        }
        return runJob(job, "manual", true);
    }

    public void reloadSchedules() {
        scheduledTasks.keySet().forEach(this::cancel);
        jobs().stream()
            .filter(job -> job.getStatus() != null && job.getStatus() == 1)
            .forEach(this::schedule);
    }

    private void schedule(ScheduledJobRow job) {
        CronTrigger trigger = new CronTrigger(job.getCronExpression());
        ScheduledFuture<?> future = taskScheduler.schedule(() -> runJob(job.getId(), "scheduled"), trigger);
        if (future != null) {
            scheduledTasks.put(job.getId(), future);
        }
    }

    private void runJob(Long id, String triggerType) {
        try {
            runJob(find(id), triggerType, true);
        } catch (Exception ignored) {
            // runJob writes failure logs; scheduler thread should not stop on task failure.
        }
    }

    private Map<String, Object> runJob(ScheduledJobRow job, String triggerType, boolean useLock) {
        if (useLock) {
            LockConfiguration lockConfiguration = new LockConfiguration(
                Instant.now(),
                lockName(job),
                Duration.ofSeconds(Math.max(1, job.getLockAtMostSeconds())),
                Duration.ZERO
            );
            Optional<SimpleLock> lock = lockProvider.lock(lockConfiguration);
            if (lock.isEmpty()) {
                return Map.of("status", "skipped", "reason", "locked");
            }
            try {
                return executeAndLog(job, triggerType);
            } finally {
                lock.get().unlock();
            }
        }
        return executeAndLog(job, triggerType);
    }

    private Map<String, Object> executeAndLog(ScheduledJobRow job, String triggerType) {
        Instant started = Instant.now();
        Long logId = insertLog(job, triggerType, started);
        try {
            ScheduledJobHandler handler = handlers.get(job.getJobKey());
            if (handler == null) {
                throw new BusinessException("未找到任务处理器：" + job.getJobKey());
            }
            ScheduledJobContext context = new ScheduledJobContext(
                job.getId(), job.getCode(), job.getJobKey(), triggerType, job.getParams(), parseParams(job.getParams()));
            String output = handler.run(context);
            finishLog(logId, "success", output, "");
            jdbc.update("""
                UPDATE scheduled_jobs SET last_run_at = NOW(), next_run_at = ?, updated_at = NOW() WHERE id = ?
                """, nextRunAt(job.getCronExpression()), job.getId());
            return Map.of("status", "success", "log_id", logId);
        } catch (Exception e) {
            finishLog(logId, "failed", "", errorMessage(e));
            jdbc.update("UPDATE scheduled_jobs SET last_run_at = NOW(), updated_at = NOW() WHERE id = ?", job.getId());
            return Map.of("status", "failed", "log_id", logId, "error", errorMessage(e));
        }
    }

    private Long insertLog(ScheduledJobRow job, String triggerType, Instant started) {
        jdbc.update("""
            INSERT INTO scheduled_job_logs (scheduled_job_id, job_code, job_key, trigger_type, instance_id, status, started_at)
            VALUES (?, ?, ?, ?, ?, 'running', ?)
            """, job.getId(), job.getCode(), job.getJobKey(), triggerType, instanceId, java.sql.Timestamp.from(started));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void finishLog(Long logId, String status, String outputSummary, String errorMessage) {
        jdbc.update("""
            UPDATE scheduled_job_logs
            SET status = ?, ended_at = NOW(), duration_ms = TIMESTAMPDIFF(MICROSECOND, started_at, NOW()) / 1000,
              output_summary = ?, error_message = ?
            WHERE id = ?
            """, status, trim(outputSummary), trim(errorMessage), logId);
    }

    private ScheduledJobRow find(Long id) {
        List<ScheduledJobRow> rows = jdbc.queryForList("SELECT * FROM scheduled_jobs WHERE id = ?", id).stream()
            .map(this::jobRow)
            .toList();
        if (rows.isEmpty()) {
            throw new BusinessException("定时任务不存在", 404);
        }
        return rows.getFirst();
    }

    private void validateRequest(ScheduledJobRequest request, Long ignoreId) {
        try {
            CronExpression.parse(request.getCronExpression());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("cron 表达式不合法");
        }
        if (!handlers.containsKey(request.getJobKey())) {
            throw new BusinessException("任务处理器不存在");
        }
        Long codeCount = ignoreId == null
            ? jdbc.queryForObject("SELECT COUNT(*) FROM scheduled_jobs WHERE code = ?", Long.class, request.getCode())
            : jdbc.queryForObject("SELECT COUNT(*) FROM scheduled_jobs WHERE code = ? AND id <> ?", Long.class, request.getCode(), ignoreId);
        if (codeCount != null && codeCount > 0) {
            throw new BusinessException("任务编码已存在");
        }
        parseParams(nz(request.getParams()));
    }

    private void requireJob(Long id) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM scheduled_jobs WHERE id = ?", Long.class, id);
        if (count == null || count == 0) {
            throw new BusinessException("定时任务不存在", 404);
        }
    }

    private void cancel(Long id) {
        ScheduledFuture<?> future = scheduledTasks.remove(id);
        if (future != null) {
            future.cancel(false);
        }
    }

    private ScheduledJobRow jobRow(Map<String, Object> row) {
        ScheduledJobRow job = new ScheduledJobRow();
        job.setId(Rows.lng(row, "id"));
        job.setName(Rows.str(row, "name"));
        job.setCode(Rows.str(row, "code"));
        job.setJobKey(Rows.str(row, "job_key"));
        job.setCronExpression(Rows.str(row, "cron_expression"));
        job.setParams(Rows.str(row, "params"));
        job.setStatus(Rows.integer(row, "status"));
        job.setAllowManual(Rows.integer(row, "allow_manual"));
        job.setLockAtMostSeconds(Rows.integer(row, "lock_at_most_seconds"));
        job.setRemark(Rows.str(row, "remark"));
        job.setLastRunAt(Rows.date(row, "last_run_at"));
        job.setNextRunAt(Rows.date(row, "next_run_at"));
        job.setCreatedAt(Rows.date(row, "created_at"));
        job.setUpdatedAt(Rows.date(row, "updated_at"));
        return job;
    }

    private Map<String, Object> parseParams(String params) {
        if (params == null || params.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(params, MAP_TYPE);
        } catch (Exception e) {
            throw new BusinessException("任务参数必须是 JSON 对象");
        }
    }

    private java.sql.Timestamp nextRunAt(String cronExpression) {
        LocalDateTime next = CronExpression.parse(cronExpression).next(LocalDateTime.now());
        return next == null ? null : java.sql.Timestamp.valueOf(next);
    }

    private String errorMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private String trim(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 4000 ? value.substring(0, 4000) : value;
    }

    private int intValue(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }

    private String resolveInstanceId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    private String lockName(ScheduledJobRow job) {
        return "scheduled-job:" + job.getId();
    }
}
