package top.nb6.scheduler.xxl.biz

import com.xxl.job.core.biz.ReactiveJobGroupBiz
import com.xxl.job.core.biz.ReactiveJobInfoBiz
import com.xxl.job.core.biz.exceptions.ApiInvokeException
import com.xxl.job.core.biz.model.JobGroupDto
import com.xxl.job.core.biz.model.JobGroupListDto
import com.xxl.job.core.biz.model.JobInfoDto
import com.xxl.job.core.biz.model.JobInfoListDto
import com.xxl.job.core.biz.model.types.FlagConstants
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ClientHttpRequest
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import top.nb6.scheduler.xxl.http.*
import top.nb6.scheduler.xxl.utils.FormUtils
import java.net.URLEncoder
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.security.auth.login.LoginException
import kotlin.math.ceil

class GeneralApiResponse(code: Long?, msg: String?, val content: Any? = null) : CommonAdminApiResponse(code, msg)
class Locker {
    companion object {
        val log: Logger = LoggerFactory.getLogger(Locker::class.java)
        const val RETRY_INTERVAL_MS = 50
    }

    private val locked = AtomicBoolean(false)


    fun <T> lock(duration: Duration, job: Flux<T>): Flux<T> {
        return obtainLock(duration).flux().defaultIfEmpty(false)
            .filter {
                if (!it) {
                    error("Could not get lock")
                }
                it
            }
            .doOnSubscribe {
                log.debug("Obtaining lock")
            }
            .doOnError { e -> log.error("failed to obtain lock $e", e) }
            .flatMap {
                log.debug("Invoking the real job")
                job
            }
            .doFinally {
                releaseLock()
            }
    }

    fun <T> lock(duration: Duration, job: Mono<T>): Mono<T> {
        return obtainLock(duration).defaultIfEmpty(false)
            .filter {
                if (!it) {
                    error("Could not get lock")
                }
                it
            }
            .doOnSubscribe {
                log.debug("Obtaining lock")
            }
            .doOnError { e -> log.error("failed to obtain lock $e", e) }
            .flatMap {
                log.debug("Invoking the real job")
                job
            }
            .doFinally {
                releaseLock()
            }
    }

    private fun obtainLock(timeOut: Duration): Mono<Boolean> {
        val times = ceil(timeOut.toMillis() * 1.0 / RETRY_INTERVAL_MS).toLong()
        return Mono.fromCallable { (!locked.compareAndExchange(false, true)) }
            .filter {
                if (log.isDebugEnabled) {
                    log.debug("Getting lock? $it")
                }
                if (!it) {
                    error("Failed to get lock")
                }
                it
            }.retryWhen(
                Retry.backoff(times, Duration.ofMillis(RETRY_INTERVAL_MS.toLong()))
            ).timeout(timeOut)
    }

    private fun releaseLock(): Boolean {
        return locked.compareAndExchange(true, false)
    }
}

