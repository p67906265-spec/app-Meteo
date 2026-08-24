package com.quaderno.appmeteo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quaderno.appmeteo.data.Condition
import com.quaderno.appmeteo.data.WeatherRepository
import com.quaderno.appmeteo.data.weatherCodeToCondition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HourItem(val label: String, val temp: Int, val condition: Condition)
data class DayItem(val label: String, val min: Int, val max: Int, val condition: Condition)

data class WeatherUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val currentTemp: Int = 0,
    val condition: Condition = Condition.SOLE,
    val maxToday: Int = 0,
    val minToday: Int = 0,
    val hourly: List<HourItem> = emptyList(),
    val daily: List<DayItem> = emptyList()
)

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState

    /** Chiamare con le coordinate ottenute da FusedLocationProviderClient. */
    fun load(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val response = repository.fetchForecast(latitude, longitude)

                val hourly = response.hourly.time.indices
                    .take(6)
                    .map { i ->
                        HourItem(
                            label = response.hourly.time[i].substringAfter("T"),
                            temp = response.hourly.temperature2m[i].toInt(),
                            condition = weatherCodeToCondition(response.hourly.weatherCode[i])
                        )
                    }

                val daily = response.daily.time.indices
                    .drop(1) // salta oggi, mostra i prossimi giorni
                    .take(6)
                    .map { i ->
                        DayItem(
                            label = response.daily.time[i],
                            min = response.daily.tempMin[i].toInt(),
                            max = response.daily.tempMax[i].toInt(),
                            condition = weatherCodeToCondition(response.daily.weatherCode[i])
                        )
                    }

                _uiState.value = WeatherUiState(
                    loading = false,
                    currentTemp = response.currentWeather.temperature.toInt(),
                    condition = weatherCodeToCondition(response.currentWeather.weathercode),
                    maxToday = response.daily.tempMax.firstOrNull()?.toInt() ?: 0,
                    minToday = response.daily.tempMin.firstOrNull()?.toInt() ?: 0,
                    hourly = hourly,
                    daily = daily
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }
}
