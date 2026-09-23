package com.hrs.admin.scheduler;

public interface ScheduledJobHandler {
    String jobKey();

    String name();

    String run(ScheduledJobContext context);
}
