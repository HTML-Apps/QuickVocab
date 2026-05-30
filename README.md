# QuicVocab – Vokabeltrainer

Persönlicher Vokabeltrainer als Android-App (Cordova + HTML).

## Voraussetzungen

- Node.js 18+
- Java JDK 17
- Android SDK (via Android Studio empfohlen)
- `ANDROID_HOME` Umgebungsvariable gesetzt

## Schnellstart

```bash
# Cordova installieren
npm install -g cordova

# Projekt erstellen und Dateien kopieren
cordova create VokalPro com.yourname.vokalprо VokalPro
cd VokalPro

# index.html und config.xml ersetzen
cp /pfad/zur/index.html www/index.html
cp /pfad/zur/config.xml config.xml

# Android-Platform hinzufügen
cordova platform add android

# Debug-Build
cordova build android

# Release-Build (unsigned)
cordova build android --release
```

## APK-Pfad nach Build

```
platforms/android/app/build/outputs/apk/debug/app-debug.apk
platforms/android/app/build/outputs/apk/release/app-release-unsigned.apk
```

## Signing für Release (einmalig)

```bash
# Keystore erstellen
keytool -genkey -v -keystore vokalprо.keystore \
  -alias vokalprо -keyalg RSA -keysize 2048 -validity 10000

# APK signieren
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore vokalprо.keystore \
  platforms/android/app/build/outputs/apk/release/app-release-unsigned.apk \
  vokalprо

# Zipalign
zipalign -v 4 app-release-unsigned.apk VokalPro.apk
```

## Google Vision API einrichten

1. [Google Cloud Console](https://console.cloud.google.com) öffnen
2. Neues Projekt erstellen
3. "Cloud Vision API" aktivieren
4. API-Key erstellen (API & Services → Credentials)
5. In der App: ⚙️ → API-Key eintragen

## Features

- **Scan**: Foto von Schulbuch → OCR via Google Vision API → Wortpaare extrahieren
- **Editieren**: Vokabelliste bearbeiten, suchen, sortieren
- **Sprachen**: Mehrere Sprachpaare verwalten
- **Streak**: Lernfortschritt, Statistiken, tägliches Ziel
- **Lernkarte**: Persistente Banner-Notification mit Vokabelquiz
- **Dark/Light Mode**: Systemweit umschaltbar
- **Offline**: Vollständig offline nutzbar (außer OCR)

## GitHub Actions – Automatischer APK-Build

Erstelle `.github/workflows/build.yml`:

```yaml
name: Build Android APK
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with: { node-version: '18' }
      - uses: actions/setup-java@v3
        with: { java-version: '17', distribution: 'temurin' }
      - uses: android-actions/setup-android@v2
      - run: npm install -g cordova
      - run: cordova platform add android
      - run: cordova build android --release
      - uses: actions/upload-artifact@v3
        with:
          name: app-release
          path: platforms/android/app/build/outputs/apk/release/*.apk
```
