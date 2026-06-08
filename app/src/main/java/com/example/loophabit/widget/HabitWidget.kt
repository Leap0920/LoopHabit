package com.example.loophabit.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.loophabit.LoopHabitApp
import com.example.loophabit.MainActivity
import com.example.loophabit.data.Habit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(context)
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        val app = context.applicationContext as LoopHabitApp
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val currentUserId by app.repository.currentUserIdFlow.collectAsState(initial = 0L)
        val incompleteHabits by app.repository.getIncompleteHabitsOfToday(currentUserId, todayDate)
            .collectAsState(initial = emptyList())
        val loopIndex by app.repository.loopIndexFlow.collectAsState(initial = 0)

        val size = incompleteHabits.size
        val activeIndex = if (size > 0) ((loopIndex % size) + size) % size else 0
        val activeHabit = if (size > 0) incompleteHabits[activeIndex] else null

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                if (activeHabit != null) {
                    putExtra("focus_habit_id", activeHabit.id)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            if (currentUserId == 0L) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF1E1E1E)))
                        .cornerRadius(24.dp)
                        .clickable(actionStartActivity(intent))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Account Logged Out",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Please log in to track habits.",
                        style = TextStyle(
                            color = ColorProvider(Color.Gray),
                            fontSize = 11.sp
                        )
                    )
                }
            } else if (activeHabit != null) {
                val parsedColor = try {
                    Color(android.graphics.Color.parseColor(activeHabit.colorHex))
                } catch (e: Exception) {
                    Color(0xFF8338EC)
                }

                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF1E1E1E)))
                        .cornerRadius(24.dp)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .clickable(actionStartActivity(intent))
                    ) {
                        Column(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(10.dp)
                                        .background(ColorProvider(parsedColor))
                                        .cornerRadius(5.dp)
                                ) {}
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Text(
                                    text = "FOCUS MODE",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFF8338EC)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(6.dp))
                            Text(
                                text = activeHabit.title,
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = "Tap to open app & focus",
                                style = TextStyle(
                                    color = ColorProvider(Color.Gray),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = GlanceModifier.width(8.dp))

                    Box(
                        modifier = GlanceModifier
                            .size(36.dp)
                            .background(ColorProvider(parsedColor.copy(alpha = 0.2f)))
                            .cornerRadius(18.dp)
                            .clickable(actionRunCallback<CompleteHabitAction>(
                                actionParametersOf(CompleteHabitAction.HabitIdKey to activeHabit.id)
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            style = TextStyle(
                                color = ColorProvider(parsedColor),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                    }
                }
            } else {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF1E1E1E)))
                        .cornerRadius(24.dp)
                        .clickable(actionStartActivity(intent))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "All done!",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "You've completed all habits today.",
                        style = TextStyle(
                            color = ColorProvider(Color.Gray),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
