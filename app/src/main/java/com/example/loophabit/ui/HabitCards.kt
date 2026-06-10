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
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    // Use plain float state for drag tracking (synchronous, no coroutine overhead)
    var dragOffset by remember(habit.id) { mutableFloatStateOf(0f) }
    val animatable = remember(habit.id) { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 120.dp.toPx() }

    Box(
        modifier = modifier
            .offset { IntOffset(dragOffset.roundToInt(), 0) }
            .graphicsLayer {
                val rotation = (dragOffset / 40f)
                val alpha = 1f - (abs(dragOffset) / 1200f).coerceIn(0f, 0.8f)
                this.rotationZ = rotation
                this.alpha = alpha
            }
            .pointerInput(habit.id) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (dragOffset > swipeThreshold) {
                                animatable.snapTo(dragOffset)
                                animatable.animateTo(
                                    targetValue = 1000f,
                                    animationSpec = tween(durationMillis = 200)
                                )
                                dragOffset = 0f
                                onSwipeRight()
                            } else if (dragOffset < -swipeThreshold) {
                                animatable.snapTo(dragOffset)
                                animatable.animateTo(
                                    targetValue = -1000f,
                                    animationSpec = tween(durationMillis = 200)
                                )
                                dragOffset = 0f
                                onSwipeLeft()
                            } else {
                                animatable.snapTo(dragOffset)
                                animatable.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                                dragOffset = 0f
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            animatable.snapTo(dragOffset)
                            animatable.animateTo(
                                0f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                            dragOffset = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Direct state mutation — no coroutine launch per pixel
                        dragOffset += dragAmount.x
                    }
                )
            }
    ) {
        content(dragOffset)
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
    manualMinutes: Int,
    showManualTimeAction: Boolean,
    onEditManualTime: () -> Unit,
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Color indicator dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(parsedColor)
            )

            // Habit info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showManualTimeAction && manualMinutes > 0) {
                    Text(
                        text = "${manualMinutes}m focus",
                        fontSize = 11.sp,
                        color = parsedColor.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            // Action buttons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showManualTimeAction) {
                    IconButton(
                        onClick = onEditManualTime,
                        modifier = Modifier
                            .size(34.dp)
                            .background(parsedColor.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Add focus time",
                            tint = parsedColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onUncomplete,
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Undo",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
