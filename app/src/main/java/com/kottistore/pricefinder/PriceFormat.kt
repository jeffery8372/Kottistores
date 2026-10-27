package com.kottistore.pricefinder

import java.util.Locale

fun formatPrice(rate: Double?): String {
    if (rate == null) return "unknown"
    return if (rate == rate.toLong().toDouble()) {
        rate.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", rate)
    }
}
