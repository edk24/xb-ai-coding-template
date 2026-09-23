package com.hrs.admin.scheduler;

import java.util.Map;

public record ScheduledJobContext(
    Long jobId,
    String code,
    String jobKey,
    String triggerType,
    String params,
    Map<String, Object> paramMap
) {
}
