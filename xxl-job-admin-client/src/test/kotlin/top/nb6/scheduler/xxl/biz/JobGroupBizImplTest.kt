package top.nb6.scheduler.xxl.biz

import org.junit.jupiter.api.*
import top.nb6.scheduler.xxl.http.XxlAdminHttpClient
import top.nb6.scheduler.xxl.http.XxlAdminSiteProperties

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JobGroupBizImplTest {
    companion object {
        val adminSiteConfigProps = XxlAdminSiteProperties(
            "http://localhost:8080/xxl-job-admin",
            "admin", "123456"
        )
        val client: XxlAdminHttpClient = XxlAdminHttpClient(adminSiteConfigProps)
        const val oldAppName = "oldAppName"
        const val newAppName = "newAppName"
        const val deleteAppName = "delAppName"
    }

    @BeforeAll
    @AfterAll
    fun cleanup() {
        val jobGroupBiz = JobGroupBizImpl(client)
        val currentResult = jobGroupBiz.query(null, null, null, null)
        for (item in currentResult.data) {
            if (item.appname.equals(oldAppName, true)
                || item.appname.equals(newAppName, true)
                || item.appname.equals(deleteAppName, true)
            ) {
                //remove it
                jobGroupBiz.delete(item.id.toLong())
            }
        }
    }

    @Test
    fun testQuery() {
        val jobGroupBiz = JobGroupBizImpl(client)
        val data = jobGroupBiz.query(null, null, null, null)
        Assertions.assertNotNull(data)
        Assertions.assertNotNull(data.recordsTotal)
        if (data.recordsTotal > 0) {
            Assertions.assertNotNull(data.data)
            Assertions.assertTrue(data.data.size > 0)
            Assertions.assertNotNull(data.data.get(0))
        }
    }

    @Test
    fun testCreteAndUpdate() {
        val jobGroupBiz = JobGroupBizImpl(client)
        val result = jobGroupBiz.create(oldAppName, oldAppName, 0, "")
        Assertions.assertNotNull(result)
        Assertions.assertNotNull(result.id)
        Assertions.assertTrue(result.id > 0)
        Assertions.assertEquals(oldAppName, result.appname)
        Assertions.assertEquals(oldAppName, result.title)
        Assertions.assertEquals(0, result.addressType)

        val updateResult = jobGroupBiz.update(result.id, newAppName, newAppName, 0, "")
        Assertions.assertNotNull(updateResult)
        Assertions.assertNotNull(updateResult.id)
        Assertions.assertTrue(updateResult.id > 0)
        Assertions.assertEquals(newAppName, updateResult.appname)
        Assertions.assertEquals(newAppName, updateResult.title)
        Assertions.assertEquals(0, updateResult.addressType)
    }

    @Test
    fun testDelete() {
        val appName = deleteAppName
        val jobGroupBiz = JobGroupBizImpl(client)
        val result = jobGroupBiz.create(appName, appName, 0, "")
        Assertions.assertNotNull(result)
        Assertions.assertNotNull(result.id)
        Assertions.assertTrue(result.id > 0)
        Assertions.assertEquals(appName, result.appname)
        Assertions.assertEquals(appName, result.title)
        Assertions.assertEquals(0, result.addressType)

        val deleteResult = jobGroupBiz.delete(result.id.toLong())
        Assertions.assertNotNull(deleteResult)
        Assertions.assertNotNull(deleteResult.data)
        Assertions.assertNull(deleteResult.data.firstOrNull { it.id == result.id })
    }
}