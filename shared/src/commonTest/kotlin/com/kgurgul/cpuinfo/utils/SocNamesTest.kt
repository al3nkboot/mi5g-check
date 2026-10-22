package com.kgurgul.cpuinfo.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class SocNamesTest {
    @Test
    fun mapsKnownCode() {
        assertEquals("Snapdragon 8 Elite (SM8750)", SocNames.withMarketingName("Qualcomm SM8750"))
        assertEquals("Tensor G3 (ZUMA)", SocNames.withMarketingName("zuma"))
    }

    @Test
    fun keepsUnknownOrAlreadyNamed() {
        assertEquals("Foo X1", SocNames.withMarketingName("Foo X1"))
        assertEquals(
            "Qualcomm Snapdragon 8 Elite SM8750",
            SocNames.withMarketingName("Qualcomm Snapdragon 8 Elite SM8750"),
        )
    }
}
