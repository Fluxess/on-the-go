package ru.routealert.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TripUiState(
    val isActive: Boolean = false,
    val enabledPointCount: Int = 0,
    val nearestName: String? = null,
    val nearestDistanceMeters: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null
)

object TripStateHolder {
    private val _state = MutableStateFlow(TripUiState())
    val state: StateFlow<TripUiState> = _state.asStateFlow()

    fun update(transform: (TripUiState) -> TripUiState) {
        _state.value = transform(_state.value)
    }

    fun setInactive() {
        _state.value = TripUiState(isActive = false)
    }
}
