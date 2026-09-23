package com.hrs.admin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduledJobRow {
    private Long id;
    private String name;
    private String code;
    @JsonProperty("job_key")
    private String jobKey;
    @JsonProperty("cron_expression")
    private String cronExpression;
    private String params;
    private Integer status;
    @JsonProperty("allow_manual")
    private Integer allowManual;
    @JsonProperty("lock_at_most_seconds")
    private Integer lockAtMostSeconds;
    private String remark;
    @JsonProperty("last_run_at")
    private String lastRunAt;
    @JsonProperty("next_run_at")
    private String nextRunAt;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getJobKey() { return jobKey; }
    public void setJobKey(String jobKey) { this.jobKey = jobKey; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getAllowManual() { return allowManual; }
    public void setAllowManual(Integer allowManual) { this.allowManual = allowManual; }
    public Integer getLockAtMostSeconds() { return lockAtMostSeconds; }
    public void setLockAtMostSeconds(Integer lockAtMostSeconds) { this.lockAtMostSeconds = lockAtMostSeconds; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(String lastRunAt) { this.lastRunAt = lastRunAt; }
    public String getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(String nextRunAt) { this.nextRunAt = nextRunAt; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
