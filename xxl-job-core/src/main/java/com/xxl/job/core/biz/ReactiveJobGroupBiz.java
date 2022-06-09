package com.xxl.job.core.biz;

import com.xxl.job.core.biz.exceptions.ApiInvokeException;
import com.xxl.job.core.biz.exceptions.LoginFailedException;
import com.xxl.job.core.biz.model.JobGroupDto;
import com.xxl.job.core.biz.model.JobGroupListDto;
import reactor.core.publisher.Mono;

public interface ReactiveJobGroupBiz {
    Mono<JobGroupListDto> query(String appName, String title, Integer offset, Integer count) throws
        LoginFailedException;

    /**
     * 创建执行器
     *
     * @param appName      应用名称
     * @param title        标题
     * @param registerType 注册方式：0自动、1手动
     * @param addressList  执行器的地址列表
     * @return
     * @throws LoginFailedException
     * @throws ApiInvokeException
     */
    Mono<JobGroupDto> create(String appName, String title, Integer registerType, String addressList)
        throws LoginFailedException, ApiInvokeException;

    Mono<JobGroupDto> update(int id, String appName, String title, Integer registerType,
                             String addressList) throws LoginFailedException, ApiInvokeException;

    Mono<JobGroupListDto> delete(Long id) throws LoginFailedException, ApiInvokeException;
}
