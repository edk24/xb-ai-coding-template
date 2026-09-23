package com.hrs.admin.service;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Map;

final class Rows {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Rows() {
    }

    static String str(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    static Long lng(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    static Integer integer(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    static String date(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(FORMATTER);
        }
        return String.valueOf(value).replace('T', ' ');
    }
}
