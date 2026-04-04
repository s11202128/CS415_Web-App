package com.bof.mobile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bof.mobile.data.repository.DashboardRepository
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.DashboardResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val data: DashboardResponse? = null,
    val errorMessage: String? = null,
    val lastLoadedCustomerId: Int? = null,
    val lastUpdatedAtEpochMs: Long? = null,
    val hasLoadedOnce: Boolean = false
)

class DashboardViewModel(private val dashboardRepository: DashboardRepository) : ViewModel() {
    private val tag = "DashboardViewModel"
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState
    private var loadJob: Job? = null

    fun loadDashboard(customerId: Int?) {
        val normalizedCustomerId = customerId?.takeIf { it > 0 }
        loadJob?.cancel()
        Log.d(tag, "loadDashboard requested customerId=$normalizedCustomerId")

        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = dashboardRepository.getDashboard(normalizedCustomerId)) {
                    is ApiResult.Success -> {
                        Log.d(tag, "loadDashboard success customer=${result.data.customer.id} accounts=${result.data.accounts.size}")
                        _uiState.update {
                            it.copy(
                                data = result.data,
                                errorMessage = null,
                                lastLoadedCustomerId = normalizedCustomerId ?: result.data.customer.id,
                                lastUpdatedAtEpochMs = System.currentTimeMillis(),
                                hasLoadedOnce = true
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        Log.e(tag, "loadDashboard error message=${result.message}")
                        _uiState.update { it.copy(errorMessage = result.message, hasLoadedOnce = true) }
                    }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
