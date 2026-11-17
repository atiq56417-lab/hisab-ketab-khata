package com.hisabkitab.khata.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * All money in the app is stored as a Long number of paisa (1 rupee = 100 paisa).
 * No Double/Float is ever used for balances.
 */
object Money {
    /** Parses user input such as "1,250.50". Returns null when empty, invalid, zero or negative. */
    fun parsePositive(text: String): Long? = parseSigned(text, allowBlank = false)?.takeIf { it > 0 }

    /** Parses a signed amount. Blank gives 0 when [allowBlank] is true. Returns null when invalid. */
    fun parseSigned(text: String, allowBlank: Boolean = true): Long? {
        val t = text.trim().replace(",", "")
        if (t.isEmpty()) return if (allowBlank) 0L else null
        return try {
            BigDecimal(t).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        } catch (e: Exception) {
            null
        }
    }

    fun format(paisa: Long): String =
        DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).format(BigDecimal.valueOf(paisa, 2))
}

object Formats {
    fun dateTime(ts: Long): String = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(ts))
    fun fileStamp(): String = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ENGLISH).format(Date())
}
