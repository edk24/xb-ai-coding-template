package com.hrs.admin.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtils {
    /**
     * 密码策略：8-64 位，且同时包含字母和数字。
     */
    public static final String POLICY_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[\\s\\S]{8,64}$";
    public static final String POLICY_MESSAGE = "密码需 8–64 位且同时包含字母和数字";

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BYTES = 16;
    private static final int LEGACY_SALT_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtils() {
    }

    /**
     * 生成 16 字节随机 salt（32 个十六进制字符）。
     */
    public static String newSalt() {
        byte[] bytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * PBKDF2WithHmacSHA256 派生 16 字节密钥（32 个十六进制字符）。
     * 仅供新密码使用，旧数据由 {@link #verify(String, String, String)} 兼容。
     */
    public static String hash(String plainPassword, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), HexFormat.of().parseHex(salt), ITERATIONS, KEY_BYTES * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return HexFormat.of().formatHex(factory.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 不可用", e);
        }
    }

    /**
     * 校验明文密码。salt 为 8 位十六进制时按旧方案 md5Hex(md5Hex(明文) + salt) 校验；
     * 非法/损坏的 salt 或 hash 一律返回 false，避免把数据问题暴露成 500。
     */
    public static boolean verify(String plainPassword, String storedHash, String salt) {
        if (plainPassword == null || storedHash == null || salt == null) {
            return false;
        }
        String expected;
        try {
            if (isLegacySalt(salt)) {
                expected = md5Hex(md5Hex(plainPassword) + salt);
            } else if (salt.length() == SALT_BYTES * 2) {
                expected = hash(plainPassword, salt);
            } else {
                return false;
            }
        } catch (RuntimeException e) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), storedHash.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 旧方案 salt 只有 4 字节（8 个十六进制字符），据此判断是否需要自动升级哈希。
     */
    public static boolean isLegacySalt(String salt) {
        return salt != null && salt.length() == LEGACY_SALT_LENGTH;
    }

    /**
     * 仅为兼容历史数据保留，新密码不得再使用 MD5。
     */
    public static String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 digest unavailable", e);
        }
    }
}
