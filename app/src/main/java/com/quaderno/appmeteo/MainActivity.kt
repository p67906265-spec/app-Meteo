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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.quaderno.appmeteo.data.CurrentWeather
import com.quaderno.appmeteo.data.Daily
import com.quaderno.appmeteo.data.ForecastResponse
import com.quaderno.appmeteo.data.GeoResult
import com.quaderno.appmeteo.data.WeatherCode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val BgTop = Color(0xFFD9E7FF)
private val BgMid = Color(0xFFF5F2FF)
private val BgBottom = Color(0xFFF9F7FC)
private val Ink = Color(0xFF17182A)
private val Muted = Color(0xFF6F7195)
private val Violet = Color(0xFF7762E9)
private val VioletDark = Color(0xFF5C48D7)
private val PaleCard = Color(0xD9FFFFFF)
private val Line = Color(0x1F6E7191)

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) fetchCurrentLocationWeather() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(viewModel = viewModel, onRequestLocation = { requestLocationWeather() })
                }
            }
        }
    }

    private fun requestLocationWeather() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) fetchCurrentLocationWeather()
        else locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocationWeather() {
        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.loadForecast(location.latitude, location.longitude, "Posizione attuale")
                }
            }
    }
}

@Composable
fun AppRoot(viewModel: WeatherViewModel, onRequestLocation: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgMid, BgBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> WeatherScreen(viewModel, onRequestLocation)
                    else -> RadarScreen(state.forecast?.latitude, state.forecast?.longitude)
                }
            }
            NavigationBar(
                containerColor = Color(0xEFFFFFFF),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.WbSunny, null) },
                    label = { Text("Meteo") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Radar") }
                )
            }
        }
    }
}

