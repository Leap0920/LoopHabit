package com.example.loophabit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loophabit.data.Habit
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeableCard(
    habit: Habit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Float) -> Unit
) {
    val swipeOffset = remember(habit.id) { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 120.dp.toPx() }

    Box(
        modifier = modifier
            .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
            .graphicsLayer {
                val rotation = (swipeOffset.value / 40f)
                val alpha = 1f - (abs(swipeOffset.value) / 1200f).coerceIn(0f, 0.8f)
                this.rotationZ = rotation
                this.alpha = alpha
            }
            .pointerInput(habit.id) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            val targetOffset = swipeOffset.value
                            if (targetOffset > swipeThreshold) {
                                // Complete (Swipe Right)
                                swipeOffset.animateTo(
                                    targetValue = 1000f,
                                    animationSpec = tween(durationMillis = 200)
                                )
                                onSwipeRight()
                            } else if (targetOffset < -swipeThreshold) {
                                // Skip/Cycle (Swipe Left)
                                swipeOffset.animateTo(
                                    targetValue = -1000f,
                                    animationSpec = tween(durationMillis = 200)
                                )
                                onSwipeLeft()
                            } else {
                                // Return
                                swipeOffset.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            swipeOffset.animateTo(
                                0f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Use the pointerInput coroutine context directly - no need to launch
                        coroutineScope.launch {
                            swipeOffset.snapTo(swipeOffset.value + dragAmount.x)
                        }
                    }
                )
            }
    ) {
        content(swipeOffset.value)
    }
}

@Composable
fun HabitCardContent(
    habit: Habit,
    isTop: Boolean,
    modifier: Modifier = Modifier,
    swipeOffset: Float = 0f
) {
    val parsedColor = remember(habit.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(habit.colorHex))
        } catch (e: Exception) {
            Color(0xFF8338EC) // Fallback purple
        }
    }

    val swipeIndicatorAlpha = (abs(swipeOffset) / 200f).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTop) 6.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.85f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            parsedColor.copy(alpha = 0.15f),
                            parsedColor.copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Top row: Color tag and visual indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(parsedColor)
                    )
                    if (isTop) {
                        Text(
                            text = "SWIPE TO COMPLETE →",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = parsedColor.copy(alpha = 0.6f)
                        )
                    }
                }

                // Middle: Large title
                Column {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom row: Interactive guides
                if (isTop) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "← Swipe Left to Skip",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Swipe indicator",
                            tint = parsedColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    )
                }
            }

            // Visual feedback overlay based on swipe
            if (isTop && swipeOffset != 0f) {
                val overlayColor = if (swipeOffset > 0) Color(0xFF06D6A0) else Color(0xFF8338EC)
                val icon = if (swipeOffset > 0) Icons.Outlined.Check else Icons.Outlined.Close
                val textLabel = if (swipeOffset > 0) "COMPLETE" else "SKIP"

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(overlayColor.copy(alpha = 0.12f * swipeIndicatorAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .graphicsLayer {
                                this.alpha = swipeIndicatorAlpha
                                this.scaleX = 0.8f + (0.2f * swipeIndicatorAlpha)
                                this.scaleY = 0.8f + (0.2f * swipeIndicatorAlpha)
                            }
                            .background(
                                overlayColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(icon, contentDescription = textLabel, tint = Color.White)
                        Text(
                            text = textLabel,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedHabitRow(
    habit: Habit,
    onUncomplete: () -> Unit
) {
    val parsedColor = remember(habit.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(habit.colorHex))
        } catch (e: Exception) {
            Color(0xFF8338EC)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                )
                Text(
                    text = habit.title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onUncomplete,
                modifier = Modifier
                    .size(24.dp)
                    .background(parsedColor.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Undo completion",
                    tint = parsedColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
