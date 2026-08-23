package com.quaderno.appmeteo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.quaderno.appmeteo.data.Daily
import com.quaderno.appmeteo.data.GeoResult
import com.quaderno.appmeteo.data.WeatherCode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchCurrentLocationWeather()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(
                        viewModel = viewModel,
                        onRequestLocation = { requestLocationWeather() }
                    )
                }
            }
        }
    }

    private fun requestLocationWeather() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            fetchCurrentLocationWeather()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocationWeather() {
        val client = LocationServices.getFusedLocationProviderClient(this)
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                viewModel.loadForecast(location.latitude, location.longitude, "Posizione attuale")
            }
        }
    }
}

@Composable
fun AppRoot(viewModel: WeatherViewModel, onRequestLocation: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> WeatherScreen(viewModel = viewModel, onRequestLocation = onRequestLocation)
                1 -> RadarScreen()
            }
        }
        NavigationBar {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = { Icon(Icons.Default.WbSunny, contentDescription = null) },
                label = { Text("Meteo") }
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Default.Map, contentDescription = null) },
                label = { Text("Radar") }
            )
        }
    }
}

@Composable
fun RadarScreen() {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Scarica i frame radar in Kotlin (evita problemi di fetch/CORS dentro la WebView)
    LaunchedEffect(pageReady) {
        if (!pageReady) return@LaunchedEffect
        try {
            val response = com.quaderno.appmeteo.data.RainViewerNetwork.api.getWeatherMaps()
            val frames = response.radar.past.takeLast(8).map { frame ->
                val url = "${response.host}${frame.path}/256/{z}/{x}/{y}/2/1_1.png"
                mapOf("time" to frame.time, "url" to url)
            }
            val json = com.google.gson.Gson().toJson(frames)
            // Passiamo la stringa JSON già "escaped" per l'uso dentro apici singoli JS
            val escaped = org.json.JSONObject.quote(json)
            webView?.evaluateJavascript("loadFrames($escaped)", null)
        } catch (e: Exception) {
            loadError = "Impossibile scaricare i dati radar: ${e.message}"
            val escaped = org.json.JSONObject.quote("Radar non disponibile: ${e.message}")
            webView?.evaluateJavascript("showError($escaped)", null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                    clearCache(true)
                    clearHistory()
                    setBackgroundColor(android.graphics.Color.WHITE)
                    WebView.setWebContentsDebuggingEnabled(true)
                    webChromeClient = android.webkit.WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            pageReady = true
                            // Ricalcola le dimensioni della mappa più volte: la WebView dentro
                            // Compose spesso non ha ancora le dimensioni definitive a questo punto
                            view?.postDelayed({
                                view.evaluateJavascript("if(window.invalidateMapSize) invalidateMapSize();", null)
                            }, 400)
                            view?.postDelayed({
                                view.evaluateJavascript("if(window.invalidateMapSize) invalidateMapSize();", null)
                            }, 1200)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            loadError = "Errore caricamento pagina: $description"
                        }
                    }
                    loadUrl("file:///android_asset/radar.html?v=" + System.currentTimeMillis())
                    webView = this
                    // Quando Compose assegna finalmente la dimensione reale alla WebView,
                    // forziamo Leaflet a ricalcolare l'area visibile
                    addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                        if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                            (v as WebView).evaluateJavascript(
                                "if(window.invalidateMapSize) invalidateMapSize();", null
                            )
                        }
                    }
                }
            }
        )

        loadError?.let { message ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
            ) {
                Text(message, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel, onRequestLocation: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Meteo",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Barra di ricerca
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.searchCity(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cerca una città…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = onRequestLocation) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Usa posizione attuale")
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            // Tendina risultati: fluttua sopra il contenuto sottostante invece di spingerlo giù
            androidx.compose.animation.AnimatedVisibility(
                visible = state.searchResults.isNotEmpty(),
                modifier = Modifier
                    .padding(top = 60.dp)
                    .zIndex(10f),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                CitySearchDropdown(
                    results = state.searchResults,
                    onSelect = { result ->
                        query = ""
                        viewModel.selectCity(result)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
            }
            state.forecast != null -> {
                WeatherContent(cityName = state.cityName, forecast = state.forecast!!)
            }
            else -> {
                Text(
                    "Cerca una città o usa la tua posizione attuale per vedere il meteo.",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        }
    }
}

@Composable
fun CitySearchDropdown(results: List<GeoResult>, onSelect: (GeoResult) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn {
            itemsIndexed(results) { index, result ->
                CityResultRow(result) { onSelect(result) }
                if (index < results.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun CityResultRow(result: GeoResult, onClick: () -> Unit) {
    val subtitle = listOfNotNull(result.admin1, result.country).joinToString(", ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (subtitle.isNotBlank()) {
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun WeatherContent(cityName: String, forecast: com.quaderno.appmeteo.data.ForecastResponse) {
    val current = forecast.currentWeather

    Column {
        if (cityName.isNotBlank()) {
            Text(cityName, fontSize = 18.sp, color = Color.Gray)
        }

        // Meteo attuale
        current?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                            )
                        )
                    )
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(WeatherCode.emoji(it.weathercode), fontSize = 64.sp)
                    Text(
                        "${it.temperature.toInt()}°C",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(WeatherCode.description(it.weathercode), fontSize = 16.sp, color = Color.Gray)
                    Text("Vento: ${it.windspeed.toInt()} km/h", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }

        // Previsioni orarie (prossime 24h)
        forecast.hourly?.let { hourly ->
            Text("Prossime ore", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val count = minOf(24, hourly.time.size)
                items(count) { index ->
                    HourlyItem(
                        time = hourly.time[index],
                        temp = hourly.temperature_2m[index],
                        code = hourly.weathercode[index]
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Previsioni giornaliere
        forecast.daily?.let { daily ->
            Text("Prossimi giorni", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn {
                items(daily.time.size) { index ->
                    DailyRow(daily, index)
                }
            }
        }
    }
}

@Composable
fun HourlyItem(time: String, temp: Double, code: Int) {
    val hour = runCatching {
        LocalTime.parse(time.substringAfter("T")).format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault(time)

    Card(
        modifier = Modifier
            .width(72.dp)
            .background(Color.Transparent, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(hour, fontSize = 12.sp, color = Color.Gray)
            Text(WeatherCode.emoji(code), fontSize = 22.sp)
            Text("${temp.toInt()}°", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun DailyRow(daily: Daily, index: Int) {
    val dayLabel = runCatching {
        LocalDate.parse(daily.time[index])
            .dayOfWeek
            .getDisplayName(TextStyle.SHORT, Locale.ITALIAN)
            .replaceFirstChar { it.uppercase() }
    }.getOrDefault(daily.time[index])

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dayLabel, modifier = Modifier.width(56.dp))
        Text(WeatherCode.emoji(daily.weathercode[index]), fontSize = 20.sp, modifier = Modifier.width(40.dp))
        Text(
            WeatherCode.description(daily.weathercode[index]),
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = Color.Gray
        )
        Text("${daily.temperature_2m_min[index].toInt()}°", color = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text("${daily.temperature_2m_max[index].toInt()}°", fontWeight = FontWeight.Medium)
    }
}
