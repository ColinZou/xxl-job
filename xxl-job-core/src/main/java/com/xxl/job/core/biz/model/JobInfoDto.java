package com.xxl.job.core.biz.model;

import com.xxl.job.core.biz.model.types.EnumExecutorBlockStrategy;
import com.xxl.job.core.biz.model.types.EnumExecutorRoutingStrategy;
import com.xxl.job.core.biz.model.types.EnumGlueType;
import com.xxl.job.core.biz.model.types.EnumMissingFireStrategy;
import com.xxl.job.core.biz.model.types.EnumScheduleType;

import java.util.Date;

public class JobInfoDto {
    private int id;                // 主键ID

    private int jobGroup;        // 执行器主键ID
    private String jobDesc;

    private Date addTime;
    private Date updateTime;

    private String author;        // 负责人
    private String alarmEmail;    // 报警邮件

    private EnumScheduleType scheduleType;            // 调度类型
    private String scheduleConf;            // 调度配置，值含义取决于调度类型
    private EnumMissingFireStrategy misfireStrategy;            // 调度过期策略

    private EnumExecutorRoutingStrategy executorRouteStrategy;    // 执行器路由策略
    private String executorHandler;            // 执行器，任务Handler名称
    private String executorParam;            // 执行器，任务参数
    private EnumExecutorBlockStrategy executorBlockStrategy;    // 阻塞处理策略
    private int executorTimeout;            // 任务执行超时时间，单位秒
    private int executorFailRetryCount;        // 失败重试次数

    private EnumGlueType glueType;        // GLUE类型
    private String glueSource;        // GLUE源代码
    private String glueRemark;        // GLUE备注
    private Date glueUpdatetime;    // GLUE更新时间

    private String childJobId;        // 子任务ID，多个逗号分隔

    private int triggerStatus;        // 调度状态：0-停止，1-运行
    private long triggerLastTime;    // 上次调度时间
    private long triggerNextTime;    // 下次调度时间

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(int jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAlarmEmail() {
        return alarmEmail;
    }

    public void setAlarmEmail(String alarmEmail) {
        this.alarmEmail = alarmEmail;
    }

    public EnumScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(EnumScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getScheduleConf() {
        return scheduleConf;
    }

    public void setScheduleConf(String scheduleConf) {
        this.scheduleConf = scheduleConf;
    }

    public EnumMissingFireStrategy getMisfireStrategy() {
        return misfireStrategy;
    }

    public void setMisfireStrategy(EnumMissingFireStrategy misfireStrategy) {
        this.misfireStrategy = misfireStrategy;
    }

    public EnumExecutorRoutingStrategy getExecutorRouteStrategy() {
        return executorRouteStrategy;
    }

    public void setExecutorRouteStrategy(EnumExecutorRoutingStrategy executorRouteStrategy) {
        this.executorRouteStrategy = executorRouteStrategy;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getExecutorParam() {
        return executorParam;
    }

    public void setExecutorParam(String executorParam) {
        this.executorParam = executorParam;
    }

    public EnumExecutorBlockStrategy getExecutorBlockStrategy() {
        return executorBlockStrategy;
    }

    public void setExecutorBlockStrategy(
        EnumExecutorBlockStrategy executorBlockStrategy) {
        this.executorBlockStrategy = executorBlockStrategy;
    }

    public int getExecutorTimeout() {
        return executorTimeout;
    }

    public void setExecutorTimeout(int executorTimeout) {
        this.executorTimeout = executorTimeout;
    }

    public int getExecutorFailRetryCount() {
        return executorFailRetryCount;
    }

    public void setExecutorFailRetryCount(int executorFailRetryCount) {
        this.executorFailRetryCount = executorFailRetryCount;
    }

    public EnumGlueType getGlueType() {
        return glueType;
    }

    public void setGlueType(EnumGlueType glueType) {
        this.glueType = glueType;
    }

    public String getGlueSource() {
        return glueSource;
    }

    public void setGlueSource(String glueSource) {
        this.glueSource = glueSource;
    }

    public String getGlueRemark() {
        return glueRemark;
    }

    public void setGlueRemark(String glueRemark) {
        this.glueRemark = glueRemark;
    }

    public Date getGlueUpdatetime() {
        return glueUpdatetime;
    }

    public void setGlueUpdatetime(Date glueUpdatetime) {
        this.glueUpdatetime = glueUpdatetime;
    }

    public String getChildJobId() {
        return childJobId;
    }

    public void setChildJobId(String childJobId) {
        this.childJobId = childJobId;
    }

    public int getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(int triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    public long getTriggerLastTime() {
        return triggerLastTime;
    }

    public void setTriggerLastTime(long triggerLastTime) {
        this.triggerLastTime = triggerLastTime;
    }

    public long getTriggerNextTime() {
        return triggerNextTime;
    }

    public void setTriggerNextTime(long triggerNextTime) {
        this.triggerNextTime = triggerNextTime;
    }

    public JobInfoDto(int id, int jobGroup, String jobDesc, Date addTime, Date updateTime,
                      String author, String alarmEmail,
                      EnumScheduleType scheduleType, String scheduleConf,
                      EnumMissingFireStrategy misfireStrategy,
                      EnumExecutorRoutingStrategy executorRouteStrategy,
                      String executorHandler, String executorParam,
                      EnumExecutorBlockStrategy executorBlockStrategy, int executorTimeout,
                      int executorFailRetryCount,
                      EnumGlueType glueType, String glueSource, String glueRemark,
                      Date glueUpdatetime, String childJobId, int triggerStatus,
                      long triggerLastTime,
                      long triggerNextTime) {
        this.id = id;
        this.jobGroup = jobGroup;
        this.jobDesc = jobDesc;
        this.addTime = addTime;
        this.updateTime = updateTime;
        this.author = author;
        this.alarmEmail = alarmEmail;
        this.scheduleType = scheduleType;
        this.scheduleConf = scheduleConf;
        this.misfireStrategy = misfireStrategy;
        this.executorRouteStrategy = executorRouteStrategy;
        this.executorHandler = executorHandler;
        this.executorParam = executorParam;
        this.executorBlockStrategy = executorBlockStrategy;
        this.executorTimeout = executorTimeout;
        this.executorFailRetryCount = executorFailRetryCount;
        this.glueType = glueType;
        this.glueSource = glueSource;
        this.glueRemark = glueRemark;
        this.glueUpdatetime = glueUpdatetime;
        this.childJobId = childJobId;
        this.triggerStatus = triggerStatus;
        this.triggerLastTime = triggerLastTime;
        this.triggerNextTime = triggerNextTime;
    }

    public JobInfoDto() {
    }
}
