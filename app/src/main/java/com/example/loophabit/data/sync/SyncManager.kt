package com.example.loophabit.data.sync

import com.example.loophabit.data.AppDatabase
import com.example.loophabit.data.Habit
import com.example.loophabit.data.HabitCompletion
import com.example.loophabit.data.User
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
import java.util.concurrent.atomic.AtomicBoolean

/** Sync state sealed class */
sealed interface SyncState {
    data class Syncing(val progress: Float, val message: String) : SyncState
    object Idle : SyncState
    object Completed : SyncState
    data class Error(val message: String) : SyncState
}

/** Result of a successful sync */
data class SyncResult(
    val pushedCount: Int,
    val pulledCount: Int
)

/** Result of push operation */
data class PushResult(val pushedCount: Int)

/** Result of pull operation */
data class PullResult(val pulledCount: Int)

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
) {

    // Sync state exposed to UI
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    // Prevent concurrent syncs
    private val isSyncing = AtomicBoolean(false)

    /** Performs a full bidirectional sync: push local → pull remote */
    suspend fun fullSync(): Result<SyncResult> {
        // Check authentication before syncing
        if (!authStateProvider.isSignedIn) {
            return Result.failure(Exception("Not authenticated. Please sign in to sync."))
        }

        if (!isSyncing.compareAndSet(false, true)) {
            return Result.failure(Exception("Sync already in progress"))
        }

        _syncState.value = SyncState.Syncing(progress = 0.0f, message = "Starting sync...")

        return try {
            // Step 1: Push local changes to Supabase
            _syncState.value = SyncState.Syncing(progress = 0.2f, message = "Pushing local changes...")
            val pushResult = pushLocalChanges()
            if (pushResult.isFailure) {
                _syncState.value = SyncState.Error(pushResult.exceptionOrNull()?.message ?: "Push failed")
                Result.failure(pushResult.exceptionOrNull() ?: Exception("Push failed"))
            } else {
                // Step 2: Pull remote changes from Supabase
                _syncState.value = SyncState.Syncing(progress = 0.6f, message = "Pulling remote changes...")
                val pullResult = pullRemoteChanges()
                if (pullResult.isFailure) {
                    _syncState.value = SyncState.Error(pullResult.exceptionOrNull()?.message ?: "Pull failed")
                    Result.failure(pullResult.exceptionOrNull() ?: Exception("Pull failed"))
                } else {
                    _syncState.value = SyncState.Completed
                    Result.success(SyncResult(pushResult.getOrNull()?.pushedCount ?: 0, pullResult.getOrNull()?.pulledCount ?: 0))
                }
            }
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            Result.failure(e)
        } finally {
            isSyncing.set(false)
        }
    }

    /** Pushes local changes to Supabase */
    suspend fun pushLocalChanges(): Result<PushResult> {
        return try {
            var totalPushed = 0

            // Push users first (FK dependency for habits)
            val users = database.userDao().getAllUsers().first()
            for (user in users) {
                val userDto = UserMapper.toDto(user)
                val result = supabaseSyncClient.upsertProfile(userDto)
                if (result.isSuccess) {
                    totalPushed++
                }
            }

            // Push habits
            val habits = database.habitDao().getAllHabits().first()
            for (habit in habits) {
                val habitDto = HabitMapper.toDto(habit)
                val result = supabaseSyncClient.upsertHabit(habitDto)
                if (result.isSuccess) {
                    totalPushed++
                }
            }

            // Push completions
            val completions = database.habitDao().getAllCompletions().first()
            for (completion in completions) {
                val completionDto = HabitCompletionMapper.toDto(completion)
                val result = supabaseSyncClient.upsertCompletion(completionDto)
                if (result.isSuccess) {
                    totalPushed++
                }
            }

            Result.success(PushResult(pushedCount = totalPushed))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Pulls remote changes from Supabase into local database */
    suspend fun pullRemoteChanges(): Result<PullResult> {
        return try {
            var totalPulled = 0

            // Pull users
            val userResult = supabaseSyncClient.selectProfiles()
            if (userResult.isSuccess) {
                for (dto in userResult.getOrNull()!!) {
                    val user = UserMapper.toEntity(dto)
                    database.userDao().upsertUser(user)
                    totalPulled++
                }
            }

            // Pull habits
            val habitResult = supabaseSyncClient.selectHabits()
            if (habitResult.isSuccess) {
                for (dto in habitResult.getOrNull()!!) {
                    val habit = HabitMapper.toEntity(dto)
                    database.habitDao().upsertHabit(habit)
                    totalPulled++
                }
            }

            // Pull completions
            val completionResult = supabaseSyncClient.selectCompletions()
            if (completionResult.isSuccess) {
                for (dto in completionResult.getOrNull()!!) {
                    val completion = HabitCompletionMapper.toEntity(dto)
                    database.habitDao().upsertCompletion(completion)
                    totalPulled++
                }
            }

            Result.success(PullResult(pulledCount = totalPulled))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Checks if a sync is currently in progress */
    fun isCurrentlySyncing(): Boolean = isSyncing.get()
}
