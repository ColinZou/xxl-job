package top.nb6.scheduler.xxl.biz

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import top.nb6.scheduler.xxl.http.XxlAdminSiteProperties

internal class ReactiveJobGroupBizImplTest {
    companion object {
        private val adminSiteConfigProps = XxlAdminSiteProperties(
            "http://localhost:8080/xxl-job-admin",
            "admin", "123456"
        )
        const val oldAppName = "oldAppName"
        const val newAppName = "newAppName"
        const val deleteAppName = "delAppName"
        val jobGroupBiz = ReactiveJobGroupBizImpl(adminSiteConfigProps)
    }

    @Test
    fun testQuery() {
        jobGroupBiz
            .query(oldAppName, "", null, null)
            .`as`(StepVerifier::create)
            .assertNext {
                Assertions.assertNotNull(it)
            }.verifyComplete()

    }
}