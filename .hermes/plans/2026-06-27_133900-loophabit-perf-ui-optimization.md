# LoopHabit Performance & UI Optimization Plan

**Goal:** Eliminate lag/jank and perceived "no loading" issues, and materially improve the UI polish across the app.

**Architecture:** Jetpack Compose + Room + Supabase (Ktor) + WorkManager. Single-activity, one god-ViewModel (`HabitViewModel`) exposing ~15 `StateFlow`s that the `MainScreen` collects all at once every recomposition. The lag comes from (1) excessive recomposition, (2) synchronous network sync fired on every habit mutation, (3) heavy non-memoized computations in composition, and (4) no loading/skeleton states so the user sees blanks then pops.

**Tech Stack:** Kotlin, Compose Material3, Room 2.x, Supabase-KT (Ktor OkHttp), WorkManager, Glance widgets.

---

## Root-Cause Analysis (what's actually making it laggy)

### 1. MainScreen collects 13+ StateFlows at the top level — `MainScreen.kt:165-178`
Every `collectAsState()` at the `MainScreen` top means a change to ANY flow (e.g. `allFocusSessions` updating during a sync pull) invalidates the entire `MainScreen` composable body, including the `AnimatedContent` switch and all tab content. This is the single biggest recomposition cost.

### 2. `triggerSync()` fires on every single habit/todo mutation — `HabitViewModel.kt:381,391,401,411,477,484,491,504,515,523,553,598,655,688`
`addHabit`, `completeHabit`, `uncompleteHabit`, `nextHabit`, `prevHabit`, `deleteHabit`, `toggleTodo`, `addTodo`, etc. each call `triggerSync()` → `syncManager.fullSync()` which does a full push-then-pull of ALL users, ALL habits, ALL completions over the network, sequentially, on `viewModelScope` (Main dispatcher by default for the launch, though the suspend calls themselves are on IO via Ktor). Every swipe-to-complete triggers a network round-trip and a DB re-query storm. This is the "laggy as hell" on actions.

### 3. `manualFocusMinutesForHabit` / `hasFocusTimeForHabitOnDate` called per-row in composition — `MainScreen.kt:589-593`, `HabitViewModel.kt:429-445`
These read `allFocusSessions.value` (a List) and do `firstOrNull`/`any` with `SimpleDateFormat` parsing **on every recomposition**, per completed-habit row, not memoized. With N completed habits this is N × List-scan × date-format-construction per frame.

### 4. `InsightsDashboard` does heavy un-memoized work — `InsightsComponents.kt:104-150`
`completionsInLast30`, `overallConsistency`, and several `remember` blocks recompute on every recomposition triggered by any collected flow. Some are `remember(allCompletions)` (good) but `totalCompletions` (line 104) and `completionsInLast30` (108) are plain vals recomputed every time the function runs, which is every recomposition of the parent.

### 5. `updateWidget()` called on every mutation — `HabitViewModel.kt:351,382,392,402,412,424,460,469,477,484,492,505,516,524`
`HabitWidget().updateAll(applicationContext)` is relatively expensive (Glance re-renders all widget instances). Called synchronously in the same coroutine as the DB write, 14+ call sites. Debouncing this will cut background work substantially.

### 6. No loading / skeleton / progress states anywhere
- `MainScreen` renders `emptyList()` defaults until Room emits — user sees blank screen then content pops in. No shimmer/skeleton.
- `completeHabit` etc. have no optimistic UI — the card stays until the Room Flow re-emits, so swipes feel "stuck".
- Insights charts animate in but data populates late with no loading indicator.
- Sync runs with a tiny `SyncStatusIndicator` icon that's `enabled=false` and does nothing — no real feedback.

### 7. `AnimatedContent` tab transition with slide+fade is 300ms and re-runs content — `MainScreen.kt:363-378`
The transition holds both old and new tab composables alive simultaneously during the 300ms slide. For the TODAY tab (which holds the LazyColumn + card stack) this doubles composition cost during every tab switch. Combined with #1, tab switches feel janky.

