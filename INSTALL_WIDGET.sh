#!/data/data/com.termux/files/usr/bin/bash
set -e

ROOT="$(pwd)"
if [ ! -f "$ROOT/settings.gradle.kts" ] || [ ! -d "$ROOT/app/src/main" ]; then
  echo "ERRORE: esegui il comando dalla cartella ~/app-Meteo"
  exit 1
fi

echo "Controllo progetto Meteo..."
test -f app/src/main/java/com/quaderno/appmeteo/widget/WeatherWidgetProvider.kt
test -f app/src/main/res/xml/weather_widget_info.xml
test -f app/src/main/res/layout/weather_widget.xml

echo "Widget presente e progetto pronto."
echo "Ora esegui:"
echo "  git add ."
echo "  git commit -m 'Aggiungi widget meteo funzionante'"
echo "  git push"
