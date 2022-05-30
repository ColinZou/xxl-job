package top.nb6.scheduler.xxl.biz

import com.xxl.job.core.biz.model.JobGroupDto
import com.xxl.job.core.biz.model.JobInfoDto
import com.xxl.job.core.biz.model.types.*
import org.junit.jupiter.api.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import top.nb6.scheduler.xxl.http.XxlAdminSiteProperties

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReactiveJobInfoClientImplTest {
    companion object {
        val adminSiteConfigProps = XxlAdminSiteProperties(
            "http://localhost:8080/xxl-job-admin",
            "admin", "123456"
        )
        val jobGroupBiz = ReactiveJobGroupBizImpl(adminSiteConfigProps)
        val jobInfoBiz = ReactiveJobInfoClientImpl(adminSiteConfigProps)
        const val oldAppName = "oldAppName"
        const val jobCronExpression = "0 0 * * * ?"
        const val oldJobDescription = "oldJobDesc"
        const val newJobDescription = "newJobDesc"
        const val jobHandlerName = "testJobHandler"
        var groupId = 0
    }

    @BeforeAll
    fun setUp() {
        jobGroupBiz.create(oldAppName, oldAppName, 0, "")
            .`as`(StepVerifier::create)
            .assertNext { result ->
                Assertions.assertNotNull(result)
                Assertions.assertNotNull(result.id)
                Assertions.assertTrue(result.id > 0)
                Assertions.assertEquals(oldAppName, result.appname)
                Assertions.assertEquals(oldAppName, result.title)
                Assertions.assertEquals(0, result.addressType)
                groupId = result.id
            }.verifyComplete()
    }

    @AfterAll
    fun cleanUp() {
        var jobGroupList: List<JobGroupDto>? = null
        jobGroupBiz.query(null, null, null, null)
            .flatMap { currentResult ->
                jobGroupList = currentResult.data
                val items = currentResult.data.filter { item ->
                    item.appname.equals(oldAppName, true)
                }
                Flux.just(*items.toTypedArray()).flatMap { dto ->
                    jobInfoBiz.query(
                        dto.id,
                        FlagConstants.JOB_QRY_TRIGGER_STATUS_ALL, null, null,
                        null, null, null
                    ).flatMap { jobBiz ->
                        Flux.just(*jobBiz.data.toTypedArray()).flatMap { jobInfo ->
                            Mono.zip(jobInfoBiz.remove(jobInfo.id), Mono.just(jobInfo))
                        }.collectList()
                    }
                }.map { tuple ->
                    tuple.map { it.t2.jobGroup }.toSet()
                }.collectList()
            }
            .`as`(StepVerifier::create)
            .assertNext { currentResult ->
                Assertions.assertNotNull(currentResult)
            }
            .verifyComplete()
        jobGroupList?.let { list ->
            Flux.just(*list.filter { it.appname.equals(oldAppName, true) }.toTypedArray())
                .flatMap { dto ->
                    jobGroupBiz.delete(dto.id.toLong())
                }.map {
                    it.data
                }.collectList()
                .`as`(StepVerifier::create)
                .assertNext {
                    val data = it.flatten().map { dto -> dto.appname }.toSet()
                    Assertions.assertFalse(data.contains(oldAppName))
                }.verifyComplete()
        }
    }

    @Test
    fun testQuery() {
        jobInfoBiz.query(
            groupId, FlagConstants.JOB_QRY_TRIGGER_STATUS_ALL,
            null, null, null, null, null
        ).`as`(StepVerifier::create)
            .assertNext {
                Assertions.assertNotNull(it)
                if (it.recordsFiltered > 0) {
                    Assertions.assertNotNull(it.data[0])
                }
            }.verifyComplete()
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
        val newDto = getNewJobInfo()
        var createResult: JobInfoDto? = null
        jobInfoBiz
            .create(dto = newDto)
            .`as`(StepVerifier::create)
            .assertNext { result ->
                Assertions.assertNotNull(result)
                Assertions.assertNotNull(result.id)
                Assertions.assertEquals(groupId, result.jobGroup)
                Assertions.assertEquals(oldJobDescription, result.jobDesc)
                Assertions.assertEquals(jobHandlerName, result.executorHandler)
                Assertions.assertEquals(jobCronExpression, result.scheduleConf)
                createResult = result
            }
            .verifyComplete()
        Assertions.assertNotNull(createResult)
        createResult?.let { result ->
            result.jobDesc = newJobDescription
            jobInfoBiz.update(result)
                .`as`(StepVerifier::create)
                .assertNext { updateResult ->
                    Assertions.assertNotNull(updateResult)
                    Assertions.assertEquals(newJobDescription, updateResult.jobDesc)
                }
                .verifyComplete()
        }
        createResult?.let { result ->
            jobInfoBiz.remove(result.id)
                .`as`(StepVerifier::create)
                .assertNext {
                    Assertions.assertTrue(it)
                }
                .verifyComplete()
        }
    }

    @Test
    fun testJobStartAndStop() {
        val dto = getNewJobInfo("forTriggering")
        var createResult: JobInfoDto? = null
        jobInfoBiz
            .create(dto)
            .`as`(StepVerifier::create)
            .assertNext { result ->
                Assertions.assertNotNull(result)
                createResult = result
            }
            .verifyComplete()
        Assertions.assertNotNull(createResult)
        createResult?.let { result ->
            testJobStatusChange(jobInfoBiz::startJob, result.id, FlagConstants.JOB_QRY_TRIGGER_STATUS_RUNNING)
            testJobStatusChange(jobInfoBiz::stopJob, result.id, FlagConstants.JOB_QRY_TRIGGER_STATUS_STOPPED)
        }
    }

    private fun testJobStatusChange(method: (Int) -> Mono<Boolean>, jobId: Int, targetTriggerStatus: Int) {
        method.invoke(jobId).filter {
            it
        }.flatMap {
            getJobStatus(jobId)
        }.`as`(StepVerifier::create)
            .assertNext {
                Assertions.assertEquals(targetTriggerStatus, it)
            }
            .verifyComplete()
    }

    private fun getJobStatus(jobId: Int): Mono<Int> {
        return jobInfoBiz.query(
            groupId, FlagConstants.JOB_QRY_TRIGGER_STATUS_ALL,
            null, null, null, null, null
        ).mapNotNull { it.data.firstOrNull { dto -> dto.id == jobId } }
            .mapNotNull { it?.triggerStatus }
    }
}