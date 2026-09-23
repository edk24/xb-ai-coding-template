package com.hrs.admin.scheduler;

import org.springframework.stereotype.Component;

@Component
public class SystemNoopJobHandler implements ScheduledJobHandler {
    @Override
    public String jobKey() {
        return "system.noop";
    }

    @Override
    public String name() {
        return "系统示例任务";
    }

    @Override
    public String run(ScheduledJobContext context) {
        Object message = context.paramMap().get("message");
        if (message == null || String.valueOf(message).isBlank()) {
            return "noop executed";
        }
        return String.valueOf(message);
    }
}
