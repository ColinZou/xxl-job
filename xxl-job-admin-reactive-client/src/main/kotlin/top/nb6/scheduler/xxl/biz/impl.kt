package top.nb6.scheduler.xxl.biz

import com.xxl.job.core.biz.ReactiveJobGroupBiz
import com.xxl.job.core.biz.model.JobGroupDto
import com.xxl.job.core.biz.model.JobGroupListDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ClientHttpRequest
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import top.nb6.scheduler.xxl.http.*
import java.net.URLEncoder
import java.time.Duration
import java.util.*

class GeneralApiResponse(code: Long?, msg: String?, val content: Any? = null) : CommonAdminApiResponse(code, msg)

abstract class AbstractAdminBizClient(private val config: XxlAdminSiteProperties) {
    private val client: WebClient = WebClientProvider.getClient(config)

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AbstractAdminBizClient::class.java)
        private const val MAX_RETRIES = 5
    }

    private fun doLogin(): Mono<Boolean> {
        return Mono.just(config).flatMap { props ->
            val loginName = props.loginName
            val password = props.loginPassword
            if (loginName.isEmpty() || password.isEmpty()) {
                error("Empty loginName or empty loginPassword")
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
            response.map {
                it.code == Constants.STATUS_CODE_OK
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
                    error("Login needed")
                } else {
                    error("Unexpected redirect")
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
                            error("Oops, login failed")
                        }
                    }
                } else {
                    error("No need to login or tried times $triedTimes >= max=$MAX_RETRIES")
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