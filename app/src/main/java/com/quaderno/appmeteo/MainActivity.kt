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
import androidx.compose.foundation.layout.weight
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

private val ScreenBg = Color(0xFFF7F4FA)
private val CardBg = Color(0xFFF0EAF5)
private val CardBgSoft = Color(0xFFF3EDF7)
private val Purple = Color(0xFF7A57A9)
private val TextDark = Color(0xFF1F1B24)
private val TextSoft = Color(0xFF8A8490)
private val DividerSoft = Color(0xFFD9D2DF)

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
                Surface(modifier = Modifier.fillMaxSize(), color = ScreenBg) {
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
            } else {
                viewModel.clearError()
            }
        }.addOnFailureListener {
            viewModel.clearError()
        }
    }
}

@Composable
fun AppRoot(viewModel: WeatherViewModel, onRequestLocation: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> WeatherScreen(viewModel = viewModel, onRequestLocation = onRequestLocation)
                1 -> RadarScreen(centerLat = state.forecast?.latitude, centerLon = state.forecast?.longitude)
            }
        }
        NavigationBar(containerColor = Color(0xFFF1ECF5)) {
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

    LaunchedEffect(pageReady, centerLat, centerLon) {
        if (!pageReady || centerLat == null || centerLon == null) return@LaunchedEffect
        webView?.evaluateJavascript("if(window.centerOn) centerOn($centerLat, $centerLon, 7);", null)
    }

    LaunchedEffect(pageReady) {
        if (!pageReady) return@LaunchedEffect
        try {
            val response = com.quaderno.appmeteo.data.RainViewerNetwork.api.getWeatherMaps()
            val frames = response.radar.past.takeLast(8).map { frame ->
                val url = "${response.host}${frame.path}/256/{z}/{x}/{y}/2/1_1.png"
                mapOf("time" to frame.time, "url" to url)
            }
            val json = com.google.gson.Gson().toJson(frames)
            val escaped = org.json.JSONObject.quote(json)
            webView?.evaluateJavascript("loadFrames($escaped)", null)
        } catch (e: Exception) {
            loadError = "Radar non disponibile: ${e.message}"
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
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            pageReady = true
                            view?.postDelayed({
                                view.evaluateJavascript("if(window.invalidateMapSize) invalidateMapSize();", null)
                            }, 600)
                        }
                    }
                    loadUrl("file:///android_asset/radar.html?v=" + System.currentTimeMillis())
                    webView = this
                }
            }
        )

        loadError?.let { message ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(message, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun WeatherScreen(viewModel: WeatherViewModel, onRequestLocation: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var panelExpanded by rememberSaveable { mutableStateOf(false) }
    var requestedOnce by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!requestedOnce && state.forecast == null) {
            requestedOnce = true
            onRequestLocation()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                HeaderRow(
                    locationLabel = state.cityName.ifBlank { "Posizione attuale" },
                    expanded = panelExpanded,
                    onClick = { panelExpanded = !panelExpanded }
                )
            }
        }

        item {
            AnimatedVisibility(visible = panelExpanded) {
                SearchPanel(
                    query = query,
                    onQueryChange = {
                        query = it
                        viewModel.searchCity(it)
                    },
                    searchResults = state.searchResults,
                    onUseCurrentLocation = {
                        panelExpanded = false
                        query = ""
                        viewModel.searchCity("")
                        onRequestLocation()
                    },
                    onSelectCity = { city ->
                        panelExpanded = false
                        query = ""
                        viewModel.searchCity("")
                        viewModel.selectCity(city)
                    }
                )
            }
        }

        when {
            state.isLoading && state.forecast == null -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Purple)
                    }
                }
            }
            state.error != null && state.forecast == null -> {
                item {
                    Text(
                        text = state.error ?: "",
                        color = Color.Red,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            state.forecast != null -> {
                weatherItems(state.forecast!!)
            }
            else -> {
                item {
                    Text(
                        text = "Tocca Posizione attuale per usare il GPS o cercare una città.",
                        color = TextSoft,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.weatherItems(forecast: ForecastResponse) {
    val current = forecast.currentWeather
    if (current != null) {
        item {
            CurrentWeatherCard(current = current)
        }
    }

    forecast.hourly?.let { hourly ->
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Prossime ore",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                val now = LocalDateTime.now()
                val startIndex = hourly.time.indexOfFirst { t ->
                    runCatching { LocalDateTime.parse(t).isAfter(now) }.getOrDefault(false)
                }.let { if (it == -1) 0 else it }
                val endIndex = minOf(hourly.time.size, startIndex + 12)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items((startIndex until endIndex).toList()) { index ->
                        HourlyItem(
                            time = hourly.time[index],
                            temp = hourly.temperature_2m[index],
                            code = hourly.weathercode[index]
                        )
                    }
                }
            }
        }
    }

    forecast.daily?.let { daily ->
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Prossimi giorni",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                ) {
                    daily.time.indices.forEach { index ->
                        DailyRow(daily = daily, index = index)
                        if (index < daily.time.lastIndex) {
                            HorizontalDivider(color = DividerSoft, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderRow(locationLabel: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Meteo",
            color = TextDark,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 2.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = locationLabel,
                color = TextDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextDark,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun SearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<GeoResult>,
    onUseCurrentLocation: () -> Unit,
    onSelectCity: (GeoResult) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBgSoft)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    placeholder = { Text("Cerca una città…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    }
                )
                OutlinedButton(onClick = onUseCurrentLocation, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Usa posizione attuale")
                }
                if (searchResults.isNotEmpty()) {
                    CitySearchDropdown(results = searchResults, onSelect = onSelectCity)
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        LazyColumn {
            itemsIndexed(results) { index, result ->
                CityResultRow(result = result, onClick = { onSelect(result) })
                if (index < results.lastIndex) {
                    HorizontalDivider(color = DividerSoft, modifier = Modifier.padding(horizontal = 14.dp))
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
                .size(36.dp)
                .clip(CircleShape)
                .background(Purple.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Purple)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(result.name, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = TextSoft, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CurrentWeatherCard(current: com.quaderno.appmeteo.data.CurrentWeather) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(CardBg, CardBgSoft)
                    )
                )
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${current.temperature.toInt()}°C",
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 54.sp,
                    lineHeight = 56.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = WeatherCode.description(current.weathercode),
                    color = TextSoft,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vento: ${current.windspeed.toInt()} km/h",
                    color = TextSoft,
                    fontSize = 17.sp
                )
            }
            Text(
                text = WeatherCode.emoji(current.weathercode),
                fontSize = 72.sp,
                modifier = Modifier.padding(start = 10.dp, end = 4.dp)
            )
        }
    }
}

@Composable
fun HourlyItem(time: String, temp: Double, code: Int) {
    val hour = runCatching {
        val parsed = if (time.contains("T")) LocalTime.parse(time.substringAfter("T")) else LocalTime.parse(time)
        parsed.format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault(time)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier
                .width(86.dp)
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(hour, fontSize = 12.sp, color = TextSoft)
            Text(WeatherCode.emoji(code), fontSize = 24.sp)
            Text("${temp.toInt()}°", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = TextDark)
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
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel,
            color = TextDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(54.dp)
        )
        Text(
            text = WeatherCode.emoji(daily.weathercode[index]),
            fontSize = 22.sp,
            modifier = Modifier.width(38.dp)
        )
        Text(
            text = WeatherCode.description(daily.weathercode[index]),
            color = TextSoft,
            fontSize = 17.sp,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        MinMaxColumn(label = "Min", value = "${daily.temperature_2m_min[index].toInt()}°")
        Spacer(modifier = Modifier.width(16.dp))
        MinMaxColumn(label = "Max", value = "${daily.temperature_2m_max[index].toInt()}°", bold = true)
    }
}

@Composable
fun MinMaxColumn(label: String, value: String, bold: Boolean = false) {
    Column(horizontalAlignment = Alignment.End) {
        Text(text = label, color = TextSoft, fontSize = 12.sp)
        Text(
            text = value,
            color = TextDark,
            fontSize = 18.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}
