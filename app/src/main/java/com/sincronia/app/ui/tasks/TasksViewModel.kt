package com.sincronia.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.core.database.TaskListEntity
import com.sincronia.app.data.tasks.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class TasksUiState(
    val lists: List<TaskListEntity> = emptyList(),
    val selectedListId: String? = null,
    val tasks: List<TaskEntity> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(repository: TaskRepository) : ViewModel() {
    private val selected = MutableStateFlow<String?>(null)
    private val lists = repository.lists("demo")
    private val selection = combine(lists, selected) { available, choice ->
        available.firstOrNull { it.googleId == choice }?.googleId ?: available.firstOrNull()?.googleId
    }
    private val tasks = selection.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.list("demo", id)
    }
    val uiState: StateFlow<TasksUiState> = combine(lists, selection, tasks) { available, choice, items ->
        TasksUiState(available, choice, items, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun selectList(id: String) { selected.value = id }
}
