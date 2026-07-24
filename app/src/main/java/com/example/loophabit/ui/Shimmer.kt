package com.example.loophabit.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** (B1) Shimmer placeholder modifier for loading states. */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "shimmerTranslate"
    )
    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.15f),
        Color.Gray.copy(alpha = 0.35f),
        Color.Gray.copy(alpha = 0.15f)
    )
    background(
        Brush.linearGradient(
            shimmerColors,
            start = Offset(translateAnim - 200f, 0f),
            end = Offset(translateAnim, 0f)
        )
    )
}

@Composable
fun HabitCardSkeleton() {
    Box(
        Modifier
            .fillMaxWidth(0.9f)
            .height(280.dp)
            .clip(RoundedCornerShape(28.dp))
            .shimmer()
    )
}

@Composable
fun CompletedRowSkeleton() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .shimmer()
    )
}
