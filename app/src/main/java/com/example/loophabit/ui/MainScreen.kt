package com.example.loophabit.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast

import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.loophabit.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun formatSelectedDate(dateStr: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        SimpleDateFormat("MMMM d", Locale.getDefault()).format(date!!)
    } catch (e: Exception) {
        dateStr
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // (F4) use theme tokens instead of hardcoded purple/blue
    colors: List<Color> = listOf(
        com.example.loophabit.ui.theme.Indigo500,
        com.example.loophabit.ui.theme.Indigo700
    )
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
    // (B4) show a real spinning ring while syncing instead of a static disabled icon
    val tint: Color = when (syncState) {
        is SyncState.Syncing -> MaterialTheme.colorScheme.primary
        is SyncState.Completed -> MaterialTheme.colorScheme.tertiary
        is SyncState.Error -> MaterialTheme.colorScheme.error
        SyncState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(end = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (syncState is SyncState.Syncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = tint
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = when (syncState) {
                    is SyncState.Error -> "Sync failed"
                    is SyncState.Completed -> "Synced"
                    else -> "Not syncing"
                },
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: HabitViewModel, darkModeEnabled: Boolean) {
    val currentUserId by viewModel.currentUserId.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    // (P1) allFocusSessions removed from top-level — it changes during sync and
    // was invalidating the entire MainScreen tree. FocusScreen collects it itself;
    // the ManualTimeDialog now uses focusInfoForSelectedDate instead.
    val loopIndex by viewModel.loopIndex.collectAsState()
    val currentHabit by viewModel.currentHabit.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allCompletionDates by viewModel.allCompletionDates.collectAsState()
    val todayDate = viewModel.todayDate

    val app = LocalContext.current.applicationContext as com.example.loophabit.LoopHabitApp

    val scrollState = rememberScrollState()
    val todayListState = rememberLazyListState()
    var activeTab by remember { mutableStateOf("TODAY") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var selectedHabitForDetails by remember { mutableStateOf<Habit?>(null) }
    var showNumericalLogDialogForHabit by remember { mutableStateOf<Habit?>(null) }
    var manualTimeHabit by remember { mutableStateOf<Habit?>(null) }

    // Auto-update state
    var showAutoUpdateDialog by remember { mutableStateOf(false) }
    var latestUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showDownloadProgress by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Auto-check for updates on app launch
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val updateInfo = UpdateManager.checkForUpdates()
                if (updateInfo != null) {
                    val currentVer = UpdateManager.getCurrentVersionName(context)
                    if (UpdateManager.isNewerVersion(currentVer, updateInfo.versionName)) {
                        latestUpdateInfo = updateInfo
                        showAutoUpdateDialog = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
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

    // (P1) derive counts from allHabits instead of separately collecting
    // incomplete/completed flows at the top level (which invalidated MainScreen
    // on every habit completion). The TodayTab collects these itself.
    val totalHabitsCount = allHabits.size
    val completionProgress = 0f // computed inside TodayTab where the flows live

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
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
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
                // (F5) cleaner nav bar - use surface color, proper label typography
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == "TODAY",
                        onClick = { activeTab = "TODAY" },
                        icon = {
                            Icon(
                                Icons.Outlined.Home,
                                contentDescription = "Today",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Today", style = MaterialTheme.typography.labelMedium) }
                    )
                    NavigationBarItem(
                        selected = activeTab == "FOCUS",
                        onClick = { activeTab = "FOCUS" },
                        icon = {
                            Icon(
                                Icons.Outlined.Timer,
                                contentDescription = "Focus",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Focus", style = MaterialTheme.typography.labelMedium) }
                    )
                    NavigationBarItem(
                        selected = activeTab == "TODO",
                        onClick = { activeTab = "TODO" },
                        icon = {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = "Todo",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Todo", style = MaterialTheme.typography.labelMedium) }
                    )
                    NavigationBarItem(
                        selected = activeTab == "INSIGHTS",
                        onClick = { activeTab = "INSIGHTS" },
                        icon = {
                            Icon(
                                Icons.Outlined.DateRange,
                                contentDescription = "Insights",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Insights", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            },
            floatingActionButton = {
                if (activeTab == "TODAY") {
                    AnimatedVisibility(
                        visible = todayListState.firstVisibleItemIndex == 0 && todayListState.firstVisibleItemScrollOffset == 0,
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
                        // (C1) faster crossfade instead of 300ms slide — avoids
                        // holding two heavy tab trees alive simultaneously
                        (fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(180))).using(
                            androidx.compose.animation.SizeTransform(clip = false)
                        )
                    },
                    label = "tabTransition",
                    modifier = Modifier.fillMaxSize()
                ) { targetTab ->
                    when (targetTab) {
                        "TODAY" -> {
                                // (P4) TodayTab collects its own state, isolating it
                                // from the AnimatedContent tab transition. Previously
                                // all TODAY state was collected at the top-level
                                // MainScreen, so switching tabs re-composed everything.
                                TodayTab(
                                    viewModel = viewModel,
                                    totalHabitsCount = totalHabitsCount,
                                    todayListState = todayListState,
                                    onAddHabit = { showAddDialog = true },
                                    onShowNumericalLog = { showNumericalLogDialogForHabit = it },
                                    onShowManualTime = { manualTimeHabit = it },
                                    onShowHabitDetails = { selectedHabitForDetails = it }
                                )
                            }
                        "FOCUS" -> {
                            FocusScreen(viewModel = viewModel)
                        }
                        "TODO" -> {
                            // (A2) TodoTab collects todos itself, isolating recomposition
                            TodoTab(
                                viewModel = viewModel,
                                onAddTodo = { title, notes -> viewModel.addTodo(title, notes) },
                                onUpdateTodo = { todo, title, notes -> viewModel.updateTodo(todo, title, notes) },
                                onToggleTodo = { todo -> viewModel.toggleTodo(todo) },
                                onDeleteTodo = { todo -> viewModel.deleteTodo(todo) }
                            )
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

    val habitForManualTime = manualTimeHabit
    if (habitForManualTime != null) {
        // (P1) use focusInfoForSelectedDate instead of allFocusSessions
        val focusInfoForDialog by viewModel.focusInfoForSelectedDate.collectAsState()
        val existingMinutes = remember(habitForManualTime.id, focusInfoForDialog) {
            focusInfoForDialog[habitForManualTime.id]?.manualMinutes ?: 0
        }
        ManualTimeDialog(
            habit = habitForManualTime,
            initialMinutes = existingMinutes,
            onDismiss = { manualTimeHabit = null },
            onSave = { minutes, startHour, startMinute ->
                viewModel.setManualFocusMinutes(habitForManualTime.id, minutes, startHour, startMinute)
                manualTimeHabit = null
            }
        )
    }



    val focusHabitId by viewModel.focusHabitId.collectAsState()
    val focusHabit = allHabits.find { it.id == focusHabitId }

    // (P1) collect these only when the focus overlay is active, not at the top level
    val incompleteHabits by viewModel.incompleteHabits.collectAsState()
    val completedHabits by viewModel.completedHabits.collectAsState()

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

    // Auto-update dialog (shown on app launch if update available)
    if (showAutoUpdateDialog && latestUpdateInfo != null) {
        val update = latestUpdateInfo!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAutoUpdateDialog = false },
            title = {
                Text(
                    text = "New Update Available!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Version: ${update.versionName}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (update.releaseNotes.isNotBlank()) {
                        Text(
                            text = "Release Notes:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = update.releaseNotes,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 6,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "A new update is available. Do you want to download and install it now?",
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showAutoUpdateDialog = false
                        showDownloadProgress = true
                        downloadProgress = 0f
                        coroutineScope.launch {
                            val apkFile = withContext(Dispatchers.IO) {
                                UpdateManager.downloadApk(context, update.downloadUrl) { progress ->
                                    downloadProgress = progress
                                }
                            }
                            showDownloadProgress = false
                            if (apkFile != null) {
                                UpdateManager.installApk(context, apkFile)
                            } else {
                                Toast.makeText(context, "Failed to download update APK.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Update Now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAutoUpdateDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    // Download progress dialog
    if (showDownloadProgress) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            androidx.compose.material3.Card(
                shape = RoundedCornerShape(20.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Downloading Update",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


// (P4) TodayTab collects its own state, isolating recomposition from tab transitions.
// Previously all this state was collected at the top-level MainScreen, so any flow
// change (including allFocusSessions during sync) invalidated the entire screen.
@Composable
private fun TodayTab(
    viewModel: HabitViewModel,
    totalHabitsCount: Int,
    todayListState: androidx.compose.foundation.lazy.LazyListState,
    onAddHabit: () -> Unit,
    onShowNumericalLog: (com.example.loophabit.data.Habit) -> Unit,
    onShowManualTime: (com.example.loophabit.data.Habit) -> Unit,
    onShowHabitDetails: (com.example.loophabit.data.Habit) -> Unit
) {
    val currentUserId by viewModel.currentUserId.collectAsState()
    val incompleteHabitsForSelected by viewModel.incompleteHabitsForSelected.collectAsState()
    val completedHabitsForSelected by viewModel.completedHabitsForSelected.collectAsState()
    val optimistic by viewModel.optimisticCompletions.collectAsState()
    val focusInfoMap by viewModel.focusInfoForSelectedDate.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allCompletionDates by viewModel.allCompletionDates.collectAsState()
    val loopIndex by viewModel.loopIndex.collectAsState()
    val todayDate = viewModel.todayDate

    // (P3) memoize the effective lists so filter operations do not re-run
    // on every recomposition (e.g. when focusInfoMap changes but habits do not)
    val effectiveIncomplete = remember(incompleteHabitsForSelected, optimistic) {
        incompleteHabitsForSelected.filter { it.id !in optimistic }
    }
    val effectiveCompleted = remember(completedHabitsForSelected, incompleteHabitsForSelected, optimistic) {
        completedHabitsForSelected + incompleteHabitsForSelected.filter { it.id in optimistic }
    }

    LazyColumn(
                                state = todayListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // (B2) show shimmer skeletons ONLY during the transient
                                // initial load (before a user is resolved). Once a user ID
                                // is set, even with zero habits we show the real empty state.
                                val isInitialLoad = currentUserId == 0L

                                if (isInitialLoad) {
                                    item { Spacer(modifier = Modifier.height(10.dp)) }
                                    item { HabitCardSkeleton() }
                                    item { Spacer(modifier = Modifier.height(30.dp)) }
                                    repeat(3) {
                                        item { CompletedRowSkeleton() }
                                        item { Spacer(modifier = Modifier.height(8.dp)) }
                                    }
                                    item { Spacer(modifier = Modifier.height(80.dp)) }
                                    return@LazyColumn
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }

                                // Date Picker Row (under header, before progress)
                                item {
                                    DatePickerRow(
                                        selectedDate = selectedDate,
                                        onDateSelected = { viewModel.setSelectedDate(it) },
                                        completionDates = allCompletionDates
                                    )
                                }

                                // Progress Section
                                item {
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                                text = if (selectedDate == todayDate) "Today's Loop" else "${formatSelectedDate(selectedDate)}'s Loop",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (incompleteHabitsForSelected.size + completedHabitsForSelected.size == 0) "No habits added yet"
                                                else "${completedHabitsForSelected.size} of ${incompleteHabitsForSelected.size + completedHabitsForSelected.size} completed",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            CircularProgressIndicator(
                                                progress = { if (incompleteHabitsForSelected.size + completedHabitsForSelected.size > 0) completedHabitsForSelected.size.toFloat() / (incompleteHabitsForSelected.size + completedHabitsForSelected.size).toFloat() else 0f },
                                                modifier = Modifier.size(64.dp),
                                                strokeWidth = 6.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            )
                                            Text(
                                                text = "${if (incompleteHabitsForSelected.size + completedHabitsForSelected.size > 0) (completedHabitsForSelected.size.toFloat() / (incompleteHabitsForSelected.size + completedHabitsForSelected.size).toFloat() * 100).roundToInt() else 0}%",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                }

                                item { Spacer(modifier = Modifier.height(30.dp)) }

                                // The Stack
                                item {
                                if (effectiveIncomplete.isNotEmpty()) {
                                    val size = effectiveIncomplete.size
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
                                            val habit = effectiveIncomplete[cardIndex]
                                            val scale = 1f - (i * 0.05f)
                                            val yOffset = (i * 16).dp

                                            if (i == 0) {
                                                SwipeableCard(
                                                    habit = habit,
                                                    onSwipeLeft = { viewModel.nextHabit() },
                                                    onSwipeRight = {
                                                        if (habit.isNumerical) {
                                                            onShowNumericalLog(habit)
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
                                                ) {
                                                    HabitCardContent(
                                                        habit = habit,
                                                        isTop = true
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
                                                val isZeroHabits = totalHabitsCount == 0
                                                Icon(
                                                    imageVector = if (isZeroHabits) Icons.Outlined.Add else Icons.Outlined.Celebration,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = if (isZeroHabits) "Start your first loop" else "All done!",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 24.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = if (isZeroHabits) "Tap + to add a habit" else "You've completed all habits for today!",
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                    }
                                }
                                }

                                item { Spacer(modifier = Modifier.height(20.dp)) }

                                // Completed List Section
                                if (effectiveCompleted.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = if (selectedDate == todayDate) "Completed Today" else "Completed on ${formatSelectedDate(selectedDate)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Start
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    items(effectiveCompleted, key = { it.id }) { habit ->
                                        // (A4) use the derived map instead of per-row function calls
                                        val info = focusInfoMap[habit.id] ?: HabitViewModel.FocusRowInfo(false, 0)
                                        CompletedHabitRow(
                                            habit = habit,
                                            manualMinutes = info.manualMinutes,
                                            showManualTimeAction = !info.hasFocusTime,
                                            onEditManualTime = { onShowManualTime(habit) },
                                            onUncomplete = { viewModel.uncompleteHabit(habit.id) }
                                        )
                                    }
                                    item { Spacer(modifier = Modifier.height(20.dp)) }
                                }

                                // Bottom Spacer to prevent overlap with FAB
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
}



// (A2) Per-tab wrapper that collects only the todos flow, so todo mutations
// don't invalidate the entire MainScreen tree.
@Composable
private fun TodoTab(
    viewModel: HabitViewModel,
    onAddTodo: (String, String?) -> Unit,
    onUpdateTodo: (com.example.loophabit.data.TodoItem, String, String?) -> Unit,
    onToggleTodo: (com.example.loophabit.data.TodoItem) -> Unit,
    onDeleteTodo: (com.example.loophabit.data.TodoItem) -> Unit
) {
    val todos by viewModel.todos.collectAsState()
    TodoScreen(
        todos = todos,
        onAddTodo = onAddTodo,
        onUpdateTodo = onUpdateTodo,
        onToggleTodo = onToggleTodo,
        onDeleteTodo = onDeleteTodo
    )
}

@Composable
fun WheelTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (hour24: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPm = initialHour >= 12
    val initialHour12 = when {
        initialHour == 0 -> 12
        initialHour > 12 -> initialHour - 12
        else -> initialHour
    }

    var selectedHour12 by remember { mutableIntStateOf(initialHour12) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute / 5 * 5) }
    var selectedAmPm by remember { mutableStateOf(if (isPm) "PM" else "AM") }

    val hours = (1..12).toList()
    val minutes = (0..55 step 5).toList()
    val amPm = listOf("AM", "PM")

    val ITEM_HEIGHT = 48.dp
    val VISIBLE_ITEMS = 5

    @Composable
    fun <T> WheelColumn(
        items: List<T>,
        selectedItem: T,
        label: (T) -> String,
        onSelectionChange: (T) -> Unit
    ) {
        val initialIdx = items.indexOf(selectedItem).coerceAtLeast(0)
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIdx)

        // Detect the centered item based on scroll position
        val density = LocalDensity.current
        val itemHeightPx = with(density) { ITEM_HEIGHT.toPx() }
        val halfVisible = VISIBLE_ITEMS / 2

        LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress) {
                val layoutInfo = listState.layoutInfo
                val center = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2
                val closest = layoutInfo.visibleItemsInfo.minByOrNull { 
                    val itemCenter = it.offset + it.size / 2
                    kotlin.math.abs(itemCenter - center)
                }
                if (closest != null && items[closest.index] != selectedItem) {
                    onSelectionChange(items[closest.index])
                }
            }
        }

        Box(
            modifier = modifier
                .width(64.dp)
                .height(ITEM_HEIGHT * VISIBLE_ITEMS)
        ) {
            // Selection highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp)
                    )
            )

            // Fade gradient overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT * 2)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT * 2)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = ITEM_HEIGHT * 2),
            ) {
                items(items.size) { index ->
                    val isSelected = items[index] == selectedItem
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ITEM_HEIGHT),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label(items[index]),
                            fontSize = if (isSelected) 20.sp else 16.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelColumn(
                items = hours,
                selectedItem = selectedHour12,
                label = { "$it" },
                onSelectionChange = {
                    selectedHour12 = it
                    val hour24 = when {
                        selectedAmPm == "AM" && it == 12 -> 0
                        selectedAmPm == "PM" && it != 12 -> it + 12
                        else -> it
                    }
                    onTimeSelected(hour24, selectedMinute)
                }
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = (ITEM_HEIGHT * 2))
            )

            WheelColumn(
                items = minutes,
                selectedItem = selectedMinute,
                label = { String.format("%02d", it) },
                onSelectionChange = {
                    selectedMinute = it
                    val hour24 = when {
                        selectedAmPm == "AM" && selectedHour12 == 12 -> 0
                        selectedAmPm == "PM" && selectedHour12 != 12 -> selectedHour12 + 12
                        else -> selectedHour12
                    }
                    onTimeSelected(hour24, it)
                }
            )

            WheelColumn(
                items = amPm,
                selectedItem = selectedAmPm,
                label = { it },
                onSelectionChange = {
                    selectedAmPm = it
                    val hour24 = when {
                        it == "AM" && selectedHour12 == 12 -> 0
                        it == "PM" && selectedHour12 != 12 -> selectedHour12 + 12
                        else -> selectedHour12
                    }
                    onTimeSelected(hour24, selectedMinute)
                }
            )
        }
    }
}

@Composable
fun ManualTimeDialog(
    habit: Habit,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var hoursText by remember(initialMinutes) {
        val hours = initialMinutes / 60
        mutableStateOf(if (hours > 0) hours.toString() else "")
    }
    var minutesText by remember(initialMinutes) {
        val minutes = initialMinutes % 60
        mutableStateOf(if (minutes > 0) minutes.toString() else "")
    }
    var isError by remember { mutableStateOf(false) }

    val now = java.time.LocalTime.now()
    var selectedStartHour by remember { mutableIntStateOf(now.hour) }
    var selectedStartMinute by remember { mutableIntStateOf(now.minute / 5 * 5) }

    fun totalMinutesOrNull(): Int? {
        val hours = hoursText.toIntOrNull() ?: 0
        val minutes = minutesText.toIntOrNull() ?: 0
        if (hours !in 0..24 || minutes !in 0..59) return null
        val total = (hours * 60) + minutes
        return if (total in 0..1440) total else null
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Manual Focus Time",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = habit.title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = hoursText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 2) {
                                hoursText = input
                                isError = totalMinutesOrNull() == null
                            }
                        },
                        label = { Text("Hours") },
                        placeholder = { Text("0") },
                        isError = isError,
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = minutesText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 2) {
                                minutesText = input
                                isError = totalMinutesOrNull() == null
                            }
                        },
                        label = { Text("Minutes") },
                        placeholder = { Text("0") },
                        isError = isError,
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Start Time",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    WheelTimePicker(
                        initialHour = selectedStartHour,
                        initialMinute = selectedStartMinute,
                        onTimeSelected = { h, m ->
                            selectedStartHour = h
                            selectedStartMinute = m
                        }
                    )
                }

                Text(
                    text = "When did you start? This shows on the Productive Focus Hours chart.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Use 0h 0m to delete the manual entry.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    val totalMinutes = totalMinutesOrNull()
                    if (totalMinutes != null) {
                        onSave(totalMinutes, selectedStartHour, selectedStartMinute)
                    } else {
                        isError = true
                    }
                },
                enabled = !isError
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
                            isTop = true
                        )
                        
                        val parsedColor = remember(habit.colorHex) {
                            try {
                                Color(android.graphics.Color.parseColor(habit.colorHex))
                            } catch (e: Exception) {
                                com.example.loophabit.ui.theme.HabitFallbackColor
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
                            ) {
                                HabitCardContent(
                                    habit = cardHabit,
                                    isTop = true
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
