package com.quaderno.appmeteo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quaderno.appmeteo.data.Condition
import com.quaderno.appmeteo.data.label
import com.quaderno.appmeteo.ui.theme.*
import com.quaderno.appmeteo.viewmodel.DayItem
import com.quaderno.appmeteo.viewmodel.HourItem
import com.quaderno.appmeteo.viewmodel.WeatherUiState

@Composable
fun WeatherScreen(state: WeatherUiState, locationLabel: String = "Posizione attuale") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyBgTop, SkyBgBottom)))
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        TopBar(locationLabel)

        Spacer(Modifier.height(8.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WeatherIcon(condition = state.condition, size = 130.dp)
        }

        Spacer(Modifier.height(4.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${state.currentTemp}°",
                fontSize = 76.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(state.condition.label(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Max ${state.maxToday}°  ·  Min ${state.minToday}°",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextFaint
            )
        }

        Spacer(Modifier.height(22.dp))

        HourlyRow(state.hourly)

        Spacer(Modifier.height(18.dp))

        GraphCard(state.hourly)

        Spacer(Modifier.height(22.dp))

        Text("Prossimi giorni", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Column {
            state.daily.forEach { day -> DayRow(day) }
        }
    }
}

@Composable
private fun TopBar(locationLabel: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NavyDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Text(locationLabel, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
    }
}

@Composable
private fun HourlyRow(hourly: List<HourItem>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(hourly) { hour ->
            Column(
                modifier = Modifier
                    .width(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(hour.label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextFaint)
                Spacer(Modifier.height(6.dp))
                WeatherIconSmall(hour.condition, size = 28.dp)
                Spacer(Modifier.height(6.dp))
                Text("${hour.temp}°", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun GraphCard(hourly: List<HourItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NavyDark)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("PREVISIONI 24 ORE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyMuted)
            Text("°C", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyMuted)
        }
        Spacer(Modifier.height(10.dp))
        TemperatureLineChart(hourly.map { it.temp }, modifier = Modifier.fillMaxWidth().height(56.dp))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            hourly.firstOrNull()?.let { Text("${it.temp}°", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            hourly.maxByOrNull { it.temp }?.let { Text("${it.temp}°", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            hourly.minByOrNull { it.temp }?.let { Text("${it.temp}°", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        }
    }
}

@Composable
private fun TemperatureLineChart(values: List<Int>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min().toFloat()
        val max = values.max().toFloat()
        val range = (max - min).coerceAtLeast(1f)
        val stepX = size.width / (values.size - 1)

        val points = values.mapIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - min) / range) * size.height
            androidx.compose.ui.geometry.Offset(x, y)
        }

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }

        drawPath(
            path = path,
            color = GraphLine,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        points.forEach { drawCircle(GraphLine, radius = 6f, center = it) }
    }
}

@Composable
private fun DayRow(day: DayItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(day.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.width(44.dp))
        WeatherIconSmall(day.condition, size = 26.dp, modifier = Modifier.padding(horizontal = 12.dp))
        Text(day.condition.label(), fontSize = 13.sp, color = TextFaint, modifier = Modifier.weight(1f))
        Text("${day.min}°", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CloudMuted, modifier = Modifier.width(30.dp))
        Text("${day.max}°", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, modifier = Modifier.width(34.dp))
    }
}
