package com.example.loophabit.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.loophabit.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.OfflineBolt
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
            imageVector = Icons.Outlined.CheckCircle,
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

    val scrollState = rememberScrollState()
    var activeTab by remember { mutableStateOf("TODAY") } // TODAY, INSIGHTS
    var showAddDialog by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var selectedHabitForDetails by remember { mutableStateOf<Habit?>(null) }
    var showNumericalLogDialogForHabit by remember { mutableStateOf<Habit?>(null) }

    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importData(
                context = context,
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Failed to restore backup: $err", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val totalHabitsCount = incompleteHabits.size + completedHabits.size
    val completionProgress = if (totalHabitsCount > 0) {
        completedHabits.size.toFloat() / totalHabitsCount.toFloat()
    } else {
        0f
    }

    // Wrap with theme based on user preference
    LoopHabitTheme(darkTheme = darkModeEnabled) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = if (darkModeEnabled) R.drawable.darkmode_logo else R.drawable.logo2),
                                contentDescription = "LoopHabit Logo",
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "LoopHabit",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        // Sync Status Indicator
                        SyncStatusIndicator(syncState = syncState)
                        IconButton(onClick = { showManageDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
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
                        icon = { Icon(Icons.Outlined.Home, contentDescription = "Today") },
                        label = { Text("Today", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeTab == "FOCUS",
                        onClick = { activeTab = "FOCUS" },
                        icon = { Icon(Icons.Outlined.Timer, contentDescription = "Focus") },
                        label = { Text("Focus", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeTab == "INSIGHTS",
                        onClick = { activeTab = "INSIGHTS" },
                        icon = { Icon(Icons.Outlined.DateRange, contentDescription = "Insights") },
                        label = { Text("Insights", fontWeight = FontWeight.Bold) }
                    )
                }
            },
            floatingActionButton = {
                if (activeTab == "TODAY") {
                    AnimatedVisibility(
                        visible = scrollState.value == 0,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        FloatingActionButton(
                            onClick = { showAddDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add Habit")
                        }
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
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        val order = listOf("TODAY", "FOCUS", "INSIGHTS")
                        val fromIndex = order.indexOf(initialState)
                        val toIndex = order.indexOf(targetState)
                        if (toIndex > fromIndex) {
                            (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300))
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300))
                            )
                        }
                    },
                    label = "tabTransition",
                    modifier = Modifier.fillMaxSize()
                ) { targetTab ->
                    when (targetTab) {
                        "TODAY" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
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
                                                    onSwipeRight = {
                                                        if (habit.isNumerical) {
                                                            showNumericalLogDialogForHabit = habit
                                                        } else {
                                                            viewModel.completeHabit(habit.id)
                                                        }
                                                    },
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
                                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous")
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
                                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next")
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
                                            Icon(
                                                imageVector = Icons.Outlined.Celebration,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "All done!",
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
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize()
                                    ) {
                                        completedHabits.forEach { habit ->
                                            CompletedHabitRow(
                                                habit = habit,
                                                onUncomplete = { viewModel.uncompleteHabit(habit.id) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                }

                                // Bottom Spacer to prevent overlap with FAB
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                        "FOCUS" -> {
                            FocusScreen(viewModel = viewModel)
                        }
                        "INSIGHTS" -> {
                            InsightsDashboard(
                                viewModel = viewModel,
                                onSelectHabit = { habit -> selectedHabitForDetails = habit }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Habit Dialog
    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, colorHex, targetDays, isNum, goal, unit, pattern ->
                viewModel.addHabit(title, colorHex, targetDays, isNum, goal, unit, pattern)
                showAddDialog = false
            }
        )
    }

    // Settings Dialog
    if (showManageDialog) {
        SettingsDialog(
            habits = allHabits,
            onDismiss = { showManageDialog = false },
            onDelete = { habit ->
                viewModel.deleteHabit(habit)
            },
            onSelectHabit = { habit ->
                selectedHabitForDetails = habit
            },
            viewModel = viewModel,
            app = app,
            onImportClick = { importLauncher.launch("application/json") }
        )
    }

    if (showNumericalLogDialogForHabit != null) {
        val habit = showNumericalLogDialogForHabit!!
        NumericalLogDialog(
            habit = habit,
            onDismiss = { showNumericalLogDialogForHabit = null },
            onLog = { value ->
                viewModel.completeHabitNumerical(habit.id, value)
                showNumericalLogDialogForHabit = null
            }
        )
    }

    if (selectedHabitForDetails != null) {
        HabitDetailsDialog(
            habit = selectedHabitForDetails!!,
            viewModel = viewModel,
            onDismiss = { selectedHabitForDetails = null }
        )
    }



    val focusHabitId by viewModel.focusHabitId.collectAsState()
    val focusHabit = allHabits.find { it.id == focusHabitId }

    if (focusHabit != null) {
        val isCompleted = completedHabits.any { it.id == focusHabit.id }
        
        // Re-order incompleteHabits so the focused habit is at the top of the stack
        val index = incompleteHabits.indexOfFirst { it.id == focusHabit.id }
        val orderedIncompleteHabits = if (index != -1) {
            incompleteHabits.subList(index, incompleteHabits.size) + incompleteHabits.subList(0, index)
        } else {
            incompleteHabits
        }

        FocusModeOverlay(
            habit = focusHabit,
            orderedIncompleteHabits = orderedIncompleteHabits,
            isCompleted = isCompleted,
            onDismiss = { viewModel.setFocusHabitId(null) },
            onComplete = {
                val nextHabit = if (incompleteHabits.size > 1) {
                    val currentIndex = incompleteHabits.indexOfFirst { it.id == focusHabit.id }
                    if (currentIndex != -1) {
                        incompleteHabits[(currentIndex + 1) % incompleteHabits.size]
                    } else {
                        incompleteHabits.firstOrNull()
                    }
                } else {
                    null
                }
                viewModel.completeHabit(focusHabit.id)
                if (nextHabit != null) {
                    viewModel.setFocusHabitId(nextHabit.id)
                }
            },
            onUndo = {
                viewModel.uncompleteHabit(focusHabit.id)
            },
            onSwipeLeft = {
                val nextHabit = if (incompleteHabits.size > 1) {
                    val currentIndex = incompleteHabits.indexOfFirst { it.id == focusHabit.id }
                    if (currentIndex != -1) {
                        incompleteHabits[(currentIndex + 1) % incompleteHabits.size]
                    } else {
                        incompleteHabits.firstOrNull()
                    }
                } else {
                    null
                }
                if (nextHabit != null) {
                    viewModel.setFocusHabitId(nextHabit.id)
                }
            }
        )
    }
    }
}

@Composable
fun FocusModeOverlay(
    habit: Habit,
    orderedIncompleteHabits: List<Habit>,
    isCompleted: Boolean,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onUndo: () -> Unit,
    onSwipeLeft: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    onClick = onDismiss,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                )
        )

        // Close button at top right
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Outlined.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Outlined.Celebration else Icons.Outlined.OfflineBolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCompleted) "Completed!" else "Focus Mode",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(340.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume clicks to avoid dismissing when tapping on card area
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        HabitCardContent(
                            habit = habit,
                            isTop = true,
                            swipeOffset = 0f
                        )
                        
                        val parsedColor = remember(habit.colorHex) {
                            try {
                                Color(android.graphics.Color.parseColor(habit.colorHex))
                            } catch (e: Exception) {
                                Color(0xFF8338EC)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.85f)
                                .background(parsedColor.copy(alpha = 0.9f), shape = RoundedCornerShape(28.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Habit Completed!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your progress has been saved",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    val size = orderedIncompleteHabits.size
                    for (i in 2 downTo 0) {
                        if (i >= size) continue
                        val cardHabit = orderedIncompleteHabits[i]
                        val scale = 1f - (i * 0.05f)
                        val yOffset = (i * 16).dp

                        if (i == 0) {
                            SwipeableCard(
                                habit = cardHabit,
                                onSwipeLeft = onSwipeLeft,
                                onSwipeRight = onComplete,
                                modifier = Modifier
                                    .graphicsLayer {
                                        this.scaleX = scale
                                        this.scaleY = scale
                                        this.translationY = yOffset.toPx()
                                    }
                                    .zIndex(3f)
                            ) { swipeOffset ->
                                HabitCardContent(
                                    habit = cardHabit,
                                    isTop = true,
                                    swipeOffset = swipeOffset
                                )
                            }
                        } else {
                            HabitCardContent(
                                habit = cardHabit,
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
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isCompleted) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onUndo,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text("Undo completion", fontWeight = FontWeight.Bold)
                    }
                    
                    GradientButton(
                        text = "Leave Focus Mode",
                        onClick = onDismiss,
                        modifier = Modifier.width(180.dp)
                    )
                }
            } else {
                Text(
                    text = "Swipe right to complete • Swipe left to cycle\nTap background/close to exit",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