### 8. `SwipeableCard` uses `mutableFloatStateOf` for drag but wraps `onDragEnd` in `coroutineScope.launch` + `Animatable` — `HabitCards.kt:67-128`
The drag itself is fine (direct state mutation), but on drag-end it launches a coroutine, snaps, animates, and resets — and `onSwipeRight`/`onSwipeLeft` immediately trigger `triggerSync()` (#2) + `updateWidget()` (#5). So the swipe animation competes with network+widget work on the same dispatcher.

### 9. Theme/dynamic color recomputation — `Theme.kt:44-52`, `MainScreen.kt:181-182,244`
`MainScreen` reads `darkModeEnabled` from `app.preferences` via `collectAsState` and then wraps content in `LoopHabitTheme(darkTheme = darkModeEnabled)` — **inside** the composable body, not at the activity level. So toggling dark mode or any preference change re-themes the entire tree. Also `dynamicColor=true` default means Material You extracts colors from wallpaper on every recomposition path.

### 10. `currentUserId` init loop — `HabitViewModel.kt:61-82`
The `init { collect currentUserIdFlow }` runs forever, and on every emission (including the initial `0L`) it may query/create the local user. It's a `collect` not a `stateIn`, so it never stops. Minor, but it's an always-running coroutine touching preferences.

---

## Proposed Approach (ordered by impact)

Three phases, smallest-blast-radius first:

**Phase A — Stop the recomposition & sync storms (biggest wins, no UI redesign)**
- Hoist StateFlow collection to the narrowest consumer; split `MainScreen` into per-tab composables that collect only what they need.
- Debounce `triggerSync()` and `updateWidget()` behind a single coroutine that coalesces calls within a short window.
- Memoize the per-row focus-time lookups into a `Map<Long, FocusRowInfo>` derived `StateFlow`.
- Move theme application to `MainActivity` `setContent`, not inside `MainScreen`.

**Phase B — Loading states & optimistic UI (fixes "no loading")**
- Add skeleton/shimmer placeholders for the TODAY list, TODO list, and Insights.
- Make `completeHabit`/`uncompleteHabit`/`toggleTodo` optimistic: update local state immediately, let Room Flow confirm.
- Make the sync indicator actually show a progress ring + last-synced time.

**Phase C — UI polish (visual improvements)**
- Refine the card stack visuals, spacing, typography scale, empty-state illustrations.
- Add micro-interactions (haptic on swipe-complete, subtle elevation animations).
- Improve Insights charts (rounded bars, gradient fills, better empty state).
- Tighten the tab transition (shorter, or Crossfade instead of slide for heavy tabs).

---

## Step-by-Step Plan

### Phase A: Recomposition & Sync Storm Fixes

---

### Task A1: Move theme application from MainScreen to MainActivity

**Objective:** Stop re-wrapping the whole tree in `LoopHabitTheme` on every `MainScreen` recomposition.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/MainActivity.kt`
- Modify: `app/src/main/java/com/example/loophabit/ui/MainScreen.kt:181-182,244` (remove the `darkModeEnabled` collect + the `LoopHabitTheme { }` wrapper)

**Step 1: Edit MainActivity to apply theme in setContent**

```kotlin
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleIntent(intent)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }
    setContent {
        val app = (application as LoopHabitApp)
        val darkModeEnabled by app.preferences.darkModeEnabledFlow.collectAsState(initial = false)
        LoopHabitTheme(darkTheme = darkModeEnabled) {
            MainScreen(viewModel = viewModel)
        }
    }
}
```

**Step 2: In MainScreen, remove the `darkModeEnabled` collect and the `LoopHabitTheme { ... }` wrapper**

Remove lines ~181-182:
```kotlin
val app = (LocalContext.current.applicationContext as com.example.loophabit.LoopHabitApp)
val darkModeEnabled by app.preferences.darkModeEnabledFlow.collectAsState(initial = false)
```
Keep `val app` only if still needed for `SettingsDialog(app = app, ...)` — it is, so keep the `app` line, drop the `darkModeEnabled` line.

Change line ~244 from:
```kotlin
LoopHabitTheme(darkTheme = darkModeEnabled) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold( ... )
```
to just:
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    Scaffold( ... )
```
and remove the matching closing `}` at line ~858.

Also the TopAppBar logo selector at line ~255 uses `darkModeEnabled` — replace with `isSystemInDarkTheme()` or pass it down. Simplest: read it once via a small composable param or `isSystemInDarkTheme()`. Since dark mode is user-controlled not system, add a `darkModeEnabled: Boolean` parameter to `MainScreen` and pass from `MainActivity`.

