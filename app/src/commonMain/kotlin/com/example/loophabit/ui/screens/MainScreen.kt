package com.example.loophabit.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loophabit.data.Habit
import com.example.loophabit.data.SyncState
import com.example.loophabit.ui.*
import com.example.loophabit.ui.theme.*

@Composable
fun MainScreen(
    viewModel: LoopHabitViewModel,
    onNavigateToFocus: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAddHabit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier,
        topBar = {
            MainTopBar(
                syncState = viewModel.syncState.collectAsState().value,
                onSettingsClick = onOpenSettings
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onOpenAddHabit,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Tab Row
            MainTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Content
            when (selectedTab) {
                0 -> TodayTab(viewModel)
                1 -> onNavigateToFocus()
                2 -> onNavigateToInsights()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    syncState: SyncState,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "LoopHabit",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                SyncStatusIndicator(syncState)
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

@Composable
private fun MainTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Today", "Focus", "Insights")

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
private fun TodayTab(viewModel: LoopHabitViewModel) {
    val incompleteHabits by viewModel.incompleteHabits.collectAsState()
    val completedHabits by viewModel.completedHabits.collectAsState()
    val loopIndex by viewModel.loopIndex.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Habit Card Stack
        if (incompleteHabits.isNotEmpty()) {
            HabitCardStack(
                habits = incompleteHabits,
                currentIndex = loopIndex,
                onComplete = { habit -> viewModel.completeHabit(habit.id) },
                onNext = { viewModel.nextHabit() },
                onPrev = { viewModel.prevHabit() }
            )
        } else if (completedHabits.isNotEmpty()) {
            // All habits completed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "All habits completed!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // No habits
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No habits yet. Tap + to add one!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Completed habits list
        if (completedHabits.isNotEmpty()) {
            Text(
                text = "Completed Today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            completedHabits.forEach { habit ->
                CompletedHabitRow(
                    habit = habit,
                    onUncomplete = { viewModel.uncompleteHabit(habit.id) }
                )
            }
        }
    }
}

@Composable
private fun HabitCardStack(
    habits: List<Habit>,
    currentIndex: Int,
    onComplete: (Habit) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val safeIndex = currentIndex.coerceIn(0, habits.lastIndex)
    val currentHabit = habits[safeIndex]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        SwipeableCard(
            habit = currentHabit,
            onSwipeRight = { onComplete(currentHabit) },
            onSwipeLeft = onNext
        ) {
            HabitCardContent(habit = currentHabit, isTop = true)
        }

        // Navigation arrows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrev,
                enabled = habits.size > 1
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = onNext,
                enabled = habits.size > 1
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun SyncStatusIndicator(syncState: SyncState) {
    val (icon, tint, description) = when (syncState) {
        is SyncState.Idle -> Triple(
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.primary,
            "Synced"
        )
        is SyncState.Syncing -> Triple(
            Icons.Default.Sync,
            MaterialTheme.colorScheme.tertiary,
            "Syncing: ${syncState.message}"
        )
        is SyncState.Completed -> Triple(
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.primary,
            "Sync completed"
        )
        is SyncState.Error -> Triple(
            Icons.Default.Error,
            MaterialTheme.colorScheme.error,
            "Sync error: ${syncState.message}"
        )
    }

    Icon(
        icon,
        contentDescription = description,
        modifier = Modifier.size(16.dp),
        tint = tint
    )
}
