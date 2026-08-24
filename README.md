# Meteo — app Android (Jetpack Compose)

Progetto base con la grafica "puffy" che hai scelto: icone morbide con
bagliore (sole/nuvola disegnati a runtime, nessuna immagine esterna),
temperatura enorme, riga ore scorrevole, card scura con grafico a linea
per le 24 ore e lista dei prossimi giorni.

## Come aprirlo
1. Apri Android Studio → **Open** → seleziona questa cartella (`MeteoApp`).
2. Lascia che Gradle sincronizzi (scarica le dipendenze la prima volta).
3. Esegui su un dispositivo/emulatore con Android 8.0+ (minSdk 26).

## Cosa contiene
- `data/` — modelli e chiamata all'API **Open-Meteo** (gratuita, senza
  chiave), con conversione dei codici meteo WMO in condizioni leggibili
  in italiano (Sereno, Coperto, Pioggia, Temporale, Neve, Nebbia...).
- `viewmodel/WeatherViewModel.kt` — recupera meteo attuale, previsioni
  orarie e giornaliere.
- `ui/WeatherIcons.kt` — le icone "puffy" (sole con bagliore radiale,
  nuvola con gradiente, pioggia, temporale, neve) disegnate con
  `Canvas` di Compose: si adattano a qualunque dimensione/densità senza
  bisogno di file immagine.
- `ui/WeatherScreen.kt` — la schermata principale, fedele al mockup
  approvato.
- `ui/RadarScreen.kt` — la pagina **Radar**: mappa OpenStreetMap con le
  nuvole/precipitazioni in tempo reale sopra, usando le tile pubbliche
  e gratuite di **RainViewer** (nessuna chiave API richiesta). Include
  anche qualche frame di previsione a brevissimo termine ("nowcast") e
  un'animazione automatica tra gli ultimi frame, proprio come i radar
  meteo classici.
- `ui/AppRoot.kt` — contenitore con la barra in basso **Meteo / Radar**,
  come nello screenshot originale della tua app.
- `MainActivity.kt` — richiede il permesso di posizione, carica il
  meteo e passa le coordinate sia alla schermata Meteo sia al Radar
  (con una posizione di riserva se il permesso viene negato).

## Nota sul Radar
La pagina Radar usa una `WebView` con Leaflet + tile RainViewer,
quindi richiede una connessione internet attiva (le tile non vengono
salvate offline in questa versione base).

## Prossimi passi possibili
- Aggiungere la selezione manuale della città (oltre al GPS).
- Salvare l'ultima posizione/i dati meteo in cache per l'avvio offline.
- Aggiungere un controllo manuale per scorrere avanti/indietro tra i
  frame del radar invece della sola animazione automatica.