**Step 3: Build**

Run: `cmd.exe /c "cd /d C:\Users\sigmu\Desktop\LoopHabit && gradlew.bat assembleDebug --console=plain"`
Expected: BUILD SUCCESSFUL.

---

### Task A2: Split MainScreen into per-tab composables that collect their own state

**Objective:** Stop collecting 13 flows at the `MainScreen` level; each tab collects only what it needs so a change in `allFocusSessions` doesn't invalidate the TODAY tab.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/MainScreen.kt`
- (No new files — keep it in MainScreen.kt to minimize churn, just extract composables)

**Step 1: Extract `TodayTab` composable**

Move the `"TODAY" ->` branch body into:
```kotlin
@Composable
private fun TodayTab(
    viewModel: HabitViewModel,
    onAddHabit: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHabitDetails: (Habit) -> Unit,
    onOpenNumericalLog: (Habit) -> Unit,
    onOpenManualTime: (Habit) -> Unit,
) {
    val incompleteHabitsForSelected by viewModel.incompleteHabitsForSelected.collectAsState()
    val completedHabitsForSelected by viewModel.completedHabitsForSelected.collectAsState()
    val loopIndex by viewModel.loopIndex.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allCompletionDates by viewModel.allCompletionDates.collectAsState()
    // ... ONLY these flows
    LazyColumn(...) { ... }
}
```

**Step 2: Extract `FocusTab`, `TodoTab`, `InsightsTab` similarly**

Each collects only its own flows. `MainScreen` keeps only: `activeTab`, dialog visibility states, `focusHabitId`, auto-update state, and the `AnimatedContent` switch calling the tab composables.

**Step 3: MainScreen body becomes thin**

```kotlin
val activeTab by remember { mutableStateOf("TODAY") }
// ... dialog states only
AnimatedContent(targetState = activeTab, ...) { targetTab ->
    when (targetTab) {
        "TODAY" -> TodayTab(viewModel, onAddHabit = {...}, ...)
        "FOCUS" -> FocusScreen(viewModel)
        "TODO" -> TodoTab(viewModel, ...)
        "INSIGHTS" -> InsightsDashboard(viewModel, onSelectHabit = {...})
    }
}
```

**Step 4: Build & manually verify tab switch is smoother**

Run: `gradlew.bat assembleDebug`, install, switch tabs rapidly.
Expected: noticeably less jank on tab switch.

---

### Task A3: Debounce triggerSync() and updateWidget() into a single coalescing coroutine

**Objective:** Stop firing N network round-trips + N Glance re-renders when the user swipes through 5 habits in 3 seconds.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/HabitViewModel.kt`

**Step 1: Add a debounced sync+widget refresher**

In `HabitViewModel`, replace the many `triggerSync()` + `updateWidget()` call sites with a single `scheduleRefresh()`:

```kotlin
private val refreshChannel = kotlinx.coroutines.channels.Channel<Unit>(capacity = Channel.CONFLATED)

init {
    // existing user-id collector...
    // Debounced refresher: coalesce all refresh requests within 500ms
    viewModelScope.launch {
        var lastSync = 0L
        refreshChannel.consumeAsFlow().collect {
            kotlinx.coroutines.delay(500) // coalesce window
            val now = System.currentTimeMillis()
            if (now - lastSync > 2000) { // min 2s between actual syncs
                lastSync = now
                syncManager.fullSync()
            }
            updateWidgetNow()
        }
    }
}

private fun scheduleRefresh() {
    refreshChannel.trySend(Unit)
}

