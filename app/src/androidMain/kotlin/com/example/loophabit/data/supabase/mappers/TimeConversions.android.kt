package com.example.loophabit.data.supabase.mappers

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

actual fun epochToIso(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
}

actual fun isoToEpoch(isoString: String): Long {
    return Instant.parse(isoString).toEpochMilli()
}
