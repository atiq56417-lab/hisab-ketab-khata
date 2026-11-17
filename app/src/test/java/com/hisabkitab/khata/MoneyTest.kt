package com.hisabkitab.khata

import com.hisabkitab.khata.data.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun parsesWholeAndDecimal() {
        assertEquals(125050L, Money.parsePositive("1,250.50"))
        assertEquals(50000L, Money.parsePositive("500"))
    }

    @Test fun rejectsInvalid() {
        assertNull(Money.parsePositive("0"))
        assertNull(Money.parsePositive("-5"))
        assertNull(Money.parsePositive("abc"))
        assertNull(Money.parsePositive(""))
    }

    @Test fun signedAllowsNegativeAndBlank() {
        assertEquals(-25000L, Money.parseSigned("-250"))
        assertEquals(0L, Money.parseSigned(""))
    }

    @Test fun formatsWithoutFloatingPointErrors() {
        assertEquals("1,250.50", Money.format(125050L))
        assertEquals("0.10", Money.format(10L))
        // 0.1 + 0.2 style sums are exact because everything is whole paisa
        assertEquals("0.30", Money.format(Money.parsePositive("0.10")!! + Money.parsePositive("0.20")!!))
    }
}
