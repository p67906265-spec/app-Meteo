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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
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
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> WeatherScreen(viewModel = viewModel, onRequestLocation = onRequestLocation)
                1 -> RadarScreen(
                    centerLat = state.forecast?.latitude,
                    centerLon = state.forecast?.longitude
                )
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
fun RadarScreen(centerLat: Double? = null, centerLon: Double? = null) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Centra la mappa sulla città cercata nella scheda Meteo, se disponibile
    LaunchedEffect(pageReady, centerLat, centerLon) {
        if (!pageReady || centerLat == null || centerLon == null) return@LaunchedEffect
        webView?.evaluateJavascript("if(window.centerOn) centerOn($centerLat, $centerLon, 7);", null)
    }

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
    var positionExpanded by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Meteo",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                positionExpanded = !positionExpanded
                                if (!positionExpanded) {
                                    searchExpanded = false
                                    query = ""
                                    viewModel.searchCity("")
                                    focusManager.clearFocus()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (state.cityName.isBlank()) "Posizione attuale" else state.cityName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 175.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (positionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (positionExpanded) "Chiudi posizione" else "Apri posizione",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (positionExpanded) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                                    .clickable { searchExpanded = true }
                                    .padding(start = 12.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (state.cityName.isBlank()) "Cerca una città" else state.cityName,
                                    color = if (state.cityName.isBlank()) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                IconButton(onClick = {
                                    positionExpanded = false
                                    searchExpanded = false
                                    focusManager.clearFocus()
                                    onRequestLocation()
                                }) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Usa posizione attuale")
                                }
                            }

                            if (searchExpanded) {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = {
                                        query = it
                                        viewModel.searchCity(it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    placeholder = { Text("Scrivi una città…") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            query = ""
                                            viewModel.searchCity("")
                                            searchExpanded = false
                                            focusManager.clearFocus()
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Chiudi")
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                LaunchedEffect(searchExpanded) {
                                    if (searchExpanded) focusRequester.requestFocus()
                                }

                                if (state.searchResults.isNotEmpty()) {
                                    CitySearchDropdown(
                                        results = state.searchResults,
                                        onSelect = { result ->
                                            query = ""
                                            searchExpanded = false
                                            positionExpanded = false
                                            focusManager.clearFocus()
                                            viewModel.selectCity(result)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when {
                state.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                state.error != null -> {
                    item {
                        Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                    }
                }

                state.forecast != null -> {
                    val forecast = state.forecast!!
                    val current = forecast.currentWeather

                    current?.let { weather ->
                        item { CurrentWeatherCard(weather) }
                    }

                    forecast.hourly?.let { hourly ->
                        item { Text("Prossime ore", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) }
                        item {
                            val now = java.time.LocalDateTime.now()
                            val startIndex = hourly.time.indexOfFirst { t ->
                                runCatching { java.time.LocalDateTime.parse(t).isAfter(now) }.getOrDefault(false)
                            }.let { if (it == -1) 0 else it }
                            val endIndex = minOf(hourly.time.size, startIndex + 24)

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(endIndex - startIndex) { i ->
                                    val index = startIndex + i
                                    HourlyItem(
                                        time = hourly.time[index],
                                        temp = hourly.temperature_2m[index],
                                        code = hourly.weathercode[index]
                                    )
                                }
                            }
                        }
                    }

                    forecast.daily?.let { daily ->
                        item {
                            Text(
                                "Prossimi giorni",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(daily.time.size) { index ->
                            DailyRow(daily, index)
                            if (index < daily.time.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            }
                        }
                    }
                }

                else -> {
                    item {
                        Text(
                            "Apri Posizione attuale in alto per scegliere una città o usare il GPS.",
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                }
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
fun CurrentWeatherCard(current: com.quaderno.appmeteo.data.CurrentWeather) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                    )
                )
            )
            .padding(vertical = 15.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "${current.temperature.toInt()}°C",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    WeatherCode.description(current.weathercode),
                    fontSize = 15.sp,
                    color = Color.Gray
                )
                Text(
                    "Vento: ${current.windspeed.toInt()} km/h",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Text(WeatherCode.emoji(current.weathercode), fontSize = 48.sp)
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
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dayLabel, modifier = Modifier.width(52.dp), fontSize = 15.sp)
        Text(WeatherCode.emoji(daily.weathercode[index]), fontSize = 17.sp, modifier = Modifier.width(42.dp))
        Text(
            WeatherCode.description(daily.weathercode[index]),
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = Color.Gray,
            maxLines = 2
        )

        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Min", fontSize = 9.sp, color = Color.Gray)
            Text(
                "${daily.temperature_2m_min[index].toInt()}°",
                fontSize = 15.sp,
                color = Color.Gray
            )
        }

        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Max", fontSize = 9.sp, color = Color.Gray)
            Text(
                "${daily.temperature_2m_max[index].toInt()}°",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

