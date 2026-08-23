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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.quaderno.appmeteo.data.CurrentSnapshot
import com.quaderno.appmeteo.data.Daily
import com.quaderno.appmeteo.data.ForecastResponse
import com.quaderno.appmeteo.data.GeoResult
import com.quaderno.appmeteo.data.Hourly
import com.quaderno.appmeteo.data.WeatherCode
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
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
            } else {
                viewModel.setError("Posizione non disponibile. Attiva il GPS oppure cerca una città.")
            }
        }.addOnFailureListener {
            viewModel.setError("Impossibile ottenere la posizione attuale.")
        }
    }
}

@Composable
fun AppRoot(viewModel: WeatherViewModel, onRequestLocation: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> WeatherScreen(
                    viewModel = viewModel,
                    onRequestLocation = onRequestLocation,
                    onRefresh = { viewModel.refreshWeather() }
                )
                1 -> RadarScreen()
            }
        }
        NavigationBar(containerColor = Color(0xE514253B)) {
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
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.setSupportZoom(true)
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    }
                    loadUrl("https://www.rainviewer.com/map.html")
                }
            }
        )
    }
}

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    onRequestLocation: () -> Unit,
    onRefresh: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var showLocationPanel by remember { mutableStateOf(false) }
    var startupLoadDone by remember { mutableStateOf(false) }

    val forecast = state.forecast
    val current = remember(forecast) { forecast?.toCurrentSnapshot() }

    LaunchedEffect(Unit) {
        if (!startupLoadDone && forecast == null) {
            startupLoadDone = true
            onRequestLocation()
        }
    }

    LaunchedEffect(state.cityName, forecast?.current?.time) {
        if (forecast != null) {
            while (true) {
                delay(300_000)
                onRefresh()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedWeatherBackground(current = current)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeaderBar(
                    cityName = state.cityName.ifBlank { "Meteo" },
                    onOpenPanel = { showLocationPanel = !showLocationPanel },
                    onRefresh = onRefresh
                )
            }

            item {
                AnimatedVisibility(visible = showLocationPanel) {
                    LocationPanel(
                        query = query,
                        onQueryChange = {
                            query = it
                            viewModel.searchCity(it)
                        },
                        searchResults = state.searchResults,
                        onUseCurrentLocation = {
                            showLocationPanel = false
                            onRequestLocation()
                        },
                        onSelectCity = { city ->
                            query = city.name
                            showLocationPanel = false
                            viewModel.selectCity(city)
                        }
                    )
                }
            }

            when {
                state.isLoading && forecast == null -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
                state.error != null && forecast == null -> {
                    item {
                        GlassCard {
                            Text(state.error ?: "Errore", color = Color.White)
                        }
                    }
                }
                forecast != null && current != null -> {
                    item {
                        HeroWeatherCard(
                            cityName = state.cityName,
                            current = current,
                            isRefreshing = state.isLoading
                        )
                    }

                    item {
                        SectionTitle("Prossime ore")
                    }

                    item {
                        HourlyForecastRow(hourly = forecast.hourly, currentTime = current.time)
                    }

                    item {
                        SectionTitle("Prossimi giorni")
                    }

                    forecast.daily?.let { daily ->
                        items(daily.time.size) { index ->
                            DailyForecastRow(daily = daily, index = index)
                        }
                    }
                }
                else -> {
                    item {
                        GlassCard {
                            Text(
                                text = "Apri il pannello in alto per cercare una città o usare la posizione attuale.",
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (state.error != null && forecast != null) {
                item {
                    GlassCard {
                        Text(state.error ?: "", color = Color(0xFFFFE082))
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderBar(cityName: String, onOpenPanel: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onOpenPanel) {
            Icon(Icons.Default.Settings, contentDescription = "Impostazioni", tint = Color.White)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = cityName.ifBlank { "Meteo" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                text = "Aggiornamento automatico ogni 5 minuti",
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp
            )
        }

        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Aggiorna", tint = Color.White)
        }
    }
}

@Composable
fun LocationPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<GeoResult>,
    onUseCurrentLocation: () -> Unit,
    onSelectCity: (GeoResult) -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Posizione e ricerca", color = Color.White, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Cerca una città…") }
            )
            FilledTonalButton(onClick = onUseCurrentLocation) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Usa posizione attuale")
            }
            if (searchResults.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    searchResults.forEach { result ->
                        SearchResultChip(result = result, onClick = { onSelectCity(result) })
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultChip(result: GeoResult, onClick: () -> Unit) {
    val subtitle = listOfNotNull(result.admin1, result.country).joinToString(", ")
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(result.name, color = Color.White, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = Color(0xCCFFFFFF), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HeroWeatherCard(cityName: String, current: CurrentSnapshot, isRefreshing: Boolean) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cityName.ifBlank { "Posizione attuale" },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Aggiornato ${formatDateTimeLabel(current.time)}",
                        color = Color(0xCCFFFFFF),
                        fontSize = 12.sp
                    )
                }
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${current.temperature.toInt()}°",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.sp
                    )
                    Text(
                        text = WeatherCode.description(current.weatherCode),
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
                Text(
                    text = WeatherCode.emoji(current.weatherCode),
                    fontSize = 64.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeatherMetricChip(
                    label = "Percepita",
                    value = "${current.apparentTemperature?.toInt() ?: current.temperature.toInt()}°",
                    modifier = Modifier.weight(1f)
                )
                WeatherMetricChip(
                    label = "Vento",
                    value = "${current.windSpeed.toInt()} km/h",
                    modifier = Modifier.weight(1f)
                )
                WeatherMetricChip(
                    label = "Umidità",
                    value = current.relativeHumidity?.let { "$it%" } ?: "--",
                    modifier = Modifier.weight(1f)
                )
            }

            current.precipitation?.let {
                Text(
                    text = if (it > 0) "Precipitazioni in corso: ${"%.1f".format(Locale.US, it)} mm" else "Nessuna precipitazione in corso",
                    color = Color(0xD9FFFFFF),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun WeatherMetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = Color(0xD9FFFFFF),
                fontSize = 11.sp,
                maxLines = 1
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun HourlyForecastRow(hourly: Hourly?, currentTime: LocalDateTime?) {
    if (hourly == null || hourly.time.isEmpty()) return

    val startIndex = remember(hourly.time, currentTime) {
        if (currentTime == null) 0
        else hourly.time.indexOfFirst { value ->
            runCatching { LocalDateTime.parse(value).isAfter(currentTime) }.getOrDefault(false)
        }.takeIf { it >= 0 } ?: 0
    }
    val indexes = remember(hourly.time, startIndex) {
        val endExclusive = minOf(startIndex + 18, hourly.time.size)
        (startIndex until endExclusive).toList()
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(indexes) { index ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(78.dp)
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatHourLabel(hourly.time[index]),
                        color = Color(0xD9FFFFFF),
                        fontSize = 12.sp
                    )
                    Text(WeatherCode.emoji(hourly.weatherCode[index]), fontSize = 22.sp)
                    Text(
                        text = "${hourly.temperature_2m[index].toInt()}°",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 21.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DailyForecastRow(daily: Daily, index: Int) {
    val dayLabel = runCatching {
        LocalDate.parse(daily.time[index])
            .dayOfWeek
            .getDisplayName(TextStyle.SHORT, Locale.ITALIAN)
            .replaceFirstChar { it.uppercase() }
    }.getOrDefault(daily.time[index])

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(dayLabel, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(42.dp))
            Text(WeatherCode.emoji(daily.weatherCode[index]), fontSize = 24.sp, modifier = Modifier.width(32.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(WeatherCode.description(daily.weatherCode[index]), color = Color.White, fontSize = 16.sp)
                val rainChance = daily.precipitation_probability_max?.getOrNull(index)
                if (rainChance != null) {
                    Text("Pioggia $rainChance%", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                }
            }
            Text("${daily.temperature_2m_min[index].toInt()}°", color = Color(0xCCFFFFFF), fontSize = 18.sp)
            Text("${daily.temperature_2m_max[index].toInt()}°", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
}

@Composable
fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x1FFFFFFF)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun AnimatedWeatherBackground(current: CurrentSnapshot?) {
    val scene = current?.let { WeatherCode.scene(it.weatherCode) } ?: WeatherScene.SUN
    val isDay = current?.isDay != 0
    val gradient = when (scene) {
        WeatherScene.SUN -> listOf(Color(0xFF0C3B60), Color(0xFF1B6EA9), Color(0xFF6EC6FF))
        WeatherScene.CLOUDS -> listOf(Color(0xFF294861), Color(0xFF54728C), Color(0xFF8AA8BE))
        WeatherScene.RAIN -> listOf(Color(0xFF142637), Color(0xFF1F4C68), Color(0xFF356D8A))
        WeatherScene.STORM -> listOf(Color(0xFF10131E), Color(0xFF26374C), Color(0xFF4B5475))
        WeatherScene.SNOW -> listOf(Color(0xFF385B72), Color(0xFF7090AA), Color(0xFFB9D1E3))
        WeatherScene.FOG -> listOf(Color(0xFF4C5963), Color(0xFF6C7A85), Color(0xFF9FAAB4))
    }

    val transition = rememberInfiniteTransition(label = "weather")
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
        label = "pulse"
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradient))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val sunCenter = Offset(w * 0.82f, h * 0.18f)

            if (!isDay) {
                drawCircle(
                    color = Color(0x33FFFFFF),
                    radius = w * 0.11f,
                    center = Offset(w * 0.82f, h * 0.18f)
                )
            }

            when (scene) {
                WeatherScene.SUN -> {
                    drawCircle(Color(0x33FFF59D), radius = w * 0.24f * pulse, center = sunCenter)
                    drawCircle(Color(0xFFFFD54F), radius = w * 0.10f * pulse, center = sunCenter)
                    repeat(12) { index ->
                        val angle = ((index / 12f) + drift) * (Math.PI * 2.0)
                        val start = Offset(
                            x = sunCenter.x + kotlin.math.cos(angle).toFloat() * w * 0.14f,
                            y = sunCenter.y + kotlin.math.sin(angle).toFloat() * w * 0.14f
                        )
                        val end = Offset(
                            x = sunCenter.x + kotlin.math.cos(angle).toFloat() * w * 0.18f,
                            y = sunCenter.y + kotlin.math.sin(angle).toFloat() * w * 0.18f
                        )
                        drawLine(Color(0xFFFFE082), start = start, end = end, strokeWidth = 8f, cap = StrokeCap.Round)
                    }
                }
                WeatherScene.CLOUDS, WeatherScene.FOG -> {
                    drawCloudBand(drift, h, w)
                }
                WeatherScene.RAIN -> {
                    drawCloudBand(drift, h, w)
                    repeat(80) { index ->
                        val x = (index * 37f % w)
                        val offset = ((drift * h * 1.4f) + index * 22f) % (h + 200f)
                        drawLine(
                            color = Color(0x66E1F5FE),
                            start = Offset(x, offset - 28f),
                            end = Offset(x - 10f, offset),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                WeatherScene.STORM -> {
                    drawCloudBand(drift, h, w)
                    repeat(95) { index ->
                        val x = (index * 31f % w)
                        val offset = ((drift * h * 1.7f) + index * 18f) % (h + 200f)
                        drawLine(
                            color = Color(0x88E1F5FE),
                            start = Offset(x, offset - 34f),
                            end = Offset(x - 12f, offset),
                            strokeWidth = 4.5f,
                            cap = StrokeCap.Round
                        )
                    }
                    drawLine(
                        color = Color(0x99FFF59D),
                        start = Offset(w * 0.70f, h * 0.12f),
                        end = Offset(w * 0.60f, h * 0.28f),
                        strokeWidth = 12f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0x99FFF59D),
                        start = Offset(w * 0.60f, h * 0.28f),
                        end = Offset(w * 0.66f, h * 0.30f),
                        strokeWidth = 12f,
                        cap = StrokeCap.Round
                    )
                }
                WeatherScene.SNOW -> {
                    repeat(50) { index ->
                        val x = (index * 49f % w)
                        val y = ((drift * h) + index * 34f) % (h + 120f)
                        drawCircle(Color(0xB3FFFFFF), radius = 4f + (index % 3), center = Offset(x, y))
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudBand(drift: Float, h: Float, w: Float) {
    val baseY = h * 0.16f
    repeat(5) { index ->
        val x = ((index * 180f) + drift * 220f) % (w + 220f) - 110f
        drawCircle(Color(0x18FFFFFF), radius = 90f, center = Offset(x, baseY))
        drawCircle(Color(0x22FFFFFF), radius = 70f, center = Offset(x + 60f, baseY + 10f))
        drawCircle(Color(0x16FFFFFF), radius = 60f, center = Offset(x - 55f, baseY + 18f))
    }
}

enum class WeatherScene { SUN, CLOUDS, RAIN, STORM, SNOW, FOG }

fun ForecastResponse.toCurrentSnapshot(): CurrentSnapshot? {
    current?.let { return it }
    return currentWeather?.toSnapshot()
}

fun formatHourLabel(value: String): String = runCatching {
    LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault(value)

fun formatDateTimeLabel(value: LocalDateTime?): String {
    if (value == null) return "--:--"
    return value.format(DateTimeFormatter.ofPattern("HH:mm"))
}
