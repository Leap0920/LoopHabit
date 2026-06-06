package com.example.loophabit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.loophabit.data.Habit
import com.example.loophabit.data.sync.SyncState
import com.example.loophabit.ui.theme.LoopHabitTheme
import kotlin.math.roundToInt

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFF8338EC), Color(0xFF118AB2))
) {
    Card(
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(colors))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SyncStatusIndicator(syncState: SyncState) {
    val color: Color
    val tooltip: String

    when (syncState) {
        is SyncState.Syncing -> {
            color = MaterialTheme.colorScheme.primary
            tooltip = "Syncing..."
        }
        is SyncState.Completed -> {
            color = MaterialTheme.colorScheme.tertiary
            tooltip = "Synced"
        }
        is SyncState.Error -> {
            color = MaterialTheme.colorScheme.error
            tooltip = "Sync failed: ${syncState.message}"
        }
        SyncState.Idle -> {
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            tooltip = "Not syncing"
        }
    }

    IconButton(
        onClick = { },
        modifier = Modifier
            .size(40.dp)
            .padding(end = 8.dp),
        enabled = false // Visual indicator only for now
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = tooltip,
            tint = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: HabitViewModel) {
    val currentUserId by viewModel.currentUserId.collectAsState()
    val incompleteHabits by viewModel.incompleteHabits.collectAsState()
    val completedHabits by viewModel.completedHabits.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val loopIndex by viewModel.loopIndex.collectAsState()
    val currentHabit by viewModel.currentHabit.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    // Get dark mode preference
    val app = (LocalContext.current.applicationContext as com.example.loophabit.LoopHabitApp)
    val darkModeEnabled by app.preferences.darkModeEnabledFlow.collectAsState(initial = false)

    var activeTab by remember { mutableStateOf("TODAY") } // TODAY, INSIGHTS
    var showAddDialog by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var selectedHabitForDetails by remember { mutableStateOf<Habit?>(null) }

    val totalHabitsCount = incompleteHabits.size + completedHabits.size
    val completionProgress = if (totalHabitsCount > 0) {
        completedHabits.size.toFloat() / totalHabitsCount.toFloat()
    } else {
        0f
    }

    // Wrap with theme based on user preference
    LoopHabitTheme(darkTheme = darkModeEnabled) {
        if (currentUserId == 0L) {
            AuthScreen(viewModel = viewModel)
        } else {
            Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "LoopHabit",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    actions = {
                        // Sync Status Indicator
                        SyncStatusIndicator(syncState = syncState)
                        TextButton(onClick = { showManageDialog = true }) {
                            Text("Manage", fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == "TODAY",
                        onClick = { activeTab = "TODAY" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Today") },
                        label = { Text("Today", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeTab == "INSIGHTS",
                        onClick = { activeTab = "INSIGHTS" },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Insights") },
                        label = { Text("Insights", fontWeight = FontWeight.Bold) }
                    )
                }
            },
            floatingActionButton = {
                if (activeTab == "TODAY") {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Habit")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (activeTab == "TODAY") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Section
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(
                                        text = "Today's Loop",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = if (totalHabitsCount == 0) "No habits added yet"
                                        else "${completedHabits.size} of $totalHabitsCount completed",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { completionProgress },
                                        modifier = Modifier.size(64.dp),
                                        strokeWidth = 6.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Text(
                                        text = "${(completionProgress * 100).roundToInt()}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // The Stack
                        if (incompleteHabits.isNotEmpty()) {
                            val size = incompleteHabits.size
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                contentAlignment = Alignment.Center
                              ) {
                                // Render back to front (max 3 layers visible)
                                for (i in 2 downTo 0) {
                                    if (i >= size) continue
                                    val cardIndex = (loopIndex + i) % size
                                    val habit = incompleteHabits[cardIndex]
                                    val scale = 1f - (i * 0.05f)
                                    val yOffset = (i * 16).dp

                                    if (i == 0) {
                                        SwipeableCard(
                                            habit = habit,
                                            onSwipeLeft = { viewModel.nextHabit() },
                                            onSwipeRight = { viewModel.completeHabit(habit.id) },
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    this.scaleX = scale
                                                    this.scaleY = scale
                                                    this.translationY = yOffset.toPx()
                                                }
                                                .zIndex(3f)
                                        ) { swipeOffset ->
                                            HabitCardContent(
                                                habit = habit,
                                                isTop = true,
                                                swipeOffset = swipeOffset
                                            )
                                        }
                                    } else {
                                        HabitCardContent(
                                            habit = habit,
                                            isTop = false,
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    this.scaleX = scale
                                                    this.scaleY = scale
                                                    this.translationY = yOffset.toPx()
                                                }
                                                .zIndex(3f - i)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Navigation Buttons for accessibility
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.prevHabit() },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                                }

                                Text(
                                    text = "Card ${(loopIndex % size) + 1} of $size",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                IconButton(
                                    onClick = { viewModel.nextHabit() },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "🎉 All done!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (totalHabitsCount == 0) "Create a habit to get started" else "You've completed all habits for today!",
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Completed List Section
                        if (completedHabits.isNotEmpty()) {
                            Text(
                                text = "Completed Today",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(completedHabits) { habit ->
                                    CompletedHabitRow(
                                        habit = habit,
                                        onUncomplete = { viewModel.uncompleteHabit(habit.id) }
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    InsightsDashboard(
                        viewModel = viewModel,
                        onSelectHabit = { habit -> selectedHabitForDetails = habit }
                    )
                }
            }
        }
    }

    // Add Habit Dialog
    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, colorHex, targetDays ->
                viewModel.addHabit(title, colorHex, targetDays)
                showAddDialog = false
            }
        )
    }

    // Manage Habits Dialog
    if (showManageDialog) {
        ManageHabitsDialog(
            habits = allHabits,
            onDismiss = { showManageDialog = false },
            onDelete = { habit ->
                viewModel.deleteHabit(habit)
            },
            onSelectHabit = { habit ->
                selectedHabitForDetails = habit
            },
            app = app
        )
    }

    if (selectedHabitForDetails != null) {
        HabitDetailsDialog(
            habit = selectedHabitForDetails!!,
            viewModel = viewModel,
            onDismiss = { selectedHabitForDetails = null }
        )
    }
    }
}
