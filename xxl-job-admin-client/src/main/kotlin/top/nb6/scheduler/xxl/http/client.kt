package top.nb6.scheduler.xxl.http

import com.google.gson.Gson
import com.xxl.job.core.biz.exceptions.ApiInvokeException
import com.xxl.job.core.biz.exceptions.LoginFailedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import top.nb6.scheduler.xxl.utils.UrlUtils
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock

open class XxlAdminSiteProperties(val apiPrefix: String, val loginName: String, val loginPassword: String)
class Constants {
    companion object {
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val URI_LOGIN = "/toLogin"
        const val URI_LOGIN_HANDLER = "/login"
        const val HEADER_LOCATION = "Location"
        const val CONTENT_TYPE_JSON = "application/json"
        const val CONTENT_TYPE_URL_FORM_ENCODED = "application/x-www-form-urlencoded"
        val UTF_8: Charset = StandardCharsets.UTF_8
        const val JSON_START_TAG = "{"
        const val STATUS_CODE_OK = 200L
        const val MAX_RETRY = 5
    }
}

open class CommonAdminApiResponse(val code: Long, val msg: String? = null)

class XxlAdminHttpClient(private val adminSiteProperties: XxlAdminSiteProperties) {
    private val httpClient: HttpClient

    companion object {
        private val LOGIN_LOCK = ReentrantLock(false)
        private val log: Logger = LoggerFactory.getLogger(XxlAdminHttpClient::class.java)
    }

    init {
        val cookieHandler = CookieManager()
        cookieHandler.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        val clientBuilder = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5L))
            .cookieHandler(cookieHandler)
        httpClient = clientBuilder.build()
    }

    private fun <T> needLogin(response: HttpResponse<T>): Boolean {
        val location = response.headers().map().entries.firstOrNull() {
            it.key.equals(
                Constants.HEADER_LOCATION,
                true
            )
        }?.value?.firstOrNull()
        return location?.endsWith(Constants.URI_LOGIN, true) ?: false
    }

    /**
     * 检查某个响应是否为错误响应
     */
    fun isErrorJsonResponse(jsonContent: String): Pair<Boolean, String?> {
        return if (jsonContent.startsWith(Constants.JSON_START_TAG)) {
            val responseObj = Gson().fromJson(jsonContent, CommonAdminApiResponse::class.java)
            return Pair(responseObj.code == Constants.STATUS_CODE_OK, responseObj.msg)
        } else {
            Pair(false, "Not a json response")
        }
    }

    @Throws(LoginFailedException::class)
    fun doLogin(): Boolean {
        val loginName = adminSiteProperties.loginName
        val loginPassword = adminSiteProperties.loginPassword
        if (loginName.isEmpty() || loginPassword.isEmpty()) {
            throw LoginFailedException("Empty loginName or empty loginPassword")
        }
        LOGIN_LOCK.lock()
        try {
            val loginUri = Constants.URI_LOGIN_HANDLER
            val response = request(
                loginUri, HttpResponse.BodyHandlers.ofString(Constants.UTF_8),
                HttpRequest.BodyPublishers.ofString("userName=$loginName&password=$loginPassword", Constants.UTF_8),
                "POST",
                contentType = Constants.CONTENT_TYPE_URL_FORM_ENCODED,
                autoLogin = false
            )
            val responseBody = response.body()
            if (responseBody.isEmpty() || !responseBody.startsWith(Constants.JSON_START_TAG)) {
                log.error(
                    "Failed to login xxl-job admin site, loginName=$loginName " +
                            "password length=${loginPassword.length}: $responseBody"
                )
                throw LoginFailedException("Failed to login, got bad response")
            }
            if (log.isDebugEnabled) {
                log.debug(
                    "Got login response for xxl-job admin site, loginName=$loginName " +
                            "password length=${loginPassword.length} $responseBody"
                )
            }
            val data = Gson().fromJson<HashMap<String, Any>>(responseBody, java.util.HashMap::class.java)
            val codeKey = "code"
            val code = if (data.containsKey(codeKey)) {
                (data[codeKey] as Double).toLong()
            } else 0L
            return code == Constants.STATUS_CODE_OK
        } finally {
            LOGIN_LOCK.unlock()
        }
    }

    @Throws(LoginFailedException::class)
    fun <T> request(
        uri: String,
        responseBodyHandler: HttpResponse.BodyHandler<T>,
        requestBodyPublisher: HttpRequest.BodyPublisher,
        method: String = "GET",
        timeout: Duration = Duration.ofSeconds(10),
        contentType: String = "application/json",
        autoLogin: Boolean = true
    ): HttpResponse<T> {
        return requestInternal(uri, responseBodyHandler, requestBodyPublisher, method, timeout, contentType, autoLogin)
    }

    private fun <T> requestInternal(
        uri: String,
        responseBodyHandler: HttpResponse.BodyHandler<T>?,
        requestBodyPublisher: HttpRequest.BodyPublisher,
        method: String = "GET",
        timeout: Duration = Duration.ofSeconds(10),
        contentType: String = "application/json",
        autoLogin: Boolean = true, nTimes: Int = 1
    ): HttpResponse<T> {
        if (nTimes > Constants.MAX_RETRY) {
            throw ApiInvokeException("Exceed maximum retried times ${Constants.MAX_RETRY}")
        }
        val finalUri = URI(UrlUtils.append(adminSiteProperties.apiPrefix, uri))
        var requestBuilder = HttpRequest.newBuilder()
            .uri(finalUri)
            .timeout(timeout)
            .method(method, requestBodyPublisher)
        if (!method.equals("get", true)) {
            requestBuilder = requestBuilder.header(Constants.HEADER_CONTENT_TYPE, contentType)
        }
        val request = requestBuilder.build()
        val response = httpClient.send(request, responseBodyHandler)
        return if (autoLogin && needLogin(response)) {
            if (doLogin()) {
                return requestInternal(
                    uri,
                    responseBodyHandler,
                    requestBodyPublisher,
                    method,
                    timeout,
                    contentType,
                    autoLogin,
                    nTimes + 1
                )
            } else {
                throw LoginFailedException()
            }
        } else {
            response
        }
    }
}