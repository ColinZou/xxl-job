package top.nb6.scheduler.xxl.http

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets


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

open class CommonAdminApiResponse(val code: Long?, val msg: String? = null) {
    fun ok(): Boolean {
        return code == Constants.STATUS_CODE_OK
    }
}

class ClientConstants {
    companion object {
        const val URI_JOB_GROUP_LIST = "/jobgroup/pageList"
        const val URI_JOB_GROUP_CREATE = "/jobgroup/save"
        const val URI_JOB_GROUP_UPDATE = "/jobgroup/update"
        const val URI_JOB_GROUP_REMOVE = "/jobgroup/remove"
        const val URI_JOB_INFO_LIST = "/jobinfo/pageList"
        const val URI_JOB_INFO_ADD = "/jobinfo/add"
        const val URI_JOB_INFO_UPDATE = "/jobinfo/update"
        const val URI_JOB_INFO_REMOVE = "/jobinfo/remove"
        const val URI_JOB_SCHEDULE_START = "/jobinfo/start"
        const val URI_JOB_SCHEDULE_STOP = "/jobinfo/stop"
        val utf8: Charset = StandardCharsets.UTF_8
    }
}

class JobInfoCreationResponse(code: Long, msg: String?, val content: String?) : CommonAdminApiResponse(code, msg)

