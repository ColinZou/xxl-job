package top.nb6.scheduler.xxl.biz

import com.xxl.job.core.biz.ReactiveJobGroupBiz
import com.xxl.job.core.biz.model.JobGroupDto
import com.xxl.job.core.biz.model.JobGroupListDto
import reactor.core.publisher.Mono



class ReactiveJobGroupBizImpl : ReactiveJobGroupBiz {
    override fun query(appName: String?, title: String?, offset: Int?, count: Int?): Mono<JobGroupListDto> {
        TODO("Not yet implemented")
    }

    override fun create(appName: String?, title: String?, registerType: Int?, addressList: String?): Mono<JobGroupDto> {
        TODO("Not yet implemented")
    }

    override fun update(
        id: Int,
        appName: String?,
        title: String?,
        registerType: Int?,
        addressList: String?
    ): Mono<JobGroupDto> {
        TODO("Not yet implemented")
    }

    override fun delete(id: Long?): Mono<JobGroupListDto> {
        TODO("Not yet implemented")
    }
}