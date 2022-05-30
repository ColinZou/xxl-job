--#
--# XXL-JOB v2.4.0-SNAPSHOT
--Copyright (c) 2015-present, xuxueli.

CREATE TABLE "xxl_job_info"
(
    "id"                        SERIAL8      NOT NULL PRIMARY KEY,
    "job_group"                 INT8         NOT NULL,
    "job_desc"                  varchar(255) NOT NULL,
    "add_time"                  timestamptz           DEFAULT NULL,
    "update_time"               timestamptz           DEFAULT NULL,
    "author"                    varchar(64)           DEFAULT NULL,
    "alarm_email"               varchar(255)          DEFAULT NULL,
    "schedule_type"             varchar(50)  NOT NULL DEFAULT 'NONE',
    "schedule_conf"             varchar(128)          DEFAULT NULL,
    "misfire_strategy"          varchar(50)  NOT NULL DEFAULT 'DO_NOTHING',
    "executor_route_strategy"   varchar(50)           DEFAULT NULL,
    "executor_handler"          varchar(255)          DEFAULT NULL,
    "executor_param"            varchar(512)          DEFAULT NULL,
    "executor_block_strategy"   varchar(50)           DEFAULT NULL,
    "executor_timeout"          INT4         NOT NULL DEFAULT 0,
    "executor_fail_retry_count" INT4         NOT NULL DEFAULT 0,
    "glue_type"                 varchar(50)  NOT NULL,
    "glue_source"               text,
    "glue_remark"               varchar(128)          DEFAULT NULL,
    "glue_updatetime"           timestamptz           DEFAULT NULL,
    "child_jobid"               varchar(255)          DEFAULT NULL,
    "trigger_status"            INT2         NOT NULL DEFAULT 0,
    "trigger_last_time"         BIGINT       NOT NULL DEFAULT 0,
    "trigger_next_time"         BIGINT       NOT NULL DEFAULT 0
);


CREATE TABLE "xxl_job_log"
(
    "id"                        SERIAL8 NOT NULL PRIMARY KEY,
    "job_group"                 INT8    NOT NULL,
    "job_id"                    INT8    NOT NULL,
    "executor_address"          varchar(255)     DEFAULT NULL,
    "executor_handler"          varchar(255)     DEFAULT NULL,
    "executor_param"            varchar(512)     DEFAULT NULL,
    "executor_sharding_param"   varchar(20)      DEFAULT NULL,
    "executor_fail_retry_count" INT8    NOT NULL DEFAULT 0,
    "trigger_time"              timestamptz      DEFAULT NULL,
    "trigger_code"              INT8    NOT NULL,
    "trigger_msg"               text,
    "handle_time"               timestamptz      DEFAULT NULL,
    "handle_code"               INT8    NOT NULL,
    "handle_msg"                text,
    "alarm_status"              INT2    NOT NULL DEFAULT 0
);
CREATE INDEX xxl_job_log_I_trigger_time ON xxl_job_log (trigger_time);
CREATE INDEX xxl_job_log_I_handle_code ON xxl_job_log (handle_code);

CREATE TABLE "xxl_job_log_report"
(
    "id"            SERIAL8 NOT NULL PRIMARY KEY,
    "trigger_day"   timestamptz      DEFAULT NULL,
    "running_count" INT8    NOT NULL DEFAULT 0,
    "suc_count"     INT8    NOT NULL DEFAULT 0,
    "fail_count"    INT8    NOT NULL DEFAULT 0,
    "update_time"   timestamptz      DEFAULT NULL
);
CREATE UNIQUE INDEX xxl_job_log_report_i_trigger_day ON xxl_job_log_report (trigger_day);

CREATE TABLE "xxl_job_logglue"
(
    "id"          SERIAL8      NOT NULL PRIMARY KEY,
    "job_id"      INT8         NOT NULL,
    "glue_type"   varchar(50) DEFAULT NULL,
    "glue_source" text,
    "glue_remark" varchar(128) NOT NULL,
    "add_time"    timestamptz DEFAULT NULL,
    "update_time" timestamptz DEFAULT NULL
);

CREATE TABLE "xxl_job_registry"
(
    "id"             SERIAL8      NOT NULL PRIMARY KEY,
    "registry_group" varchar(50)  NOT NULL,
    "registry_key"   varchar(255) NOT NULL,
    "registry_value" varchar(255) NOT NULL,
    "update_time"    timestamptz DEFAULT NULL
);
CREATE INDEX xxl_job_registry_i_g_k_v ON xxl_job_registry ("registry_group", "registry_key", "registry_value");


CREATE TABLE "xxl_job_group"
(
    "id"           SERIAL8     NOT NULL PRIMARY KEY,
    "app_name"     varchar(64) NOT NULL,
    "title"        varchar(12) NOT NULL,
    "address_type" INT2        NOT NULL DEFAULT 0,
    "address_list" text,
    "update_time"  timestamptz          DEFAULT NULL
);

CREATE TABLE "xxl_job_user"
(
    "id"         SERIAL8     NOT NULL PRIMARY KEY,
    "username"   varchar(50) NOT NULL,
    "password"   varchar(50) NOT NULL,
    "role"       INT2        NOT NULL,
    "permission" varchar(255) DEFAULT NULL
);
CREATE UNIQUE INDEX xxl_job_user_i_username ON xxl_job_user (username);

CREATE TABLE "xxl_job_lock"
(
    "lock_name" varchar(50) NOT NULL PRIMARY KEY
);
INSERT INTO "xxl_job_lock" ("lock_name")
VALUES ('schedule_lock');