abstract class AbstractAdminBizClient(private val config: XxlAdminSiteProperties) {
    private val client: WebClient = WebClientProvider.getClient(config)

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AbstractAdminBizClient::class.java)
        private const val MAX_RETRIES = 5
        private val locker: Locker = Locker()
    }

    private fun doLogin(): Mono<Boolean> {
        return Mono.just(config).flatMap { props ->
            // check if already logged
            client.get().uri("/").retrieve().bodyToMono<String>().defaultIfEmpty("")
                .flatMap {
                    if (it.isNotBlank()) {
                        Mono.just(true)
                    } else {
                        val loginName = props.loginName
                        val password = props.loginPassword
                        if (loginName.isEmpty() || password.isEmpty()) {
                            error(LoginException("Empty loginName or empty loginPassword"))
                        }
                        val response = request(
                            Constants.URI_LOGIN_HANDLER,
                            BodyInserters.fromFormData("userName", loginName).with("password", password),
                            CommonAdminApiResponse::class.java,
                            HttpMethod.POST.name,
                            contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                            autoLogin = false
                        ).onErrorResume { error ->
                            log.error("Login failed", error)
                            Mono.just(CommonAdminApiResponse(500, "Not logged in because $error"))
                        }
                        locker.lock(Duration.ofSeconds(5), response.map { data ->
                            data.code == Constants.STATUS_CODE_OK
                        })
                    }
                }
        }
    }

    protected fun <T> request(
        uri: String,
        bodyInserter: BodyInserter<*, in ClientHttpRequest>?,
        responseBodyType: Class<T>,
        method: String = "GET",
        timeout: Duration = Duration.ofSeconds(10),
        contentType: String = "application/json",
        autoLogin: Boolean = true,
        responseMediaType: MediaType = MediaType.APPLICATION_JSON
    ): Mono<T> {
        return requestInternal(
            uri,
            bodyInserter,
            responseBodyType,
            method,
            timeout,
            contentType,
            autoLogin,
            responseMediaType
        )
    }

    private fun <T> requestInternal(
        uri: String,
        bodyInserter: BodyInserter<*, in ClientHttpRequest>?,
        responseBodyType: Class<T>,
        method: String = "GET",
        timeout: Duration = Duration.ofSeconds(10),
        contentType: String = "application/json",
        autoLogin: Boolean = true,
        responseMediaType: MediaType = MediaType.APPLICATION_JSON,
        triedTimes: Int = 0
    ): Mono<T> {
        val request = client
            .method(HttpMethod.resolve(method) ?: HttpMethod.GET)
            .uri(uri).accept(responseMediaType)
        val response = if (Objects.nonNull(bodyInserter)) {
            request.contentType(MediaType.parseMediaType(contentType))
                .body(bodyInserter!!).retrieve()
        } else {
            request.retrieve()
        }
        return response.onStatus({ it.is3xxRedirection },
            {
                val redirectLocation = it.headers().asHttpHeaders().location?.toString()
                log.warn("Redirecting to $redirectLocation")
                if ((redirectLocation ?: "").endsWith(Constants.URI_LOGIN, ignoreCase = true)) {
                    error(LoginException("Login needed"))
                } else {
                    error(ApiInvokeException("Unexpected redirect"))
                }
            })
            .bodyToMono(responseBodyType)
            .onErrorResume { error ->
                log.warn("Not logged in", error)
                if (autoLogin && triedTimes < MAX_RETRIES) {
                    return@onErrorResume doLogin().flatMap { ok ->
                        if (ok) {
                            requestInternal(
                                uri,
                                bodyInserter,
                                responseBodyType,
                                method,
                                timeout,
                                contentType,
                                autoLogin,
                                responseMediaType,
                                triedTimes + 1
                            )
                        } else {
                            error(LoginException("Oops, login failed"))
                        }
                    }
                } else {
                    error(LoginException("No need to login or tried times $triedTimes >= max=$MAX_RETRIES"))
                }
            }
            .timeout(timeout)
    }
}

class ReactiveJobGroupBizImpl(config: XxlAdminSiteProperties) : ReactiveJobGroupBiz, AbstractAdminBizClient(config) {
    override fun query(appName: String?, title: String?, offset: Int?, count: Int?): Mono<JobGroupListDto> {
        val form = BodyInserters.fromFormData("appname", URLEncoder.encode(appName ?: "", Constants.UTF_8))
            .with("title", URLEncoder.encode(title ?: "", Constants.UTF_8))
            .with("start", "${offset ?: 0}").with("length", "${count ?: 1000}")
        return request(
            ClientConstants.URI_JOB_GROUP_LIST,
            form,
            JobGroupListDto::class.java,
            "POST",
            contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE
        )
    }

    override fun create(appName: String?, title: String?, registerType: Int?, addressList: String?): Mono<JobGroupDto> {
        return internalCreate(ClientConstants.URI_JOB_GROUP_CREATE, 0, appName, title, registerType, addressList)
    }

    override fun update(
        id: Int,
        appName: String?,
        title: String?,
        registerType: Int?,
        addressList: String?
    ): Mono<JobGroupDto> {
        return internalCreate(ClientConstants.URI_JOB_GROUP_UPDATE, id, appName, title, registerType, addressList)
    }

    override fun delete(id: Long?): Mono<JobGroupListDto> {
        val formContent = BodyInserters.fromFormData("id", "${id ?: 0}")
        return request(
            ClientConstants.URI_JOB_GROUP_REMOVE, formContent, GeneralApiResponse::class.java, "POST",
            contentType = Constants.CONTENT_TYPE_URL_FORM_ENCODED
        ).flatMap {
            if (!it.ok()) {
                error(ApiInvokeException("Failed to delete job group id=$id"))
            }
            this.query(null, null, null, null)
        }
    }

    private fun internalCreate(
        uri: String,
        id: Int,
        appName: String?,
        title: String?,
        registerType: Int?,
        addressList: String?
    ): Mono<JobGroupDto> {
        val formContent = BodyInserters.fromFormData(
            "appname",
            URLEncoder.encode(appName ?: "", Constants.UTF_8)
        ).with("title", URLEncoder.encode(title ?: "", Constants.UTF_8))
            .with("addressType", "${registerType ?: 0}")
            .with("addressList", addressList ?: "")
        if (id > 0) {
            formContent.with("id", "$id")
        }
        return request(
            uri,
            formContent,
            GeneralApiResponse::class.java,
            "POST",
            contentType = Constants.CONTENT_TYPE_URL_FORM_ENCODED
        ).flatMap {
            if (!it.ok()) {
                error(ApiInvokeException("Failed to create/save job group name=$appName registerType=$registerType id=$id error=${it.msg}"))
            }
            query(appName, title, null, null).mapNotNull { groupList ->
                groupList.data.firstOrNull()
            }
        }
    }
}

