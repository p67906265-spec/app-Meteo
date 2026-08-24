package com.quaderno.appmeteo.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Pagina "Radar": mostra le nuvole e le precipitazioni in tempo reale (con qualche
 * frame di previsione a breve termine) sopra una mappa OpenStreetMap, usando le tile
 * pubbliche e gratuite di RainViewer (nessuna chiave API richiesta).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RadarScreen(latitude: Double, longitude: Double, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = android.webkit.WebViewClient()

                // Inietta la posizione dell'utente prima di caricare la pagina,
                // così la mappa parte già centrata sul posto giusto.
                addJavascriptInterface(this, "AndroidBridge")

                loadDataWithBaseURL(
                    "https://appassets.androidplatform.net",
                    buildInjectedHtml(latitude, longitude),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

/**
 * Legge assets/radar.html e inietta le coordinate iniziali come variabili globali JS,
 * evitando di dover passare per un bridge JS più complesso.
 */
private fun buildInjectedHtml(latitude: Double, longitude: Double): String {
    val script = "<script>window.START_LAT=$latitude; window.START_LON=$longitude;</script>"
    return RADAR_HTML_TEMPLATE.replace("<!--INJECT-->", script)
}

// Il contenuto è lo stesso di assets/radar.html: qui viene duplicato come stringa
// per poter iniettare lat/lon senza un secondo giro di caricamento file.
// In alternativa si può caricare "file:///android_asset/radar.html" direttamente
// e passare le coordinate via evaluateJavascript dopo onPageFinished.
private const val RADAR_HTML_TEMPLATE = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<style>
  html, body, #map { height: 100%; margin: 0; padding: 0; background:#0f1b22; }
  #frame-label {
    position:absolute; bottom:14px; left:50%; transform:translateX(-50%);
    background:rgba(28,37,64,0.85); color:#fff; font-family:sans-serif;
    font-size:12px; font-weight:600; padding:6px 14px; border-radius:14px;
    z-index:1000; pointer-events:none;
  }
</style>
<!--INJECT-->
</head>
<body>
<div id="map"></div>
<div id="frame-label">Caricamento radar…</div>

<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
  var startLat = window.START_LAT || 41.9028;
  var startLon = window.START_LON || 12.4964;

  var map = L.map('map', { zoomControl: true, attributionControl: false }).setView([startLat, startLon], 7);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18
  }).addTo(map);

  L.marker([startLat, startLon]).addTo(map);

  var radarLayer = null;
  var frames = [];
  var currentFrame = 0;

  function showFrame(index) {
    if (frames.length === 0) return;
    if (radarLayer) map.removeLayer(radarLayer);
    var frame = frames[index];
    radarLayer = L.tileLayer(
      'https://tilecache.rainviewer.com' + frame.path + '/256/{z}/{x}/{y}/2/1_1.png',
      { opacity: 0.65 }
    ).addTo(map);

    var date = new Date(frame.time * 1000);
    var label = date.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' });
    document.getElementById('frame-label').innerText =
      (frame.isForecast ? 'Previsione ' : 'Radar ') + label;
  }

  fetch('https://api.rainviewer.com/public/weather-maps.json')
    .then(function(res){ return res.json(); })
    .then(function(data){
      var past = (data.radar && data.radar.past) || [];
      var nowcast = (data.radar && data.radar.nowcast) || [];
      frames = past.concat(nowcast.map(function(f){ f.isForecast = true; return f; }));
      currentFrame = past.length > 0 ? past.length - 1 : 0;
      showFrame(currentFrame);

      setInterval(function(){
        currentFrame = (currentFrame + 1) % frames.length;
        showFrame(currentFrame);
      }, 800);
    })
    .catch(function(){
      document.getElementById('frame-label').innerText = 'Radar non disponibile';
    });
</script>
</body>
</html>
"""
