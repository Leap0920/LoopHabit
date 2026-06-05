package com.example.loophabit.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.loophabit.LoopHabitApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CompleteHabitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[HabitIdKey] ?: return
        val app = context.applicationContext as LoopHabitApp
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        app.repository.completeHabit(habitId, todayDate)

        // Request widget UI update
        HabitWidget().update(context, glanceId)
    }

    companion object {
        val HabitIdKey = ActionParameters.Key<Long>("habit_id")
    }
}

class CycleIndexAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val direction = parameters[DirectionKey] ?: return
        val app = context.applicationContext as LoopHabitApp
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        app.repository.cycleIndex(direction, todayDate)

        // Request widget UI update
        HabitWidget().update(context, glanceId)
    }

    companion object {
        val DirectionKey = ActionParameters.Key<Int>("direction")
    }
}
