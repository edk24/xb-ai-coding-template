package com.hrs.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ScheduledJobServiceTest {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ScheduledJobService scheduledJobService;

    @BeforeEach
    void cleanDynamicRows() {
        jdbc.update("DELETE FROM scheduled_job_logs");
        jdbc.update("DELETE FROM scheduled_jobs WHERE id <> 1");
        jdbc.update("DELETE FROM shedlock");
    }

    @Test
    void manualRunExecutesHandlerAndWritesSuccessLog() {
        Long jobId = createJob("manual_success_job", "system.noop", "{\"message\":\"manual ok\"}", 0);

        Map<String, Object> result = scheduledJobService.runManual(jobId, 1L);

        assertThat(result).containsEntry("status", "success");
        List<Map<String, Object>> logs = jdbc.queryForList("""
            SELECT * FROM scheduled_job_logs WHERE scheduled_job_id = ? ORDER BY id DESC
            """, jobId);
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst())
            .containsEntry("trigger_type", "manual")
            .containsEntry("status", "success");
        assertThat(String.valueOf(logs.getFirst().get("output_summary"))).contains("manual ok");
    }

    @Test
    void manualRunWritesFailedLogWhenHandlerIsMissing() {
        Long jobId = createJob("missing_handler_job", "missing.handler", "", 0);

        Map<String, Object> result = scheduledJobService.runManual(jobId, 1L);

        assertThat(result).containsEntry("status", "failed");
        List<Map<String, Object>> logs = jdbc.queryForList("""
            SELECT * FROM scheduled_job_logs WHERE scheduled_job_id = ? ORDER BY id DESC
            """, jobId);
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst())
            .containsEntry("trigger_type", "manual")
            .containsEntry("status", "failed");
        assertThat(String.valueOf(logs.getFirst().get("error_message"))).contains("未找到任务处理器");
    }

    @Test
    void manualRunSkipsWhenShedLockIsHeldByAnotherInstance() {
        Long jobId = createJob("locked_job", "system.noop", "{\"message\":\"locked\"}", 0);
        jdbc.update("""
            INSERT INTO shedlock (name, lock_until, locked_at, locked_by)
            VALUES (?, DATEADD('MINUTE', 5, CURRENT_TIMESTAMP()), CURRENT_TIMESTAMP(), ?)
            """, "scheduled-job:" + jobId, "other-instance");

        Map<String, Object> result = scheduledJobService.runManual(jobId, 1L);

        assertThat(result)
            .containsEntry("status", "skipped")
            .containsEntry("reason", "locked");
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM scheduled_job_logs WHERE scheduled_job_id = ?", Long.class, jobId);
        assertThat(count).isZero();
    }

    private Long createJob(String code, String jobKey, String params, int status) {
        jdbc.update("""
            INSERT INTO scheduled_jobs (name, code, job_key, cron_expression, params, status, lock_at_most_seconds, remark, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, code, code, jobKey, "0 0 0 * * *", params, status, 60, "", 1L, 1L);
        return jdbc.queryForObject("SELECT MAX(id) FROM scheduled_jobs WHERE code = ?", Long.class, code);
    }
}