@Composable
fun RadarScreen(centerLat: Double? = null, centerLon: Double? = null) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pageReady, centerLat, centerLon) {
        if (pageReady && centerLat != null && centerLon != null) {
            webView?.evaluateJavascript("if(window.centerOn) centerOn($centerLat, $centerLon, 7);", null)
        }
    }

    LaunchedEffect(pageReady) {
        if (!pageReady) return@LaunchedEffect
        try {
            val response = com.quaderno.appmeteo.data.RainViewerNetwork.api.getWeatherMaps()
            val frames = response.radar.past.takeLast(8).map { frame ->
                mapOf("time" to frame.time, "url" to "${response.host}${frame.path}/256/{z}/{x}/{y}/2/1_1.png")
            }
            val escaped = org.json.JSONObject.quote(com.google.gson.Gson().toJson(frames))
            webView?.evaluateJavascript("loadFrames($escaped)", null)
        } catch (e: Exception) {
            loadError = "Radar non disponibile: ${e.message}"
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
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            pageReady = true
                            view?.postDelayed({
                                view.evaluateJavascript("if(window.invalidateMapSize) invalidateMapSize();", null)
                            }, 500)
                        }
                    }
                    loadUrl("file:///android_asset/radar.html?v=" + System.currentTimeMillis())
                    webView = this
                }
            }
        )
        loadError?.let {
            Card(modifier = Modifier.align(Alignment.TopCenter).padding(12.dp)) {
                Text(it, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
fun WeatherScreen(viewModel: WeatherViewModel, onRequestLocation: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var locationExpanded by rememberSaveable { mutableStateOf(false) }
    var requestedOnce by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!requestedOnce && state.forecast == null) {
            requestedOnce = true
            onRequestLocation()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TopHeader(
                locationLabel = state.cityName.ifBlank { "Posizione attuale" },
                onToggleLocation = { locationExpanded = !locationExpanded }
            )
        }

        item {
            AnimatedVisibility(locationExpanded) {
                LocationPanel(
                    query = query,
                    onQueryChange = {
                        query = it
                        viewModel.searchCity(it)
                    },
                    results = state.searchResults,
                    onCurrent = {
                        query = ""
                        locationExpanded = false
                        viewModel.searchCity("")
                        onRequestLocation()
                    },
                    onSelect = {
                        query = ""
                        locationExpanded = false
                        viewModel.searchCity("")
                        viewModel.selectCity(it)
                    }
                )
            }
        }

        when {
            state.isLoading && state.forecast == null -> item {
                Box(Modifier.fillMaxWidth().padding(top = 72.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Violet)
                }
            }
            state.error != null && state.forecast == null -> item {
                Text(state.error ?: "", color = Color.Red, modifier = Modifier.padding(horizontal = 20.dp))
            }
            state.forecast != null -> weatherContent(state.forecast!!)
            else -> item {
                Text(
                    "Tocca Posizione attuale per scegliere la città.",
                    color = Muted,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun TopHeader(locationLabel: String, onToggleLocation: () -> Unit) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Meteo", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x9AFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text("☰", color = Ink, fontSize = 19.sp)
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xB3FFFFFF))
                .clickable(onClick = onToggleLocation)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, null, tint = Violet, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                locationLabel,
                color = Color(0xFF4F4D7A),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF4F4D7A), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LocationPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<GeoResult>,
    onCurrent: () -> Unit,
    onSelect: (GeoResult) -> Unit
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEFFFFFFF))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, null) }
                    }
                },
                placeholder = { Text("Cerca una città…") }
            )
            OutlinedButton(onClick = onCurrent, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.LocationOn, null)
                Spacer(Modifier.width(8.dp))
                Text("Usa posizione attuale")
            }
            if (results.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    LazyColumn {
                        itemsIndexed(results) { index, result ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(result) }
                                    .padding(12.dp)
                            ) {
                                Text(result.name, fontWeight = FontWeight.SemiBold)
                                Text(listOfNotNull(result.admin1, result.country).joinToString(", "), color = Muted, fontSize = 11.sp)
                            }
                            if (index < results.lastIndex) HorizontalDivider(color = Line)
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.weatherContent(forecast: ForecastResponse) {
    forecast.current?.let { current ->
        item { HeroWeatherCard(current) }
    }

    forecast.hourly?.let { hourly ->
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Prossime ore", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Vedi tutte", color = VioletDark, fontSize = 13.sp)
                }
                Spacer(Modifier.height(7.dp))
                val now = LocalDateTime.now()
                val start = hourly.time.indexOfFirst { t -> runCatching { LocalDateTime.parse(t).isAfter(now) }.getOrDefault(false) }.let { if (it < 0) 0 else it }
                val end = minOf(hourly.time.size, start + 12)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items((start until end).toList()) { i ->
                        HourCard(hourly.time[i], hourly.temperature_2m[i], hourly.weathercode[i], i == start)
                    }
                }
            }
        }
    }

    forecast.daily?.let { daily ->
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Prossimi giorni", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(7.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaleCard)
                ) {
                    Column {
                        daily.time.indices.forEach { i ->
                            DailyForecastRow(daily, i)
                            if (i < daily.time.lastIndex) HorizontalDivider(color = Line, modifier = Modifier.padding(horizontal = 14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroWeatherCard(current: CurrentWeather) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF5E9DFF), Color(0xFF7D8CF4), Color(0xFFC493F0))
                    )
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Adesso", color = Color.White.copy(alpha = .9f), fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${current.temperature.toInt()}°",
                            color = Color.White,
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 62.sp
                        )
                        Text(
                            WeatherCode.description(current.weathercode),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = .28f))
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "💨  Vento ${current.windspeed.toInt()} km/h  •  ${windDirectionLabel(current.windDirection)}",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        WeatherCode.emoji(current.weathercode),
                        fontSize = 76.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MetricGlass("🌡️", "Percepita", "${(current.apparentTemperature ?: current.temperature).toInt()}°", Modifier.weight(1f))
                    MetricGlass("💧", "Umidità", "${current.humidity ?: 0}%", Modifier.weight(1f))
                    MetricGlass("🧭", "Pressione", "${current.pressure?.toInt() ?: 0} hPa", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricGlass(icon: String, label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = .13f))
            .padding(horizontal = 7.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 15.sp)
        Text(label, color = Color.White.copy(alpha = .88f), fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun HourCard(time: String, temp: Double, code: Int, selected: Boolean) {
    val hour = runCatching {
        val localTime = LocalTime.parse(time.substringAfter("T"))
        localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault(time)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xCC7B78EE) else Color(0xDFFFFFFF)
        )
    ) {
        Column(
            modifier = Modifier.width(72.dp).padding(vertical = 10.dp, horizontal = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(if (selected) "Adesso" else hour, color = if (selected) Color.White else Muted, fontSize = 11.sp)
            Text(WeatherCode.emoji(code), fontSize = 21.sp)
            Text("${temp.toInt()}°", color = if (selected) Color.White else Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DailyForecastRow(daily: Daily, index: Int) {
    val date = runCatching { LocalDate.parse(daily.time[index]) }.getOrNull()
    val dayName = when (index) {
        0 -> "Oggi"
        1 -> "Domani"
        else -> date?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.ITALIAN)?.replaceFirstChar { it.uppercase() } ?: daily.time[index]
    }
    val dateText = date?.format(DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)) ?: ""

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(66.dp)) {
            Text(dayName, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(dateText, color = Muted, fontSize = 10.sp)
        }
        Text(WeatherCode.emoji(daily.weathercode[index]), fontSize = 21.sp, modifier = Modifier.width(34.dp))
        Text(
            WeatherCode.description(daily.weathercode[index]),
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text("Min ${daily.temperature_2m_min[index].toInt()}°", color = Color(0xFF888BD6), fontSize = 10.sp)
        Spacer(Modifier.width(7.dp))
        Text("Max ${daily.temperature_2m_max[index].toInt()}°", color = VioletDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun windDirectionLabel(deg: Int?): String {
    if (deg == null) return "--"
    val dirs = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val index = (((deg + 22.5) / 45.0).toInt()) % 8
    return dirs[index]
}
