package top.nb6.scheduler.xxl.biz

import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal class LockerTest {
    companion object {
        val executorService = ThreadPoolExecutor(4, 4, 60, TimeUnit.MINUTES, ArrayBlockingQueue(1000))
        val counter = AtomicInteger(0)
        val log: Logger = LoggerFactory.getLogger(LockerTest::class.java)
    }

    private fun testMethod(locker: Locker, id: Int) {
        locker.lock(Duration.ofSeconds(1), Mono.just(id)).block()?.let {
            println("Handing $it")
        }
    }

    @Test
    fun lock() {
        val locker = Locker()
        IntRange(1, 5).map {
            executorService.submit { this.testMethod(locker, it) }
        }.forEach {
            it.get()
        }
    }

    private fun producer(): Mono<Boolean> {
        return Mono.fromCallable {
            val value = counter.incrementAndGet()
            log.info("Current counter is $value")
            value > 10
        }
    }

    @Test
    fun testRetry() {
        producer().filter {
            if (!it) {
                error("WTF")
            }
            it
        }.retryWhen(Retry.fixedDelay(20, Duration.ofMillis(100)))
            .timeout(Duration.ofSeconds(1))
            .`as`(StepVerifier::create).expectNext(true).verifyComplete()
    }
}