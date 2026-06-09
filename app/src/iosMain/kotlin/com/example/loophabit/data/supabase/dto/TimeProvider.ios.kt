package com.example.loophabit.data.supabase.dto

actual fun currentTimestamp(): String = kotlinx.datetime.Clock.System.now().toString()
