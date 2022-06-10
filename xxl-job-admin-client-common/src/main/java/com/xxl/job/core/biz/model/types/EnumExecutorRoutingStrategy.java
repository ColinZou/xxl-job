package com.xxl.job.core.biz.model.types;

public enum EnumExecutorRoutingStrategy {
    FIRST,
    LAST,
    ROUND,
    RANDOM,
    CONSISTENT_HASH,
    LEAST_FREQUENTLY_USED,
    LEAST_RECENTLY_USED,
    FAILOVER,
    BUSYOVER,
    SHARDING_BROADCAST
}
