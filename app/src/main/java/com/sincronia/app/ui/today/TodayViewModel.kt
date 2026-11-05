package com.sincronia.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.core.log.AppLogger
import com.sincronia.app.data.calendar.EventRepository
import com.sincronia.app.data.sync.SyncCoordinator
import com.sincronia.app.data.tasks.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    events: EventRepository,
    tasks: TaskRepository,
    private val sync: SyncCoordinator,
    private val logger: AppLogger,
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val accountId = "demo"

    // Ticks at every minute boundary: moves the "now" line and switches to the next day after midnight.
    private val clock = flow {
        while (true) {
            val now = Instant.now()
            emit(now)
            delay(MINUTE_MILLIS - now.toEpochMilli() % MINUTE_MILLIS)
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val today = clock.map { it.atZone(zone).toLocalDate() }.distinctUntilChanged()
    private val refreshing = MutableStateFlow(false)

    private val day = today.flatMapLatest { date ->
        combine(
            events.day(accountId, date),
            tasks.overdue(accountId, date),
            tasks.day(accountId, date),
            tasks.completedOn(accountId, date, zone),
        ) { dayEvents, overdue, todayTasks, completed ->
            DayItems(date, dayEvents, overdue, todayTasks, completed)
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(day, clock, refreshing) { items, now, isRefreshing ->
        TodayPlanner.build(
            date = items.date,
            now = now,
            zone = zone,
            events = items.events,
            overdue = items.overdue,
            todayTasks = items.todayTasks,
            completed = items.completed,
            refreshing = isRefreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Pull to refresh runs the same pipeline as SyncWorker; its Mutex avoids overlapping runs. */
    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            try {
                sync.run()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                logger.w(TAG, "Pull to refresh failed", error)
            } finally {
                refreshing.value = false
            }
        }
    }

    private data class DayItems(
        val date: LocalDate,
        val events: List<EventEntity>,
        val overdue: List<TaskEntity>,
        val todayTasks: List<TaskEntity>,
        val completed: List<TaskEntity>,
    )

    private companion object {
        const val TAG = "Today"
        const val MINUTE_MILLIS = 60_000L
    }
}
