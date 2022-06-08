package top.nb6.scheduler.xxl.utils

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
    }
}