package com.fuku856.povomanager.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class UrgencyTest {

    @Test
    fun `履歴なし(null)はNONE`() {
        assertEquals(Urgency.NONE, urgencyOf(null))
    }

    @Test
    fun `31日以上はSAFE`() {
        assertEquals(Urgency.SAFE, urgencyOf(180))
        assertEquals(Urgency.SAFE, urgencyOf(31))
    }

    @Test
    fun `15から30日はWARNING`() {
        assertEquals(Urgency.WARNING, urgencyOf(30))
        assertEquals(Urgency.WARNING, urgencyOf(15))
    }

    @Test
    fun `8から14日はALERT`() {
        assertEquals(Urgency.ALERT, urgencyOf(14))
        assertEquals(Urgency.ALERT, urgencyOf(8))
    }

    @Test
    fun `1から7日と本日(0)はDANGER`() {
        assertEquals(Urgency.DANGER, urgencyOf(7))
        assertEquals(Urgency.DANGER, urgencyOf(1))
        assertEquals(Urgency.DANGER, urgencyOf(0))
    }

    @Test
    fun `超過(負)はEXPIRED`() {
        assertEquals(Urgency.EXPIRED, urgencyOf(-1))
    }
}
