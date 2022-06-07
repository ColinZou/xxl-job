package com.xxl.job.core.biz;

import com.xxl.job.core.biz.exceptions.LoginFailedException;
import com.xxl.job.core.biz.model.JobGroupListDto;

public interface JobGroupBiz {
    JobGroupListDto query(String appName, String title, Integer offset, Integer count) throws
        LoginFailedException;
}
