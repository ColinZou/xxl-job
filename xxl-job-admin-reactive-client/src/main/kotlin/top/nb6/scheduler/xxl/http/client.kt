package top.nb6.scheduler.xxl.http

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.util.HttpCookieStore
import org.springframework.http.client.reactive.JettyClientHttpConnector
import org.springframework.util.DigestUtils
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.locks.ReentrantLock

class WebClientProvider {
    companion object {
        private val lock = ReentrantLock()
        private val clientMap = mutableMapOf<String, WebClient>()
        private const val timeout = 5
        fun getClient(config: XxlAdminSiteProperties): WebClient {
            val key = DigestUtils.md5DigestAsHex("${config.apiPrefix}-${config.loginName}".encodeToByteArray())
            return if (clientMap.containsKey(key)) {
                clientMap[key]!!
            } else {
                lock.lock()
                try {
                    val httpClient = HttpClient()
                    val secondMills = 1000L
                    httpClient.addressResolutionTimeout = timeout * secondMills
                    httpClient.connectTimeout = timeout * secondMills
                    httpClient.isFollowRedirects = false
                    httpClient.cookieStore = HttpCookieStore()
                    val httpConnector = JettyClientHttpConnector(httpClient)
                    val client = WebClient.builder()
                        .baseUrl(config.apiPrefix)
                        .clientConnector(httpConnector).build()
                    clientMap[key] = client
                    client
                } finally {
                    lock.unlock()
                }
            }
        }
    }
}

 