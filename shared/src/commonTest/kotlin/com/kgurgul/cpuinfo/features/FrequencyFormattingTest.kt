package com.kgurgul.cpuinfo.features

import com.kgurgul.cpuinfo.utils.formatHz
import com.kgurgul.cpuinfo.utils.ltr
import kotlin.test.Test
import kotlin.test.assertEquals

class FrequencyFormattingTest {
    @Test fun zeroMinimumUsesMHzAndIsolated() {
        assertEquals("\u20660 MHz\u2069", "0 MHz".ltr())
    }

    @Test fun twoThousandMHzFormatsAsIsolatedGHz() {
        assertEquals("\u20662.0 GHz\u2069", formatHz(2_000_000_000))
    }
}
