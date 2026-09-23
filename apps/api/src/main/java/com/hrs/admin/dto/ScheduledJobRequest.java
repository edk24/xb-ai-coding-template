package com.hrs.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ScheduledJobRequest {
    @NotBlank(message = "请输入任务名称")
    private String name;
    @NotBlank(message = "请输入任务编码")
    private String code;
    @NotBlank(message = "请选择任务处理器")
    @JsonProperty("job_key")
    private String jobKey;
    @NotBlank(message = "请输入 cron 表达式")
    @JsonProperty("cron_expression")
    private String cronExpression;
    private String params = "";
    private Integer status = 1;
    @JsonProperty("allow_manual")
    private Integer allowManual = 1;
    @Min(value = 1, message = "锁最长持有秒数必须大于 0")
    @JsonProperty("lock_at_most_seconds")
    private Integer lockAtMostSeconds = 600;
    private String remark = "";

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
}
