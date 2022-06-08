package top.nb6.scheduler.xxl.http

import org.junit.jupiter.api.Assertions
import java.net.http.HttpRequest
import java.net.http.HttpResponse

internal class XxlAdminHttpClientTest {
    companion object {
        val adminSiteConfigProps = XxlAdminSiteProperties(
            "http://localhost:8080/xxl-job-admin",
            "admin", "123456"
        )
        val client: XxlAdminHttpClient = XxlAdminHttpClient(adminSiteConfigProps)
    }

    @org.junit.jupiter.api.Test
    fun testDoLogin() {
        val result = client.doLogin()
        Assertions.assertTrue(result)
        val response = client.request(
            "/jobinfo",
            HttpResponse.BodyHandlers.ofString(Constants.UTF_8),
            HttpRequest.BodyPublishers.noBody(), autoLogin = false
        )
        Assertions.assertNotNull(response)
        Assertions.assertEquals(200, response.statusCode())
    }
}