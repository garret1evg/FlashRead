package com.evgeniich.flashread.monetization

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Android implementation of [TimeProvider].
 *
 * Uses Java time APIs for accurate time measurements.
 */
actual object TimeProvider {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // YYYY-MM-DD

    /**
     * Returns the current time as epoch milliseconds.
     */
    actual fun currentTimeMs(): Long = System.currentTimeMillis()

    /**
     * Returns the current date as YYYY-MM-DD string.
     */
    actual fun currentDateString(): String = LocalDate.now().format(dateFormatter)
}
