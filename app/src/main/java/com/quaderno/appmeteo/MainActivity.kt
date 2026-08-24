package com.quaderno.appmeteo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quaderno.appmeteo.ui.AppRoot
import com.quaderno.appmeteo.ui.theme.MeteoAppTheme
import com.quaderno.appmeteo.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    // Coordinate di riserva (usate se l'utente nega il permesso di posizione)
    private val fallbackLat = 41.9028
    private val fallbackLon = 12.4964

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: WeatherViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()

            var currentLat by androidx.compose.runtime.remember { mutableStateOf(fallbackLat) }
            var currentLon by androidx.compose.runtime.remember { mutableStateOf(fallbackLon) }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    fetchLocationAndLoad(viewModel) { lat, lon ->
                        currentLat = lat
                        currentLon = lon
                    }
                } else {
                    viewModel.load(fallbackLat, fallbackLon)
                    currentLat = fallbackLat
                    currentLon = fallbackLon
                }
            }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    fetchLocationAndLoad(viewModel) { lat, lon -> currentLat = lat; currentLon = lon }
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            MeteoAppTheme {
                AppRoot(state = state, latitude = currentLat, longitude = currentLon)
            }
        }
    }

    private fun fetchLocationAndLoad(viewModel: WeatherViewModel, onLocation: (Double, Double) -> Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            viewModel.load(fallbackLat, fallbackLon)
            onLocation(fallbackLat, fallbackLon)
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(this)
        client.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.load(location.latitude, location.longitude)
                    onLocation(location.latitude, location.longitude)
                } else {
                    viewModel.load(fallbackLat, fallbackLon)
                    onLocation(fallbackLat, fallbackLon)
                }
            }
            .addOnFailureListener {
                viewModel.load(fallbackLat, fallbackLon)
                onLocation(fallbackLat, fallbackLon)
            }
    }
}
