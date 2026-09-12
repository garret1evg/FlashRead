package com.evgeniich.flashread.monetization

/**
 * Platform-specific time provider for monetization system.
 *
 * Provides current time in milliseconds and formatted date strings
 * without depending on platform-specific APIs in common code.
 */
expect object TimeProvider {

    /**
     * Returns the current time as epoch milliseconds.
     */
    fun currentTimeMs(): Long

    /**
     * Returns the current date as YYYY-MM-DD string.
     */
    fun currentDateString(): String
}