private suspend fun updateWidgetNow() {
    try { com.example.loophabit.widget.HabitWidget().updateAll(applicationContext) } catch (_: Exception) {}
}
```

**Step 2: Replace all `triggerSync()` + `updateWidget()` pairs with `scheduleRefresh()`**

In `addHabit`, `deleteHabit`, `completeHabit`, `uncompleteHabit`, `logFocusSession`, `nextHabit`, `prevHabit`, `setIndex`, `completeHabitNumerical`, `addTodo`, `updateTodo`, `toggleTodo`, `deleteTodo`, `setManualFocusMinutes`, `toggleHabitCompletionForDate` — replace the two calls with one `scheduleRefresh()`.

For `login`, `register`, `logout`, `resetAllData`, `importData` — keep an immediate `syncManager.fullSync()` since those are infrequent and user-initiated, but still call `scheduleRefresh()` for the widget.

**Step 3: Build & verify**

Run: `gradlew.bat assembleDebug`, swipe through habits quickly, watch logcat — sync should fire once after you stop, not 5 times.
Expected: BUILD SUCCESSFUL, noticeably snappier swipe-to-complete.

---

### Task A4: Memoize per-row focus-time lookups into a derived Map StateFlow

**Objective:** Stop calling `manualFocusMinutesForHabit`/`hasFocusTimeForHabitOnDate` (which scan `allFocusSessions` and construct `SimpleDateFormat` per call) inside every `items()` row.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/HabitViewModel.kt`
- Modify: `app/src/main/java/com/example/loophabit/ui/MainScreen.kt:588-597` (TodayTab after A2)

**Step 1: Add a derived StateFlow mapping habitId → focus info for the selected date**

In `HabitViewModel`:
```kotlin
data class FocusRowInfo(val hasFocusTime: Boolean, val manualMinutes: Int)

val focusInfoForSelectedDate: StateFlow<Map<Long, FocusRowInfo>> =
    combine(allFocusSessions, _selectedDate) { sessions, date ->
        val manualDetails = "Manual time • $date"
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val map = mutableMapOf<Long, FocusRowInfo>()
        // group by habitId once
        sessions.groupBy { it.habitId }.forEach { (habitId, list) ->
            val manual = list.firstOrNull { it.details == manualDetails }
            val hasTime = list.any { it.details == manualDetails || formatter.format(Date(it.timestamp)) == date }
            map[habitId] = FocusRowInfo(hasTime, (manual?.durationSeconds ?: 0) / 60)
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
```

**Step 2: Use it in the completed-habits `items()` block**

```kotlin
val focusInfo by viewModel.focusInfoForSelectedDate.collectAsState()
items(completedHabitsForSelected, key = { it.id }) { habit ->
    val info = focusInfo[habit.id] ?: FocusRowInfo(false, 0)
    CompletedHabitRow(
        habit = habit,
        manualMinutes = info.manualMinutes,
        showManualTimeAction = !info.hasFocusTime,
        onEditManualTime = { onOpenManualTime(habit) },
        onUncomplete = { viewModel.uncompleteHabit(habit.id) }
    )
}
```

**Step 3: Remove the now-unused `manualFocusMinutesForHabit`/`hasFocusTimeForHabitOnDate` public functions** (or keep as deprecated internal if Dialogs.kt uses them — check first; `MainScreen.kt:679` uses `manualFocusMinutesForHabit` for the dialog `remember`, so keep that one but make it read from the same map or keep as-is since the dialog is not in a hot path).

**Step 4: Build & verify**

Run: `gradlew.bat assembleDebug`.
Expected: BUILD SUCCESSFUL. Completed list scrolls smoother with many habits.

---

### Task A5: Memoize InsightsDashboard computations

**Objective:** Ensure all derived values in Insights are `remember`'d against their inputs.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/InsightsComponents.kt:104-150`

**Step 1: Wrap `totalCompletions`, `completionsInLast30`, `overallConsistency` in remember**

```kotlin
val totalCompletions = remember(allCompletions) { allCompletions.size }

val last30Days = remember(today) { (0 until 30).map { today.minusDays(it.toLong()).toString() } }
val completionsInLast30 = remember(allCompletions, last30Days) {
    val set = last30Days.toSet()
    allCompletions.count { set.contains(it.date) }
}
val overallConsistency = remember(completionsInLast30, allHabits) {
    val max = allHabits.size * 30
    if (max > 0) (completionsInLast30.toFloat() / max * 100).roundToInt() else 0
}
```

**Step 2: Verify the `remember` keys are correct** — anything depending on `allCompletions` or `allHabits` must key on them.

**Step 3: Build & verify**

Run: `gradlew.bat assembleDebug`, open Insights, scroll.
Expected: BUILD SUCCESSFUL.

---

### Task A6: Convert currentUserId init collector to a self-stopping stateIn