class ReactiveJobInfoClientImpl(config: XxlAdminSiteProperties) : ReactiveJobInfoBiz, AbstractAdminBizClient(config) {
    override fun query(
        jobGroupId: Int,
        triggerStatus: Int,
        jobDesc: String?,
        execHandler: String?,
        author: String?,
        offset: Int?,
        count: Int?
    ): Mono<JobInfoListDto> {
        val formContent = BodyInserters
            .fromFormData("jobGroup", "$jobGroupId")
            .with("triggerStatus", "$triggerStatus")
            .with("jobDesc", jobDesc ?: "")
            .with("executorHandler", execHandler ?: "")
            .with("author", author ?: "")
            .with("start", "${offset ?: 0}")
            .with("length", "${count ?: 1000}")
        return request(
            ClientConstants.URI_JOB_INFO_LIST,
            formContent,
            JobInfoListDto::class.java,
            "POST",
            contentType = Constants.CONTENT_TYPE_URL_FORM_ENCODED
        )
    }

    private fun getJobInfoById(jobGroupId: Int, jobId: Int): Mono<JobInfoDto> {
        return query(jobGroupId, FlagConstants.JOB_QRY_TRIGGER_STATUS_ALL, null, null, null, null, null)
            .mapNotNull {
                it.data.firstOrNull { dto -> dto.id == jobId }
            }
    }

    override fun create(dto: JobInfoDto?): Mono<JobInfoDto> {
        val body = dto?.let { item ->
            FormUtils.jobInfoDtoAsMap(item).entries.map {
                Pair(it.key, listOf(it.value))
            }
        } ?: arrayListOf()
        if (body.isEmpty()) {
            throw ApiInvokeException("Empty request data for creating job")
        }
        val form = BodyInserters.fromFormData(LinkedMultiValueMap(mutableMapOf(*body.toTypedArray())))
        return request(
            ClientConstants.URI_JOB_INFO_ADD,
            form,
            GeneralApiResponse::class.java,
            "POST",
            contentType = Constants.CONTENT_TYPE_URL_FORM_ENCODED
        ).flatMap {
            if (!it.ok()) {
                error(ApiInvokeException("Failed to create schedule"))
            }
            getJobInfoById(dto?.jobGroup ?: 0, ((it.content ?: "0") as String).toInt())
        }
    }

    override fun update(dto: JobInfoDto?): Mono<JobInfoDto> {
        val body = dto?.let { item ->
            FormUtils.jobInfoDtoAsMap(item).entries.map {
                Pair(it.key, listOf(it.value))
            }
        } ?: arrayListOf()
        if (body.isEmpty()) {
            throw ApiInvokeException("Empty request data for updating job")
        }
        val form =
            BodyInserters.fromFormData(LinkedMultiValueMap(mutableMapOf(*body.toTypedArray())))
                .with("id", "${dto?.id ?: 0}")
        return request(
            ClientConstants.URI_JOB_INFO_UPDATE,
            form,
            GeneralApiResponse::class.java,
            "POST",
            contentType = Constants.CONTENT_TYPE_URL_FORM_ENCODED
        ).flatMap {
            if (!it.ok()) {
                error(ApiInvokeException("Failed to create schedule"))
            }
            getJobInfoById(dto?.jobGroup ?: 0, dto?.id ?: 0)
        }
    }

    override fun remove(id: Int?): Mono<Boolean> {
        return internalActionJob(id ?: 0, ClientConstants.URI_JOB_INFO_REMOVE)
    }

    override fun startJob(id: Int?): Mono<Boolean> {
        return internalActionJob(id ?: 0, ClientConstants.URI_JOB_SCHEDULE_START)
    }

    override fun stopJob(id: Int?): Mono<Boolean> {
        return internalActionJob(id ?: 0, ClientConstants.URI_JOB_SCHEDULE_STOP)
    }

    private fun internalActionJob(jobId: Int, uri: String): Mono<Boolean> {
        val formContent = BodyInserters.fromFormData("id", "$jobId")
        return request(
            uri,
            formContent,
            GeneralApiResponse::class.java,
            "POST",
            contentType = Constants.CONTENT_TYPE_URL_FORM_ENCODED
        ).map {
            it.ok()
        }
    }
}