package com.example.loophabit.data.supabase.mappers

expect fun epochToIso(epochMillis: Long): String
expect fun isoToEpoch(isoString: String): Long
