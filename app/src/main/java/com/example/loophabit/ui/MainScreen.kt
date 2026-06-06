package com.example.loophabit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.loophabit.data.Habit
import com.example.loophabit.data.HabitCompletion
import com.example.loophabit.data.sync.SyncState
import com.example.loophabit.loopPreferences  // Needed for dark mode
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
// Curated pastel/vibrant HSL colors for premium aesthetic
val ColorPaletteList = listOf(
    "#EF476F", // Soft Red/Rose
    "#FFD166", // Mustard/Amber
    "#06D6A0", // Mint/Emerald
    "#118AB2", // Ocean Blue
    "#8338EC", // Purple
    "#FF9F1C"  // Vivid Orange
)

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
fun SyncStatusIndicator(syncState: com.example.loophabit.data.sync.SyncState) {
    val color: androidx.compose.ui.graphics.Color
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
    val darkModeEnabled by app.preferences.darkModeEnabledFlow.collectAsState()

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

@Composable
fun SwipeableCard(
    habit: Habit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Float) -> Unit
) {
    val swipeOffset = remember { Animatable(0f) }
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
                            imageVector = Icons.Default.Check,
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
                val icon = if (swipeOffset > 0) Icons.Default.Check else Icons.Default.Close
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
                    imageVector = Icons.Default.Close,
                    contentDescription = "Undo completion",
                    tint = parsedColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ColorPaletteList[0]) }
    var targetDaysPerWeek by remember { mutableStateOf(7) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "New Habit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What is your habit?") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Weekly Goal frequency: $targetDaysPerWeek days / week",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = targetDaysPerWeek.toFloat(),
                    onValueChange = { targetDaysPerWeek = it.roundToInt() },
                    valueRange = 1f..7f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pick Color",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ColorPaletteList.forEach { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorHex }
                                .padding(4.dp)
                        ) {
                            if (selectedColor == colorHex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.6f))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (title.isNotBlank()) onAdd(title, selectedColor, targetDaysPerWeek) },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun ManageHabitsDialog(
    habits: List<Habit>,
    onDismiss: () -> Unit,
    onDelete: (Habit) -> Unit,
    onSelectHabit: (Habit) -> Unit,
    app: com.example.loophabit.LoopHabitApp
) {
    val darkModeEnabled by app.preferences.darkModeEnabledFlow.collectAsState()
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxHeight(0.6f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Manage Habits",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dark Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dark Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = { enabled ->
                            app.preferences.setDarkModeEnabled(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                if (habits.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No habits to manage.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(habits) { habit ->
                            val parsedColor = remember(habit.colorHex) {
                                try {
                                    Color(android.graphics.Color.parseColor(habit.colorHex))
                                } catch (e: Exception) {
                                    Color(0xFF8338EC)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { 
                                            onDismiss()
                                            onSelectHabit(habit) 
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                    )
                                    Text(
                                        text = habit.title,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(habit) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Habit",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp)) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: HabitViewModel) {
    var mode by remember { mutableStateOf("LOGIN") } // LOGIN, REGISTER, FORGOT

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var securityQuestion by remember { mutableStateOf("What was the name of your first pet?") }
    var securityAnswer by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var successMsg by remember { mutableStateOf("") }

    // Forgot Password Flow State
    var forgotStep by remember { mutableStateOf(1) } // 1: find email, 2: answer & reset
    var resolvedSecurityQuestion by remember { mutableStateOf("") }

    val securityQuestions = listOf(
        "What was the name of your first pet?",
        "What is your mother's maiden name?",
        "In what city were you born?",
        "What was the make of your first car?",
        "What is your favorite book?"
    )

    var showQuestionDropdown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Soft glowing background circles matching theme colors
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopStart)
                .offset(x = (-50).dp, y = (-50).dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f), CircleShape)
        )

        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "♾️",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = when (mode) {
                        "LOGIN" -> "Welcome Back"
                        "REGISTER" -> "Create Account"
                        else -> "Reset Password"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (mode) {
                        "LOGIN" -> "Sign in to track your habits"
                        "REGISTER" -> "Join LoopHabit today"
                        else -> "Recover your account credentials"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (successMsg.isNotEmpty()) {
                    Text(
                        text = successMsg,
                        color = Color(0xFF06D6A0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )

                when (mode) {
                    "LOGIN" -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMsg = ""; successMsg = "" },
                            label = { Text("Username or Email") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User") },
                            singleLine = true,
                            shape = CircleShape,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMsg = "" },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                            singleLine = true,
                            shape = CircleShape,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        GradientButton(
                            text = "Login",
                            onClick = {
                                viewModel.login(email, password,
                                    onSuccess = { errorMsg = "" },
                                    onError = { errorMsg = it }
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Create Account",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    mode = "REGISTER"
                                    errorMsg = ""
                                    successMsg = ""
                                    password = ""
                                }
                            )
                            Text(
                                text = "Forgot Password?",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    mode = "FORGOT"
                                    forgotStep = 1
                                    errorMsg = ""
                                    successMsg = ""
                                    password = ""
                                }
                            )
                        }
                    }

                    "REGISTER" -> {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; errorMsg = "" },
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User") },
                            singleLine = true,
                            shape = CircleShape,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMsg = "" },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                            singleLine = true,
                            shape = CircleShape,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMsg = "" },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                            singleLine = true,
                            shape = CircleShape,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = securityQuestion,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Security Question") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Question") },
                                shape = CircleShape,
                                colors = textFieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showQuestionDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showQuestionDropdown,
                                onDismissRequest = { showQuestionDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                securityQuestions.forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
                                        onClick = {
                                            securityQuestion = q
                                            showQuestionDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = securityAnswer,
                            onValueChange = { securityAnswer = it; errorMsg = "" },
                            label = { Text("Your Answer") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "Answer") },
                            singleLine = true,
                            shape = CircleShape,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        GradientButton(
                            text = "Create Account",
                            colors = listOf(Color(0xFF06D6A0), Color(0xFF118AB2)),
                            onClick = {
                                viewModel.register(username, email, password, securityQuestion, securityAnswer,
                                    onSuccess = {
                                        errorMsg = ""
                                        successMsg = ""
                                    },
                                    onError = {
                                        errorMsg = it
                                    }
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Already have an account? Sign In",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                mode = "LOGIN"
                                errorMsg = ""
                                successMsg = ""
                            }
                        )
                    }

                    "FORGOT" -> {
                        if (forgotStep == 1) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it; errorMsg = "" },
                                label = { Text("Registered Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                                singleLine = true,
                                shape = CircleShape,
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            GradientButton(
                                text = "Find Account",
                                onClick = {
                                    viewModel.getSecurityQuestion(email,
                                        onSuccess = { q ->
                                            resolvedSecurityQuestion = q
                                            forgotStep = 2
                                            errorMsg = ""
                                        },
                                        onError = { errorMsg = it }
                                    )
                                }
                            )
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = resolvedSecurityQuestion,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 14.sp
                                )
                            }

                            OutlinedTextField(
                                value = securityAnswer,
                                onValueChange = { securityAnswer = it; errorMsg = "" },
                                label = { Text("Your Answer") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "Answer") },
                                singleLine = true,
                                shape = CircleShape,
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; errorMsg = "" },
                                label = { Text("New Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                                singleLine = true,
                                shape = CircleShape,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMsg = "" },
                                label = { Text("Confirm New Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                                singleLine = true,
                                shape = CircleShape,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            GradientButton(
                                text = "Reset Password",
                                onClick = {
                                    if (password != confirmPassword) {
                                        errorMsg = "Passwords do not match"
                                        return@GradientButton
                                    }
                                    viewModel.resetPassword(email, securityAnswer, password,
                                        onSuccess = {
                                            successMsg = "Password reset successfully! Log in."
                                            mode = "LOGIN"
                                            forgotStep = 1
                                            errorMsg = ""
                                            securityAnswer = ""
                                            password = ""
                                            confirmPassword = ""
                                        },
                                        onError = { errorMsg = it }
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Back to Sign In",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                mode = "LOGIN"
                                errorMsg = ""
                                successMsg = ""
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InsightsDashboard(
    viewModel: HabitViewModel,
    onSelectHabit: (Habit) -> Unit
) {
    val allHabits by viewModel.allHabits.collectAsState()
    val allCompletions by viewModel.allCompletions.collectAsState()

    // 1. Calculations:
    val today = java.time.LocalDate.now()
    val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong()) // Monday
    val currentWeekDates = (0..6).map { startOfWeek.plusDays(it.toLong()).toString() }

    val totalCompletions = allCompletions.size

    // Last 30 days overall consistency metric
    val last30Days = (0 until 30).map { today.minusDays(it.toLong()).toString() }
    val completionsInLast30 = allCompletions.count { last30Days.contains(it.date) }
    val maxPossibleCompletions = allHabits.size * 30
    val overallConsistency = if (maxPossibleCompletions > 0) {
        (completionsInLast30.toFloat() / maxPossibleCompletions.toFloat() * 100).roundToInt()
    } else {
        0
    }

    // Streaks Leaders mapping
    val habitStreaks = remember(allHabits, allCompletions) {
        allHabits.map { habit ->
            val dates = allCompletions.filter { it.habitId == habit.id }.map { it.date }
            val (current, best) = viewModel.calculateStreaks(dates)
            Triple(habit, current, best)
        }.sortedByDescending { it.second }
    }

    // 14-day history activity list
    val dateList = remember {
        (0 until 14).map { today.minusDays(it.toLong()) }.reversed()
    }
    val completionsPerDay = remember(allCompletions, dateList) {
        dateList.map { date ->
            val dateStr = date.toString()
            val count = allCompletions.count { it.date == dateStr }
            Pair(date, count)
        }
    }

    // Day of Week Frequency Heatmap
    val weekdayCompletions = remember(allCompletions) {
        val counts = IntArray(7) // Mon=0 .. Sun=6
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        for (completion in allCompletions) {
            try {
                val ld = java.time.LocalDate.parse(completion.date, formatter)
                counts[ld.dayOfWeek.value - 1]++
            } catch (e: Exception) {
                // Ignore parsing exceptions
            }
        }
        counts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Performance Dashboard",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Track your overall performance & statistics",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Consistency & Total Completion Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Consistency Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Consistency (30d)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$overallConsistency%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Total completions card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Total Completed",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$totalCompletions",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 14-day history bar chart
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Activity (Last 14 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                val maxVal = (completionsPerDay.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    completionsPerDay.forEach { (date, count) ->
                        val ratio = count.toFloat() / maxVal.toFloat()
                        val barHeight = (ratio * 70).coerceAtLeast(4f).dp
                        val isCurrentDay = date == today

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isCurrentDay) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Weekday Productivity Distribution
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Consistency by Day of Week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                val daysOfWeekNames = listOf("M", "T", "W", "T", "F", "S", "S")
                val maxCount = weekdayCompletions.maxOrNull()?.coerceAtLeast(1) ?: 1

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weekdayCompletions.forEachIndexed { idx, count ->
                        val ratio = count.toFloat() / maxCount.toFloat()
                        val barHeight = (ratio * 80).coerceAtLeast(4f).dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = daysOfWeekNames[idx],
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Habit performance leaderboard
        Text(
            text = "Habit Insights List",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (habitStreaks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No habits to show.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    habitStreaks.forEach { (habit, current, best) ->
                        val parsedColor = remember(habit.colorHex) {
                            try {
                                Color(android.graphics.Color.parseColor(habit.colorHex))
                            } catch (e: Exception) {
                                Color(0xFF8338EC)
                            }
                        }

                        // Completions this week
                        val dates = allCompletions.filter { it.habitId == habit.id }.map { it.date }
                        val completionsThisWeek = dates.count { currentWeekDates.contains(it) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectHabit(habit) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = habit.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Goal: $completionsThisWeek/${habit.targetDaysPerWeek} this week",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (current > 0) {
                                    Text(
                                        text = "🔥 $current d",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = parsedColor
                                    )
                                }
                                Text(
                                    text = "🏆 $best d",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailsDialog(
    habit: Habit,
    viewModel: HabitViewModel,
    onDismiss: () -> Unit
) {
    val completions by viewModel.getCompletionsForHabit(habit.id).collectAsState(initial = emptyList())
    val completionDates = remember(completions) { completions.map { it.date } }
    val (currentStreak, bestStreak) = remember(completionDates) {
        viewModel.calculateStreaks(completionDates)
    }

    val parsedColor = remember(habit.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(habit.colorHex))
        } catch (e: Exception) {
            Color(0xFF8338EC)
        }
    }

    var selectedDetailTab by remember { mutableStateOf("MONTH") } // MONTH, YEAR, INSIGHTS
    var showNoteEditDialogForDate by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = habit.title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Habit Insights & Log History",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Secondary Tab Row
                TabRow(
                    selectedTabIndex = when (selectedDetailTab) {
                        "MONTH" -> 0
                        "YEAR" -> 1
                        else -> 2
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedDetailTab == "MONTH",
                        onClick = { selectedDetailTab = "MONTH" },
                        text = { Text("Month", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedDetailTab == "YEAR",
                        onClick = { selectedDetailTab = "YEAR" },
                        text = { Text("Year Grid", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedDetailTab == "INSIGHTS",
                        onClick = { selectedDetailTab = "INSIGHTS" },
                        text = { Text("Logs & Stats", fontWeight = FontWeight.Bold) }
                    )
                }

                when (selectedDetailTab) {
                    "MONTH" -> {
                        MonthlyCalendarTab(
                            completions = completions,
                            parsedColor = parsedColor,
                            onDayClick = { dateStr -> showNoteEditDialogForDate = dateStr }
                        )
                    }
                    "YEAR" -> {
                        YearAtAGlanceGrid(
                            completions = completions,
                            parsedColor = parsedColor,
                            onDayClick = { dateStr -> showNoteEditDialogForDate = dateStr }
                        )
                    }
                    else -> {
                        InsightsStatsTab(
                            habit = habit,
                            completions = completions,
                            currentStreak = currentStreak,
                            bestStreak = bestStreak,
                            parsedColor = parsedColor
                        )
                    }
                }
            }
        }
    }

    if (showNoteEditDialogForDate != null) {
        val dateStr = showNoteEditDialogForDate!!
        val existingCompletion = completions.find { it.date == dateStr }
        val wasCompleted = existingCompletion != null
        val existingNotes = existingCompletion?.notes ?: ""

        NoteEditDialog(
            dateStr = dateStr,
            wasCompleted = wasCompleted,
            existingNotes = existingNotes,
            onDismiss = { showNoteEditDialogForDate = null },
            onSave = { isCompleted, notesText ->
                viewModel.toggleHabitCompletionForDate(habit.id, dateStr, wasCompleted, if (isCompleted) notesText else null)
                // If it was already completed and remains completed, but they edited notes:
                if (wasCompleted && isCompleted) {
                    viewModel.saveCompletionNote(habit.id, dateStr, notesText.ifBlank { null })
                }
                showNoteEditDialogForDate = null
            }
        )
    }
}

@Composable
fun MonthlyCalendarTab(
    completions: List<HabitCompletion>,
    parsedColor: Color,
    onDayClick: (String) -> Unit
) {
    var currentMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    val completionDates = remember(completions) { completions.map { it.date } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Calendar Controller Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
            }
            Text(
                text = "${currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Weekday Headers
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid Days
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val daysInMonth = currentMonth.lengthOfMonth()
        val totalCells = ((daysInMonth + (firstDayOfWeek - 1) + 6) / 7) * 7

        var cellCount = 0
        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until totalCells / 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val dayNum = cellCount - (firstDayOfWeek - 2)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum in 1..daysInMonth) {
                                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", currentMonth.year, currentMonth.monthValue, dayNum)
                                val completion = completions.find { it.date == dateStr }
                                val isCompleted = completion != null
                                val hasNotes = !completion?.notes.isNullOrBlank()

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCompleted) parsedColor.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isCompleted) parsedColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        )
                                        .clickable { onDayClick(dateStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 13.sp,
                                        fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCompleted) parsedColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hasNotes) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 3.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(parsedColor)
                                        )
                                    }
                                }
                            }
                        }
                        cellCount++
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Month Journal Notes list
        val monthCompletionsWithNotes = remember(completions, currentMonth) {
            completions.filter {
                it.date.startsWith(String.format(Locale.US, "%04d-%02d", currentMonth.year, currentMonth.monthValue)) &&
                        !it.notes.isNullOrBlank()
            }.sortedByDescending { it.date }
        }

        Text(
            text = "Month Diary Logs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (monthCompletionsWithNotes.isEmpty()) {
            Text(
                text = "No journal notes logged in this month yet. Tap any day on the calendar to add notes!",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                monthCompletionsWithNotes.forEach { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.date,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = parsedColor
                                )
                                Text(
                                    text = "✓ Completed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF06D6A0)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.notes ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearAtAGlanceGrid(
    completions: List<HabitCompletion>,
    parsedColor: Color,
    onDayClick: (String) -> Unit
) {
    var selectedYear by remember { mutableStateOf(java.time.LocalDate.now().year) }
    val completionDates = remember(completions) { completions.map { it.date } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Year controller header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { selectedYear-- }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Year")
            }
            Text(
                text = "Year $selectedYear",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { selectedYear++ }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Year")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label details help
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(parsedColor))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Done", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(8.dp).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Target", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid Container with horizontal scrolling in case of small devices
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(12.dp)
            ) {
                // Header Month Abbreviations: Day, J, F, M, A, M, J, J, A, S, O, N, D
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left corner offset
                    Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                        Text("", fontSize = 10.sp)
                    }

                    val monthsInitials = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                    monthsInitials.forEach { mInitial ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(21.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mInitial,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Rows for day numbers: 1 to 31
                for (dayNum in 1..31) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left index number
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(21.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "$dayNum",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }

                        // 12 months for this day
                        for (monthNum in 1..12) {
                            val isValidDay = try {
                                java.time.YearMonth.of(selectedYear, monthNum).isValidDay(dayNum)
                            } catch (e: Exception) {
                                false
                            }

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                                    .size(21.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isValidDay) {
                                    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, monthNum, dayNum)
                                    val isCompleted = completionDates.contains(dateStr)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(
                                                if (isCompleted) parsedColor
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isCompleted) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                shape = CircleShape
                                            )
                                            .clickable { onDayClick(dateStr) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun InsightsStatsTab(
    habit: Habit,
    completions: List<HabitCompletion>,
    currentStreak: Int,
    bestStreak: Int,
    parsedColor: Color
) {
    val totalCompletions = completions.size
    
    // Weekly Goal Counter calculation
    val today = java.time.LocalDate.now()
    val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong()) // Monday
    val currentWeekDates = (0..6).map { startOfWeek.plusDays(it.toLong()).toString() }
    val completionsThisWeek = completions.count { currentWeekDates.contains(it.date) }
    
    val weeklyGoalProgress = if (habit.targetDaysPerWeek > 0) {
        completionsThisWeek.toFloat() / habit.targetDaysPerWeek.toFloat()
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stats Cards Dashboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Current Streak Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔥 Streak", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$currentStreak d", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = parsedColor)
                }
            }

            // Best Streak Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆 Best", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$bestStreak d", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Total completions card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✓ Total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalCompletions", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weekly Target Goal Progress Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = parsedColor.copy(alpha = 0.12f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weekly Target Progress",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Goal is ${habit.targetDaysPerWeek} days a week. You completed $completionsThisWeek days so far.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { weeklyGoalProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(54.dp),
                        strokeWidth = 5.dp,
                        color = parsedColor,
                        trackColor = parsedColor.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "$completionsThisWeek/${habit.targetDaysPerWeek}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History logs list
        Text(
            text = "Chronological History Logs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(10.dp))

        val notesLogs = completions.filter { !it.notes.isNullOrBlank() }.sortedByDescending { it.date }
        if (notesLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notes saved for this habit yet. Write some diaries on the Calendar tab!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                notesLogs.forEach { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.date,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = parsedColor
                                )
                                Text(
                                    text = "Completed",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.notes ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteEditDialog(
    dateStr: String,
    wasCompleted: Boolean,
    existingNotes: String,
    onDismiss: () -> Unit,
    onSave: (Boolean, String) -> Unit
) {
    var isCompleted by remember { mutableStateOf(wasCompleted) }
    var notesText by remember { mutableStateOf(existingNotes) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Log for $dateStr",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = { isCompleted = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Marked as Completed",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Journal Notes / Progress Memo") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(isCompleted, notesText) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
}