**Objective:** Stop the always-running `init { collect }` that touches preferences forever.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/HabitViewModel.kt:61-82`

**Step 1: Replace the init collector with a launch that runs once then a guarded stateIn**

```kotlin
init {
    viewModelScope.launch {
        val userId = repository.currentUserIdFlow.first()
        if (userId == 0L) {
            val defaultUser = repository.getUserByUsername("local_user")
            if (defaultUser == null) {
                val newUser = User(username = "local_user", email = "local@loophabit.com", password = "local_password", securityQuestion = "Local?", securityAnswer = "Yes")
                val insertedId = repository.registerUser(newUser)
                repository.setCurrentUserId(insertedId)
            } else {
                repository.setCurrentUserId(defaultUser.id)
            }
        }
    }
}
```

Using `.first()` instead of `collect` makes it self-terminate after the first emission.

**Step 2: Build & verify**

Run: `gradlew.bat assembleDebug`.
Expected: BUILD SUCCESSFUL.

---

### Phase B: Loading States & Optimistic UI

---

### Task B1: Add a reusable shimmer/skeleton composable

**Objective:** Provide a lightweight shimmer placeholder to show while data loads.

**Files:**
- Create: `app/src/main/java/com/example/loophabit/ui/Shimmer.kt`

**Step 1: Create the shimmer modifier + skeleton composables**

```kotlin
package com.example.loophabit.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
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
        Modifier.fillMaxWidth(0.9f).height(280.dp).clip(RoundedCornerShape(28.dp)).shimmer()
    )
}

@Composable
fun CompletedRowSkeleton() {
    Box(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp)).shimmer()
    )
}
```

**Step 2: Build**

Run: `gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL (new file compiles).

---

### Task B2: Show skeletons in TodayTab while habits load

**Objective:** Avoid the blank-then-pop effect on first launch.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/MainScreen.kt` (TodayTab from A2)

**Step 1: Track a "loaded" flag**

In `TodayTab`:
```kotlin
val incompleteHabitsForSelected by viewModel.incompleteHabitsForSelected.collectAsState()
val completedHabitsForSelected by viewModel.completedHabitsForSelected.collectAsState()
val currentUserId by viewModel.currentUserId.collectAsState()
val isLoading = currentUserId == 0L || (incompleteHabitsForSelected.isEmpty() && completedHabitsForSelected.isEmpty() && /* first-load guard */ !viewModel.hasInitiallyLoaded)
```

A simpler heuristic: track a `LaunchedEffect(Unit)` that sets a `var loaded by remember { mutableStateOf(false) }` after a short delay OR after the first non-empty emission. The cleanest: add a `val isInitialLoad: StateFlow<Boolean>` to the ViewModel that's true until the first DB emission returns.

**Step 2: Render skeletons when loading, real content otherwise**

```kotlin
if (isLoading) {
    item { HabitCardSkeleton() }
    item { Spacer(Modifier.height(20.dp)) }
    repeat(3) { item { CompletedRowSkeleton(); Spacer(Modifier.height(8.dp)) } }
} else {
    // existing content
}
```

**Step 3: Build & verify cold start shows shimmer briefly**

Run: `gradlew.bat assembleDebug`, install fresh, launch.
Expected: shimmer cards appear instead of blank screen.

---

### Task B3: Make completeHabit / uncompleteHabit / toggleTodo optimistic

**Objective:** UI reflects the action immediately without waiting for the Room Flow round-trip.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/HabitViewModel.kt`

**Step 1: Add an optimistic overlay StateFlow for "just-completed" habits**

```kotlin
private val _optimisticCompletions = MutableStateFlow<Set<Long>>(emptySet())
val optimisticCompletions = _optimisticCompletions.asStateFlow()

fun completeHabit(habitId: Long) {
    _optimisticCompletions.value = _optimisticCompletions.value + habitId
    viewModelScope.launch {
        val userId = currentUserId.value
        if (userId != 0L) {
            repository.completeHabit(userId, habitId, _selectedDate.value)
            // remove optimistic once Room confirms (next emission will move it to completed list)
            kotlinx.coroutines.delay(300)
            _optimisticCompletions.value = _optimisticCompletions.value - habitId
            scheduleRefresh()
        }
    }
}
```

