package top.nb6.scheduler.xxl.biz

import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class LockerTest {
    companion object {
        val executorService = ThreadPoolExecutor(4, 4, 60, TimeUnit.MINUTES, ArrayBlockingQueue(1000))

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
}