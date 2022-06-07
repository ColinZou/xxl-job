package top.nb6.scheduler.xxl.utils

class UrlUtils {
    companion object {
        const val SLASH: String = "/"
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

 