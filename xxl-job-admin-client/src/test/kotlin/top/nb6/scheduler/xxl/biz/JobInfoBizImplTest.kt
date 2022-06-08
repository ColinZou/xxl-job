package top.nb6.scheduler.xxl.biz

import com.xxl.job.core.biz.model.JobInfoDto
import com.xxl.job.core.biz.model.types.*
import org.junit.jupiter.api.*
import top.nb6.scheduler.xxl.http.XxlAdminHttpClient
import top.nb6.scheduler.xxl.http.XxlAdminSiteProperties

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JobInfoBizImplTest {
    companion object {
        val adminSiteConfigProps = XxlAdminSiteProperties(
            "http://localhost:8080/xxl-job-admin",
            "admin", "123456"
        )
        private val client: XxlAdminHttpClient = XxlAdminHttpClient(adminSiteConfigProps)
        val jobGroupBiz = JobGroupBizImpl(client)
        val jobInfoBiz = JobInfoBizImpl(client)
        const val oldAppName = "oldAppName"
        const val jobCronExpression = "0 0 * * * ?"
        const val oldJobDescription = "oldJobDesc"
        const val newJobDescription = "newJobDesc"
        const val jobHandlerName = "testJobHandler"
        var groupId = 0
    }

    @BeforeAll
    fun setUp() {
        val result = jobGroupBiz.create(oldAppName, oldAppName, 0, "")
        Assertions.assertNotNull(result)
        Assertions.assertNotNull(result.id)
        Assertions.assertTrue(result.id > 0)
        Assertions.assertEquals(oldAppName, result.appname)
        Assertions.assertEquals(oldAppName, result.title)
        Assertions.assertEquals(0, result.addressType)
        groupId = result.id
    }

    @AfterAll
    fun cleanUp() {
        val currentResult = jobGroupBiz.query(null, null, null, null)
        for (item in currentResult.data) {
            if (item.appname.equals(JobGroupBizImplTest.oldAppName, true)) {
                val groupId = item.id
                val jobInfoList =
                    jobInfoBiz.query(
                        groupId, Constants.JOB_QRY_TRIGGER_STATUS_ALL, null,
                        null, null, null, null
                    )
                jobInfoList.data.forEach {
                    jobInfoBiz.remove(it!!.id)
                }
                //remove it
                jobGroupBiz.delete(item.id.toLong())
            }
        }
    }

    @Test
    fun testQuery() {
        val result = jobInfoBiz.query(
            groupId,
            com.xxl.job.core.biz.model.types.Constants.JOB_QRY_TRIGGER_STATUS_ALL,
            null,
            null,
            null,
            null,
            null
        )
        Assertions.assertNotNull(result)
        Assertions.assertNotNull(result.recordsFiltered)
        if (result.recordsFiltered > 0) {
            Assertions.assertNotNull(result.data)
            Assertions.assertNotNull(result.data[0])
        }
    }

    private fun getNewJobInfo(desc: String = oldJobDescription): JobInfoDto {
        return JobInfoDto(
            0,
            groupId,
            desc,
            null,
            null,
            "cz",
            "",
            EnumScheduleType.CRON,
            jobCronExpression,
            EnumMissingFireStrategy.FIRE_ONCE_NOW,
            EnumExecutorRoutingStrategy.ROUND,
            jobHandlerName,
            "",
            EnumExecutorBlockStrategy.SERIAL_EXECUTION,
            0,
            0,
            EnumGlueType.BEAN,
            "",
            "",
            null,
            null,
            0,
            0,
            0
        )
    }

    @Test
    fun testCrud() {
        val dto = getNewJobInfo()
        //create
        val result = jobInfoBiz.create(dto)
        Assertions.assertNotNull(result)
        Assertions.assertNotNull(result.id)
        Assertions.assertEquals(groupId, result.jobGroup)
        Assertions.assertEquals(oldJobDescription, result.jobDesc)
        Assertions.assertEquals(jobHandlerName, result.executorHandler)
        Assertions.assertEquals(jobCronExpression, result.scheduleConf)
        //update
        result.jobDesc = newJobDescription
        val updateResult = jobInfoBiz.update(result)
        Assertions.assertNotNull(updateResult)
        Assertions.assertEquals(newJobDescription, updateResult.jobDesc)
        //delete
        val deleteResult = jobInfoBiz.remove(result.id)
        Assertions.assertTrue(deleteResult)
        //verify delete
        val list = jobInfoBiz.query(
            groupId, Constants.JOB_QRY_TRIGGER_STATUS_ALL, null,
            null, null, null, null
        )
        val searchResult = list.data.firstOrNull { it.id == result.id }
        Assertions.assertNull(searchResult)
    }

    @Test
    fun testJobStartAndStop() {
        val dto = getNewJobInfo("forTriggering")
        //create
        val result = jobInfoBiz.create(dto)
        Assertions.assertNotNull(result)
        Assertions.assertNotNull(result.id)
        val startResult = jobInfoBiz.startJob(result.id)
        Assertions.assertTrue(startResult)
        checkJobStatus(result.id, Constants.JOB_QRY_TRIGGER_STATUS_RUNNING)
        val stopResult = jobInfoBiz.stopJob(result.id)
        Assertions.assertTrue(stopResult)
        checkJobStatus(result.id, Constants.JOB_QRY_TRIGGER_STATUS_STOPPED)

        val removeResult = jobInfoBiz.remove(result.id)
        Assertions.assertTrue(removeResult)
    }

    private fun checkJobStatus(jobId: Int, expectedStatus: Int) {
        val list = jobInfoBiz.query(
            groupId, Constants.JOB_QRY_TRIGGER_STATUS_ALL, null,
            null, null, null, null
        )
        val searchResult = list.data.firstOrNull { it.id == jobId && it.triggerStatus == expectedStatus }
        Assertions.assertNotNull(searchResult, "Could not find jobId=$jobId with triggerStatus=$expectedStatus")
    }
}