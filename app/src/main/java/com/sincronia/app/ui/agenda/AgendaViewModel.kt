package com.sincronia.app.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.data.calendar.EventRepository
import com.sincronia.app.data.tasks.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class AgendaUiState(
    val selected: LocalDate = LocalDate.now(),
    val events: List<EventEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val weekEvents: List<EventEntity> = emptyList(),
    val weekTasks: List<TaskEntity> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class AgendaViewModel @Inject constructor(
    events: EventRepository,
    tasks: TaskRepository,
) : ViewModel() {
    private val selected = MutableStateFlow(LocalDate.now())
    private val dayEvents = selected.flatMapLatest { events.day("demo", it) }
    private val dayTasks = selected.flatMapLatest { tasks.day("demo", it) }
    private val weekEvents = selected.flatMapLatest {
        events.week("demo", it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
    private val weekTasks = selected.flatMapLatest {
        tasks.week("demo", it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
    val uiState: StateFlow<AgendaUiState> = combine(selected, dayEvents, dayTasks, weekEvents, weekTasks) { date, e, t, week, weekTasks ->
        AgendaUiState(date, e, t, week, weekTasks, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgendaUiState())

    fun select(date: LocalDate) { selected.value = date }
    fun moveWeek(weeks: Long) { selected.value = selected.value.plusWeeks(weeks) }
}
