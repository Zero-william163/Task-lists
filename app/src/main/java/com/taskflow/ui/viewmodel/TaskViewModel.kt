package com.taskflow.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskflow.data.model.Task
import com.taskflow.data.repository.TaskRepository
import com.taskflow.widget.TaskFlowWidgetReceiver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository,
    private val appContext: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _showCompleted = MutableStateFlow(false)
    val showCompleted: StateFlow<Boolean> = _showCompleted.asStateFlow()

    val tasks: StateFlow<List<Task>> = combine(
        _searchQuery,
        _selectedCategory,
        _showCompleted
    ) { query, category, showCompleted ->
        Triple(query, category, showCompleted)
    }.flatMapLatest { (query, category, showCompleted) ->
        when {
            query.isNotBlank() -> repository.searchActiveTasks(query)
            category != null -> repository.getTasksByCategory(category)
            showCompleted -> repository.allCompletedTasks
            else -> repository.allActiveTasks
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTaskCount: StateFlow<Int> = repository.activeTaskCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            _selectedCategory.value = null
            _showCompleted.value = false
        }
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
        if (category != null) {
            _searchQuery.value = ""
            _showCompleted.value = false
        }
    }

    fun toggleShowCompleted() {
        _showCompleted.value = !_showCompleted.value
        if (_showCompleted.value) {
            _searchQuery.value = ""
            _selectedCategory.value = null
        }
    }

    fun selectTask(task: Task?) {
        _selectedTask.value = task
    }

    fun addTask(task: Task, onResult: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.insertTask(task)
            updateWidget()
            onResult?.invoke(id)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
            updateWidget()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            updateWidget()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
            )
            repository.updateTask(updated)
            updateWidget()
        }
    }

    fun deleteAllCompleted() {
        viewModelScope.launch {
            repository.deleteAllCompleted()
            updateWidget()
        }
    }

    fun loadTask(id: Long) {
        viewModelScope.launch {
            _selectedTask.value = repository.getTaskById(id)
        }
    }

    private fun updateWidget() {
        TaskFlowWidgetReceiver.updateAllWidgets(appContext)
    }

    class Factory(
        private val repository: TaskRepository,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                return TaskViewModel(repository, appContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
