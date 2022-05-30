package top.nb6.scheduler.xxl.biz

import org.junit.jupiter.api.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import top.nb6.scheduler.xxl.http.XxlAdminSiteProperties

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    @BeforeAll
    @AfterAll
    fun cleanup() {
        jobGroupBiz
            .query(null, null, null, null)
            .flatMap { jobGroupList ->
                if (jobGroupList.recordsFiltered > 0) {
                    Mono.just(jobGroupList.data.filter { dto ->
                        val appName = dto.appname
                        appName.equals(oldAppName, true) || appName.equals(newAppName, true)
                    }).flatMap {
                        Flux.just(*it.toTypedArray()).flatMap { dto ->
                            jobGroupBiz.delete(dto.id.toLong())
                        }.collectList()
                    }
                } else {
                    Mono.just(listOf())
                }
            }.flatMap {
                jobGroupBiz
                    .query(null, null, null, null)
            }.`as`(StepVerifier::create)
            .assertNext { list ->
                val leftCount = list.recordsFiltered
                if (leftCount > 0) {
                    val foundAny = list.data.any { dto ->
                        dto.appname.equals(oldAppName, true) || dto.appname.equals(
                            newAppName,
                            true
                        )
                    }
                    Assertions.assertEquals(false, foundAny)
                }
            }
            .verifyComplete()
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

    @Test
    fun testCreateAndUpdate() {
        var jobGroupId = 0
        jobGroupBiz
            .create(oldAppName, oldAppName, registerType = 0, addressList = "")
            .`as`(StepVerifier::create)
            .assertNext {
                jobGroupId = it.id
                Assertions.assertEquals(oldAppName, it.appname)
                Assertions.assertEquals(oldAppName, it.title)
            }.verifyComplete()
        Assertions.assertTrue(jobGroupId > 0)

        jobGroupBiz.update(jobGroupId, newAppName, newAppName, 0, "")
            .`as`(StepVerifier::create)
            .assertNext {
                Assertions.assertEquals(newAppName, it.appname)
                Assertions.assertEquals(newAppName, it.title)
            }.verifyComplete()
    }

    @Test
    fun testDelete() {
        var jobGroupId = 0
        val appName = deleteAppName
        jobGroupBiz
            .create(appName, appName, registerType = 0, addressList = "")
            .`as`(StepVerifier::create)
            .assertNext {
                jobGroupId = it.id
                Assertions.assertEquals(appName, it.appname)
                Assertions.assertEquals(appName, it.title)
            }.verifyComplete()
        Assertions.assertTrue(jobGroupId > 0)
        jobGroupBiz
            .delete(jobGroupId.toLong())
            .`as`(StepVerifier::create)
            .assertNext { dto ->
                if (dto.recordsFiltered > 0) {
                    val foundAny = dto.data.any { it.appname.equals(appName, true) }
                    Assertions.assertFalse(foundAny)
                }
            }.verifyComplete()
    }
}