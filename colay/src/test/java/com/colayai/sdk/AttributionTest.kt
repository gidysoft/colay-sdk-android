package com.colayai.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttributionTest {

    @Test
    fun installReferrerUtmAndClickId() {
        val a = Attribution.extract("utm_source=google-play&utm_medium=cpc&gclid=Cj0K%2Bx&foo=bar")
        assertEquals("google-play", a["utm_source"])
        assertEquals("cpc", a["utm_medium"])
        assertEquals("Cj0K+x", a["gclid"])
        assertFalse(a.containsKey("foo"))
    }

    @Test
    fun deepLink() {
        val a = Attribution.fromUrl("exampleapp://campaign/november?utm_campaign=black%20friday&fbclid=IwAR1&colay_link=AbC12#x")
        assertEquals("black friday", a["utm_campaign"])
        assertEquals("IwAR1", a["fbclid"])
        assertEquals("AbC12", a["colay_link"])
    }

    @Test
    fun emptyOrQuerylessLink() {
        assertTrue(Attribution.fromUrl(null).isEmpty())
        assertTrue(Attribution.fromUrl("https://example.com/path").isEmpty())
        assertTrue(Attribution.extract("utm_source=&gclid=").isEmpty())
    }

    @Test
    fun clickIdsRemovedWithoutAdsConsent() {
        val a = Attribution.stripClickIds(mapOf("utm_source" to "fb", "fbclid" to "x", "gclid" to "y", "ttclid" to "z"))
        assertEquals(mapOf("utm_source" to "fb"), a)
    }
}
