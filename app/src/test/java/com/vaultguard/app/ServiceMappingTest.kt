package com.vaultguard.app

import com.vaultguard.app.icon.ServiceMapping
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceMappingTest {

    @Test
    fun testPopularServicesCount() {
        val count = ServiceMapping.getAllKnownServices().size
        assertTrue("Service mapping must have at least 30 popular services, found $count", count >= 30)
    }

    @Test
    fun testFindService() {
        assertNotNull(ServiceMapping.findService("Gemini"))
        assertNotNull(ServiceMapping.findService("OpenAI"))
        assertNotNull(ServiceMapping.findService("GitHub"))
        assertNotNull(ServiceMapping.findService("github.com"))
        assertNotNull(ServiceMapping.findService("Discord"))
        assertNotNull(ServiceMapping.findService("Slack"))
    }
}