**Step 2: In TodayTab, subtract optimistic IDs from incomplete and add to completed**

```kotlin
val optimistic by viewModel.optimisticCompletions.collectAsState()
val effectiveIncomplete = incompleteHabitsForSelected.filter { it.id !in optimistic }
val effectiveCompleted = completedHabitsForSelected + incompleteHabitsForSelected.filter { it.id in optimistic }
```

Use these in the card stack and completed list.

**Step 3: Do the same for `toggleTodo`** with an `_optimisticTodoToggles: MutableStateFlow<Set<Long>>`.

**Step 4: Build & verify swipe-to-complete feels instant**

Run: `gradlew.bat assembleDebug`, swipe a card right.
Expected: card disappears instantly, sync happens in background.

---

### Task B4: Improve the sync status indicator to show real progress

**Objective:** Give the user visible feedback that sync is happening.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/MainScreen.kt:124-160` (`SyncStatusIndicator`)

**Step 1: Make the indicator animated and informative**

```kotlin
@Composable
fun SyncStatusIndicator(syncState: SyncState) {
    val transition = updateTransition(syncState, label = "sync")
    val tint by transition.animateColor { state ->
        when (state) {
            is SyncState.Syncing -> MaterialTheme.colorScheme.primary
            is SyncState.Completed -> MaterialTheme.colorScheme.tertiary
            is SyncState.Error -> MaterialTheme.colorScheme.error
            SyncState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        }
    }
    Box(contentAlignment = Alignment.Center) {
        if (syncState is SyncState.Syncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = tint
            )
        } else {
            Icon(Icons.Outlined.CheckCircle, contentDescription = "sync", tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}
```

**Step 2: Build & verify**

Run: `gradlew.bat assembleDebug`.
Expected: spinning ring during sync.

---

### Phase C: UI Polish

---

### Task C1: Shorten tab transition and use Crossfade for heavy tabs

**Objective:** Reduce the 300ms slide that holds two heavy tab trees alive.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/MainScreen.kt:363-378`

**Step 1: Replace the slide transition with a faster fade-based one**

```kotlin
AnimatedContent(
    targetState = activeTab,
    transitionSpec = {
        (fadeIn(tween(180)) togetherWith fadeOut(tween(180))).using(
            SizeTransform(clip = false)
        )
    },
    label = "tabTransition"
)
```

180ms crossfade is perceptually snappier and avoids composing two LazyColumns simultaneously.

**Step 2: Build & verify**

Run: `gradlew.bat assembleDebug`, tap tabs.
Expected: snappier tab switches.

---

### Task C2: Add haptic feedback on swipe-to-complete

**Objective:** Tactile confirmation makes the action feel responsive.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/HabitCards.kt:82-128`

**Step 1: Add haptic in onDragEnd when threshold crossed**

```kotlin
val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
// in onDragEnd:
if (dragOffset > swipeThreshold) {
    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    // existing animate
    onSwipeRight()
} else if (dragOffset < -swipeThreshold) {
    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
    // existing
    onSwipeLeft()
}
```

**Step 2: Build & verify on device**

Run: `gradlew.bat assembleDebug`, install, swipe.
Expected: subtle vibration on complete.

---

### Task C3: Polish Insights chart visuals

**Objective:** Nicer-looking bars and empty states.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/InsightsComponents.kt`

**Step 1: Use gradient fill for bars and rounded caps**

Replace the `Box(...).background(MaterialTheme.colorScheme.primary...)` bar with:
```kotlin
Box(
    Modifier.fillMaxWidth().height(barHeight)
        .clip(RoundedCornerShape(8.dp))
        .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))))
)
```

**Step 2: Add a friendly empty state** when `allHabits.isEmpty()`:
```kotlin
if (allHabits.isEmpty()) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Insights, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.4f))
        Spacer(Modifier.height(12.dp))
        Text("No insights yet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Add a habit to start tracking your progress", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    return
}
```

**Step 3: Build & verify**

Run: `gradlew.bat assembleDebug`.
Expected: BUILD SUCCESSFUL, nicer bars.

---

### Task C4: Refine spacing & typography in HabitCardContent

**Objective:** Tighter, more modern card look.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/HabitCards.kt:134-279`

**Step 1: Adjust paddings and type scale**

- Card padding `24.dp` → `20.dp`
- Title `headlineMedium` → `headlineSmall` with `maxLines = 2`
- Add a subtle `shadowElevation` via `CardDefaults.cardElevation(defaultElevation = 8.dp)` for top card
- Swipe hint text size 12sp → 11sp, letter spacing tweak

**Step 2: Build & verify visually**

Run: `gradlew.bat assembleDebug`, install, screenshot.
Expected: tighter card.

---

### Task C5: Add an empty-state for the TODAY tab when no habits exist

**Objective:** Currently the "All done!" state only shows when habits exist but are completed; a true zero-habit state should prompt creation.

**Files:**
- Modify: `app/src/main/java/com/example/loophabit/ui/MainScreen.kt` (TodayTab)

**Step 1: Add branch**

```kotlin
if (effectiveIncomplete.isEmpty() && completedHabitsForSelected.isEmpty()) {
    item {
        Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.AddCircle, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha=0.5f))
            Spacer(Modifier.height(16.dp))
            Text("Start your first loop", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text("Tap + to add a habit", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

**Step 2: Build & verify**

Run: `gradlew.bat assembleDebug`.
Expected: BUILD SUCCESSFUL.

---

## Verification (end-to-end)

After all tasks:

1. **Build:** `cmd.exe /c "cd /d C:\Users\sigmu\Desktop\LoopHabit && gradlew.bat assembleDebug --console=plain"` → BUILD SUCCESSFUL
2. **Install:** `adb install -r app\build\outputs\apk\debug\LoopHabit-debug.apk`
3. **Cold start:** launch app, confirm shimmer skeletons appear briefly, then content.
4. **Swipe-to-complete:** swipe a card, confirm it vanishes instantly + haptic + sync ring spins once after stop.
5. **Tab switch:** tap through Today/Focus/Todo/Insights rapidly, confirm no jank.
6. **Insights:** open, confirm bars have gradients, empty state shows with no habits.
7. **Sync:** confirm logcat shows one `fullSync` after a burst of swipes, not N.
8. **Widget:** confirm home-screen widget still updates (debounced).

## Risks & Tradeoffs

- **Debounced sync (A3):** if the app is killed within the 500ms coalesce window, the last mutation may not sync. Acceptable for a habit tracker; the local DB is the source of truth and the next launch syncs. Mitigation: also call `scheduleRefresh()` in `onPause`/`onStop` via a lifecycle observer.
- **Optimistic UI (B3):** if the DB write fails (rare — Room is local), the optimistic state lingers for 300ms then reverts on next Flow emission. Acceptable.
- **Moving theme to MainActivity (A1):** the `darkModeEnabled` is read in `MainScreen` for the logo; passing as a param is a small API change. Low risk.
- **Splitting MainScreen (A2):** large diff; risk of breaking dialog state hoisting. Do this one carefully and build after each extraction.
- **No new tests:** the existing test suite is minimal (`ExampleUnitTest`, `HabitCompletionMapperTest`). The plan relies on manual verification + build success. Adding unit tests for the new `FocusRowInfo` mapper and the debounce logic would be valuable but is out of scope unless requested.

## Open Questions

1. Should sync run on a dedicated `Dispatchers.IO` context instead of `viewModelScope` default? (Currently `syncManager.fullSync()` is `suspend` and Ktor switches to IO internally, but the `viewModelScope.launch` is on Main.) Recommend wrapping `fullSync()` call in `withContext(Dispatchers.IO)` in `scheduleRefresh`.
2. Is the Supabase backend configured with real credentials? `build.gradle.kts:30-31` has placeholder values — sync will always fail silently for local-only users. The debouncer should gracefully no-op when `!authStateProvider.isSignedIn`.

---

## Files Likely to Change

- `app/src/main/java/com/example/loophabit/MainActivity.kt`
- `app/src/main/java/com/example/loophabit/ui/MainScreen.kt` (major)
- `app/src/main/java/com/example/loophabit/ui/HabitViewModel.kt` (major)
- `app/src/main/java/com/example/loophabit/ui/HabitCards.kt`
- `app/src/main/java/com/example/loophabit/ui/InsightsComponents.kt`
- `app/src/main/java/com/example/loophabit/ui/Shimmer.kt` (new)
