package com.example.loophabit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.loophabit.data.Habit
import com.example.loophabit.data.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

    val todayDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val allHabits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incompleteHabits: StateFlow<List<Habit>> = repository.getIncompleteHabitsOfToday(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedHabits: StateFlow<List<Habit>> = repository.getCompletedHabitsOfToday(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loopIndex: StateFlow<Int> = repository.loopIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentHabit: StateFlow<Habit?> = combine(
        repository.getIncompleteHabitsOfToday(todayDate),
        repository.loopIndexFlow
    ) { habits, index ->
        if (habits.isEmpty()) return@combine null
        val safeIndex = if (index < 0 || index >= habits.size) 0 else index
        habits[safeIndex]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addHabit(title: String, colorHex: String) {
        viewModelScope.launch {
            repository.addHabit(title, colorHex, todayDate)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit, todayDate)
        }
    }

    fun completeHabit(habitId: Long) {
        viewModelScope.launch {
            repository.completeHabit(habitId, todayDate)
        }
    }

    fun uncompleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.uncompleteHabit(habitId, todayDate)
        }
    }

    fun nextHabit() {
        viewModelScope.launch {
            repository.cycleIndex(1, todayDate)
        }
    }

    fun prevHabit() {
        viewModelScope.launch {
            repository.cycleIndex(-1, todayDate)
        }
    }

    fun setIndex(index: Int) {
        viewModelScope.launch {
            repository.setLoopIndex(index)
        }
    }
}

class HabitViewModelFactory(private val repository: HabitRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
