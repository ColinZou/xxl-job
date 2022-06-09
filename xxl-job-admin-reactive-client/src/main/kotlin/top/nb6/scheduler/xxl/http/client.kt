package top.nb6.scheduler.xxl.http

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.DigestUtils
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock

class WebClientProvider {
    companion object {
        private val lock = ReentrantLock()
        private val clientMap = mutableMapOf<String, WebClient>()
        private const val timeout = 5
        private val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout * 1000)
            .responseTimeout(Duration.ofSeconds(timeout.toLong()))
            .doOnConnected {
                it.addHandlerLast(ReadTimeoutHandler(timeout))
                    .addHandlerLast(WriteTimeoutHandler(timeout))
            }
        private val httpConnector = ReactorClientHttpConnector(httpClient)
        fun getClient(config: XxlAdminSiteProperties): WebClient {
            val key = DigestUtils.md5DigestAsHex("${config.apiPrefix}-${config.loginName}".encodeToByteArray())
            return if (clientMap.containsKey(key)) {
                clientMap[key]!!
            } else {
                lock.lock()
                try {
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

 