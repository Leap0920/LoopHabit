package com.example.loophabit.data.supabase.mappers

import kotlinx.datetime.Instant

actual fun epochToIso(epochMillis: Long): String {
    return Instant.fromEpochMilliseconds(epochMillis).toString()
}

actual fun isoToEpoch(isoString: String): Long {
    return Instant.parse(isoString).toEpochMilliseconds()
}
