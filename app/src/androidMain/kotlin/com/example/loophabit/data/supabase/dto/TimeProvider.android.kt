package com.example.loophabit.data.supabase.dto

import java.time.Instant

actual fun currentTimestamp(): String = Instant.now().toString()
