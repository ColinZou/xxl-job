package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class XxlJobRegistryDaoTest {

    @Resource
    private XxlJobRegistryDao xxlJobRegistryDao;

    @Test
    public void test() {
        int ret = xxlJobRegistryDao.registryUpdate("g1", "k1", "v1", new Date());
        if (ret < 1) {
            ret = xxlJobRegistryDao.registrySave("g1", "k1", "v1", new Date());
        }
        Date targetTime = Date.from(LocalDateTime.now()
            .minus(1, ChronoUnit.SECONDS)
            .atZone(
                ZoneId.systemDefault()).toInstant());
        List<XxlJobRegistry> list = xxlJobRegistryDao.findAll(targetTime);

        int ret2 = xxlJobRegistryDao.removeDead(Arrays.asList(1));
    }

}
