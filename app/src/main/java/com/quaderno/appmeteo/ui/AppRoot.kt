package com.quaderno.appmeteo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quaderno.appmeteo.ui.theme.NavyDark
import com.quaderno.appmeteo.ui.theme.SkyBgTop
import com.quaderno.appmeteo.ui.theme.TextFaint
import com.quaderno.appmeteo.viewmodel.WeatherUiState

private enum class Tab { METEO, RADAR }

/**
 * Contenitore principale con le due schede in basso, come nello screenshot
 * originale dell'app ("Meteo" e "Radar").
 */
@Composable
fun AppRoot(state: WeatherUiState, latitude: Double, longitude: Double) {
    var selected by remember { mutableStateOf(Tab.METEO) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (selected) {
                Tab.METEO -> WeatherScreen(state = state)
                Tab.RADAR -> RadarScreen(latitude = latitude, longitude = longitude)
            }
        }
        BottomBar(selected = selected, onSelect = { selected = it })
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkyBgTop)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomItem(
            label = "Meteo",
            icon = Icons.Default.WbSunny,
            active = selected == Tab.METEO,
            onClick = { onSelect(Tab.METEO) }
        )
        BottomItem(
            label = "Radar",
            icon = Icons.Default.Map,
            active = selected == Tab.RADAR,
            onClick = { onSelect(Tab.RADAR) }
        )
    }
}

@Composable
private fun BottomItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (active) Color(0xFFE3E8F7) else Color.Transparent)
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .clickable(indication = null, interactionSource = interactionSource, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (active) NavyDark else TextFaint)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) NavyDark else TextFaint
        )
    }
}
