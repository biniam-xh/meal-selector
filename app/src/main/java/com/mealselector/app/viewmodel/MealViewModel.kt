package com.mealselector.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mealselector.app.data.GroupSetupResult
import com.mealselector.app.data.MealRepository
import com.mealselector.app.data.MealState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MealViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MealRepository(application)

    val mealState: StateFlow<MealState> = repository.mealState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MealState()
        )

    private val _isSetupLoading = MutableStateFlow(false)
    val isSetupLoading: StateFlow<Boolean> = _isSetupLoading.asStateFlow()

    private val _setupError = MutableStateFlow<String?>(null)
    val setupError: StateFlow<String?> = _setupError.asStateFlow()

    fun createGroup() {
        viewModelScope.launch {
            _isSetupLoading.value = true
            _setupError.value = null
            when (val result = repository.createGroup()) {
                is GroupSetupResult.Success -> Unit
                is GroupSetupResult.Error -> _setupError.value = result.message
            }
            _isSetupLoading.value = false
        }
    }

    fun joinGroup(code: String) {
        viewModelScope.launch {
            _isSetupLoading.value = true
            _setupError.value = null
            when (val result = repository.joinGroup(code)) {
                is GroupSetupResult.Success -> Unit
                is GroupSetupResult.Error -> _setupError.value = result.message
            }
            _isSetupLoading.value = false
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            repository.leaveGroup()
        }
    }

    fun clearSetupError() {
        _setupError.value = null
    }

    fun toggleLock() {
        viewModelScope.launch {
            repository.toggleLock()
        }
    }

    fun rotateToNext() {
        viewModelScope.launch {
            repository.rotateToNext()
        }
    }

    fun addMeal(name: String) {
        viewModelScope.launch {
            repository.addMeal(name)
        }
    }

    fun removeMeal(index: Int) {
        viewModelScope.launch {
            repository.removeMeal(index)
        }
    }

    fun moveMealUp(index: Int) {
        viewModelScope.launch {
            repository.moveMealUp(index)
        }
    }

    fun moveMealDown(index: Int) {
        viewModelScope.launch {
            repository.moveMealDown(index)
        }
    }

    fun applyLockOnAppStart() {
        viewModelScope.launch {
            repository.applyLockOnAppStart()
        }
    }

    fun setLockByDefault(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLockByDefault(enabled)
        }
    }

    fun setLockOnRotate(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLockOnRotate(enabled)
        }
    }
}
