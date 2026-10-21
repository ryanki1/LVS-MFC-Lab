package com.lvslab.lvs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LvsServiceMarkerTest {
    @Test
    fun `reports module name`() {
        assertEquals("lvs-service", LvsServiceMarker().runtimeName())
    }
}
