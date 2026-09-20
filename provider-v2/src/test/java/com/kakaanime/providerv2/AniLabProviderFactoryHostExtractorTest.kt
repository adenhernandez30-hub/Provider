package com.kakaanime.providerv2

import org.junit.Assert.assertTrue
import org.junit.Test

class AniLabProviderFactoryHostExtractorTest {
    @Test
    fun otakudesuStackWiresHostExtractors() {
        val registry = AniLabProviderFactory.otakudesu().extractorRegistry

        assertTrue(registry.find("https://example.blogspot.com/player").any { it.id == "blogger" })
        assertTrue(registry.find("https://filedon.co/d/test").any { it.id == "filedon" })
        assertTrue(registry.find("https://vidhide.com/embed/test").any { it.id == "vidhide" })
        assertTrue(registry.find("https://streamwish.to/e/test").any { it.id == "streamwish-filelions" })
        assertTrue(registry.find("https://filelions.to/v/test").any { it.id == "streamwish-filelions" })
        assertTrue(registry.find("https://uservideo.net/embed/test").any { it.id == "uservideo-family" })
        assertTrue(registry.find("https://mp4upload.com/embed/test").any { it.id == "legacy-host-family" })
    }

    @Test
    fun samehadakuStackWiresHostExtractors() {
        val registry = AniLabProviderFactory.samehadaku().extractorRegistry

        assertTrue(registry.find("https://vidhide.com/embed/test").any { it.id == "vidhide" })
        assertTrue(registry.find("https://filelions.to/v/test").any { it.id == "streamwish-filelions" })
        assertTrue(registry.find("https://odstream.xyz/embed/test").any { it.id == "legacy-host-family" })
    }
}
