package com.example.loophabit.data.sync

import com.example.loophabit.data.AppDatabase
import com.example.loophabit.data.Habit
import com.example.loophabit.data.HabitCompletion
import com.example.loophabit.data.User
import com.example.loophabit.data.SyncState
import com.example.loophabit.data.supabase.dto.HabitCompletionDto
import com.example.loophabit.data.supabase.dto.HabitDto
import com.example.loophabit.data.supabase.dto.UserDto
import com.example.loophabit.data.supabase.mappers.EntityDtoMapper
import com.example.loophabit.data.supabase.mappers.HabitCompletionMapper
import com.example.loophabit.data.supabase.mappers.HabitMapper
import com.example.loophabit.data.supabase.mappers.UserMapper
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Interface for Supabase client to enable testing and avoid direct dependency */
interface SupabaseSyncClient {
    suspend fun upsertProfile(dto: UserDto): Result<Unit>
    suspend fun upsertHabit(dto: HabitDto): Result<Unit>
    suspend fun upsertCompletion(dto: HabitCompletionDto): Result<Unit>
    suspend fun selectProfiles(): Result<List<UserDto>>
    suspend fun selectHabits(): Result<List<HabitDto>>
    suspend fun selectCompletions(): Result<List<HabitCompletionDto>>
}

/** Factory for creating SupabaseSyncClient */
object SupabaseSyncClientFactory {
    fun create(): SupabaseSyncClient = RealSupabaseSyncClient()
}

/** Real implementation using the actual Supabase client */
class RealSupabaseSyncClient : SupabaseSyncClient {
    override suspend fun upsertProfile(dto: UserDto): Result<Unit> {
        return try {
            com.example.loophabit.data.supabase.SupabaseClient.postgrest["profiles"].upsert(dto)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun upsertHabit(dto: HabitDto): Result<Unit> {
        return try {
            com.example.loophabit.data.supabase.SupabaseClient.postgrest["habits"].upsert(dto)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun upsertCompletion(dto: HabitCompletionDto): Result<Unit> {
        return try {
            com.example.loophabit.data.supabase.SupabaseClient.postgrest["habit_completions"].upsert(dto)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun selectProfiles(): Result<List<UserDto>> {
        return try {
            val list = com.example.loophabit.data.supabase.SupabaseClient.postgrest["profiles"].select().decodeList<UserDto>()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun selectHabits(): Result<List<HabitDto>> {
        return try {
            val list = com.example.loophabit.data.supabase.SupabaseClient.postgrest["habits"].select().decodeList<HabitDto>()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun selectCompletions(): Result<List<HabitCompletionDto>> {
        return try {
            val list = com.example.loophabit.data.supabase.SupabaseClient.postgrest["habit_completions"].select().decodeList<HabitCompletionDto>()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/** Interface for auth state to avoid direct dependency */
interface AuthStateProvider {
    val isSignedIn: Boolean
}

/**
 * Manages bidirectional synchronization between local Room database and remote Supabase.
 * Sync order respects FK constraints: users → habits → habit_completions.
 */
class SyncManager(
    private val database: AppDatabase,
    private val authStateProvider: AuthStateProvider,
    private val supabaseSyncClient: SupabaseSyncClient = RealSupabaseSyncClient()
) : com.example.loophabit.data.SyncManager {

    // Sync state exposed to UI
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState = _syncState.asStateFlow()

    // Prevent concurrent syncs
    private var isSyncing = false
    private val mutex = Mutex()

    /** Performs a full bidirectional sync: push local → pull remote */
    override suspend fun fullSync(): Boolean {
        // Check authentication before syncing
        if (!authStateProvider.isSignedIn) {
            _syncState.value = SyncState.Error("Not authenticated. Please sign in to sync.")
            return false
        }

        mutex.withLock {
            if (isSyncing) {
                return false
            }
            isSyncing = true
        }

        _syncState.value = SyncState.Syncing(progress = 0, message = "Starting sync...")

        return try {
            // Step 1: Push local changes to Supabase
            _syncState.value = SyncState.Syncing(progress = 20, message = "Pushing local changes...")
            val pushResult = pushLocalChanges()
            if (!pushResult) {
                _syncState.value = SyncState.Error("Push failed")
                false
            } else {
                // Step 2: Pull remote changes from Supabase
                _syncState.value = SyncState.Syncing(progress = 60, message = "Pulling remote changes...")
                val pullResult = pullRemoteChanges()
                if (!pullResult) {
                    _syncState.value = SyncState.Error("Pull failed")
                    false
                } else {
                    _syncState.value = SyncState.Completed
                    true
                }
            }
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            false
        } finally {
            mutex.withLock {
                isSyncing = false
            }
        }
    }

    /** Pushes local changes to Supabase */
    override suspend fun pushLocalChanges(): Boolean {
        return try {
            // Push users first (FK dependency for habits)
            val users = database.userDao().getAllUsers().first()
            for (user in users) {
                val userDto = UserMapper.toDto(user)
                supabaseSyncClient.upsertProfile(userDto)
            }

            // Push habits
            val habits = database.habitDao().getAllHabits().first()
            for (habit in habits) {
                val habitDto = HabitMapper.toDto(habit)
                supabaseSyncClient.upsertHabit(habitDto)
            }

            // Push completions
            val completions = database.habitDao().getAllCompletions().first()
            for (completion in completions) {
                val completionDto = HabitCompletionMapper.toDto(completion)
                supabaseSyncClient.upsertCompletion(completionDto)
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /** Pulls remote changes from Supabase into local database */
    override suspend fun pullRemoteChanges(): Boolean {
        return try {
            // Pull users
            val userResult = supabaseSyncClient.selectProfiles()
            if (userResult.isSuccess) {
                for (dto in userResult.getOrNull()!!) {
                    val user = UserMapper.toEntity(dto)
                    database.userDao().upsertUser(user)
                }
            }

            // Pull habits
            val habitResult = supabaseSyncClient.selectHabits()
            if (habitResult.isSuccess) {
                for (dto in habitResult.getOrNull()!!) {
                    val habit = HabitMapper.toEntity(dto)
                    database.habitDao().upsertHabit(habit)
                }
            }

            // Pull completions
            val completionResult = supabaseSyncClient.selectCompletions()
            if (completionResult.isSuccess) {
                for (dto in completionResult.getOrNull()!!) {
                    val completion = HabitCompletionMapper.toEntity(dto)
                    database.habitDao().upsertCompletion(completion)
                }
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /** Checks if a sync is currently in progress */
    override fun isCurrentlySyncing(): Boolean = isSyncing
}
