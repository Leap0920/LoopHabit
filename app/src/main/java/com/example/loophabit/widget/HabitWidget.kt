package com.example.loophabit.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.loophabit.LoopHabitApp
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

        val incompleteHabits by app.repository.getIncompleteHabitsOfToday(todayDate)
            .collectAsState(initial = emptyList())
        val loopIndex by app.repository.loopIndexFlow.collectAsState(initial = 0)

        val size = incompleteHabits.size
        val activeIndex = if (size > 0) ((loopIndex % size) + size) % size else 0
        val activeHabit = if (size > 0) incompleteHabits[activeIndex] else null

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (activeHabit != null) {
                // Background cards to represent stacking depth
                if (size >= 3) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth(0.85f)
                            .height(84.dp)
                            .padding(top = 16.dp)
                            .background(ColorProvider(Color(0xFF2C2C2C)))
                            .cornerRadius(16.dp)
                    ) {}
                }
                if (size >= 2) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth(0.9f)
                            .height(92.dp)
                            .padding(top = 8.dp)
                            .background(ColorProvider(Color(0xFF383838)))
                            .cornerRadius(16.dp)
                    ) {}
                }

                // Foreground active habit card
                val parsedColor = try {
                    Color(android.graphics.Color.parseColor(activeHabit.colorHex))
                } catch (e: Exception) {
                    Color(0xFF8338EC)
                }

                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth(0.95f)
                        .height(100.dp)
                        .background(ColorProvider(Color(0xFF1E1E1E)))
                        .cornerRadius(16.dp)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Accent indicator bar
                    Box(
                        modifier = GlanceModifier
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(ColorProvider(parsedColor))
                            .cornerRadius(3.dp)
                    )

                    Spacer(modifier = GlanceModifier.width(10.dp))

                    // Title & Progress Count
                    Column(
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        Text(
                            text = activeHabit.title,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            maxLines = 2
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Card ${activeIndex + 1} of $size",
                            style = TextStyle(
                                color = ColorProvider(Color.Gray),
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Arrow
                        Box(
                            modifier = GlanceModifier
                                .size(28.dp)
                                .background(ColorProvider(Color(0xFF2E2E2E)))
                                .cornerRadius(14.dp)
                                .clickable(
                                    actionRunCallback<CycleIndexAction>(
                                        actionParametersOf(CycleIndexAction.DirectionKey to -1)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "‹",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(4.dp))

                        // Next Arrow
                        Box(
                            modifier = GlanceModifier
                                .size(28.dp)
                                .background(ColorProvider(Color(0xFF2E2E2E)))
                                .cornerRadius(14.dp)
                                .clickable(
                                    actionRunCallback<CycleIndexAction>(
                                        actionParametersOf(CycleIndexAction.DirectionKey to 1)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "›",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        // Check/Complete Button
                        Box(
                            modifier = GlanceModifier
                                .size(36.dp)
                                .background(ColorProvider(parsedColor))
                                .cornerRadius(18.dp)
                                .clickable(
                                    actionRunCallback<CompleteHabitAction>(
                                        actionParametersOf(CompleteHabitAction.HabitIdKey to activeHabit.id)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            } else {
                // Empty State when all habits are complete
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth(0.95f)
                        .height(100.dp)
                        .background(ColorProvider(Color(0xFF1E1E1E)))
                        .cornerRadius(16.dp)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 All done!",
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
