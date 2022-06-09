package top.nb6.scheduler.xxl.utils

import com.xxl.job.core.biz.model.JobInfoDto
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

class UrlUtils {
    companion object {
        private const val SLASH: String = "/"
        fun append(vararg parts: String): String {
            return parts.reduce { first, second ->
                val one = if (first.endsWith(SLASH)) {
                    first
                } else {
                    first + SLASH
                }
                val two = if (second.startsWith(SLASH)) {
                    second.subSequence(IntRange(1, second.length - 1))
                } else {
                    second
                }
                one + two
            }
        }
    }
}

class FormUtils {
    companion object {
        private const val FIELD_DELIMITER = "&"
        fun build(params: Map<String, String>): String {
            return params.entries
                .filter { Objects.nonNull(it.value) }
                .map {
                    "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8)}"
                }.reduce { a, b ->
                    "$a$FIELD_DELIMITER$b"
                }
        }
        fun jobInfoDtoAsMap(dto: JobInfoDto): Map<String, String> {
            return mapOf(
                "jobGroup" to dto.jobGroup.toString(),
                "jobDesc" to dto.jobDesc,
                "author" to dto.author,
                "alarmEmail" to dto.alarmEmail,
                "scheduleType" to dto.scheduleType.name,
                "scheduleConf" to dto.scheduleConf,
                "cronGen_display" to dto.scheduleConf,
                "schedule_conf_CRON" to dto.scheduleConf,
                "schedule_conf_FIX_RATE" to "",
                "schedule_conf_FIX_DELAY" to "",
                "glueType" to dto.glueType.name,
                "executorHandler" to dto.executorHandler,
                "executorParam" to dto.executorParam,
                "executorRouteStrategy" to dto.executorRouteStrategy.name,
                "childJobId" to dto.childJobId,
                "misfireStrategy" to dto.misfireStrategy.name,
                "executorBlockStrategy" to dto.executorBlockStrategy.name,
                "executorTimeout" to dto.executorTimeout.toString(),
                "executorFailRetryCount" to dto.executorFailRetryCount.toString(),
                "glueRemark" to dto.glueRemark,
                "glueSource" to dto.glueSource
            )
        }
    }
}