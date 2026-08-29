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

    /** Cerca una città per nome usando l'API di geocoding di Open-Meteo. */
    fun searchCity(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
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

    /** Carica il meteo per una città selezionata dai risultati di ricerca. */
    fun selectCity(city: GeoResult) {
        val label = listOfNotNull(city.name, city.admin1, city.country).joinToString(", ")
        loadForecast(city.latitude, city.longitude, label)
        _uiState.update { it.copy(searchResults = emptyList()) }
    }

    /** Carica il meteo per coordinate dirette (es. posizione GPS attuale). */
    fun loadForecast(latitude: Double, longitude: Double, cityName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, cityName = cityName) }
            try {
                val forecast = NetworkModule.weatherApi.getForecast(latitude, longitude)
                _uiState.update { it.copy(isLoading = false, forecast = forecast) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Impossibile caricare il meteo: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
