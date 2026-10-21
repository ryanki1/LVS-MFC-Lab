package com.lvslab.mfr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MfrServiceMarkerTest {
    @Test
    void reportsModuleName() {
        assertEquals("mfr-service", new MfrServiceMarker().runtimeName());
    }
}
