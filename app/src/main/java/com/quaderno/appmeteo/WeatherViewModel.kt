package com.quaderno.appmeteo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quaderno.appmeteo.data.ForecastResponse
import com.quaderno.appmeteo.data.GeoResult
import com.quaderno.appmeteo.data.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val cityName: String = "",
    val forecast: ForecastResponse? = null,
    val searchResults: List<GeoResult> = emptyList(),
    val isSearching: Boolean = false
)

class WeatherViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private var lastCityName: String = ""

    fun searchCity(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                val response = NetworkModule.geocodingApi.search(query)
                _uiState.update {
                    it.copy(searchResults = response.results ?: emptyList(), isSearching = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, error = "Errore nella ricerca città: ${e.message}")
                }
            }
        }
    }

    fun selectCity(city: GeoResult) {
        val label = listOfNotNull(city.name, city.admin1, city.country).joinToString(", ")
        loadForecast(city.latitude, city.longitude, label)
        _uiState.update { it.copy(searchResults = emptyList()) }
    }

    fun loadForecast(latitude: Double, longitude: Double, cityName: String) {
        lastLatitude = latitude
        lastLongitude = longitude
        lastCityName = cityName

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, cityName = cityName) }
            try {
                val forecast = NetworkModule.weatherApi.getForecast(latitude, longitude)
                _uiState.update {
                    it.copy(isLoading = false, forecast = forecast, error = null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Impossibile caricare il meteo in tempo reale: ${e.message}"
                    )
                }
            }
        }
    }

    fun refreshWeather() {
        val lat = lastLatitude
        val lon = lastLongitude
        if (lat != null && lon != null) {
            loadForecast(lat, lon, lastCityName.ifBlank { _uiState.value.cityName.ifBlank { "Posizione attuale" } })
        }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
}
