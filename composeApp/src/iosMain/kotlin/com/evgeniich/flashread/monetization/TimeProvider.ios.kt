package com.evgeniich.flashread.monetization

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of [TimeProvider].
 *
 * Uses Foundation APIs for time operations.
 */
actual object TimeProvider {

    private val dateFormatter: NSDateFormatter by lazy {
        NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd"
        }
    }

    /**
     * Returns the current time as epoch milliseconds.
     */
    actual fun currentTimeMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

    /**
     * Returns the current date as YYYY-MM-DD string.
     */
    actual fun currentDateString(): String = dateFormatter.stringFromDate(NSDate())
}
