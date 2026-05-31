# VokalPro – Vokabeltrainer

Persönlicher Vokabeltrainer als Android-App (Apache Cordova + HTML/CSS/JS).

---

## Repo-Struktur

```
vokalpro/
├── index.html                              ← Die gesamte App (HTML + CSS + JS)
├── config.xml                              ← Cordova-Konfiguration
├── .gitignore
├── .github/
│   └── workflows/
│       ├── build-apk.yml                   ← CI: Debug-APK bauen
│       └── static.yml                      ← CI: GitHub Pages deployen
└── native/
    └── src/main/java/com/yourname/vokalpro/
        ├── VocabForegroundService.java     ← Persistente Drawer-Notification
        ├── BootReceiver.java               ← Neustart nach Geräte-Reboot
        └── VocabBridgePlugin.java          ← JS / Native Bridge
```

---

## GitHub Setup (einmalig)

1. Neues GitHub-Repo anlegen (z.B. `vokalpro`)
2. ZIP entpacken, entpackten Ordner per Drag & Drop auf github.com/DEINNAME/vokalpro hochladen
3. Commit - GitHub Actions starten automatisch

GitHub Pages: Settings -> Pages -> Source -> GitHub Actions -> speichern.
Danach erreichbar unter: https://DEINNAME.github.io/vokalpro/

---

## APK herunterladen

Nach jedem Push auf main/master:
1. GitHub -> Actions Tab
2. Letzten erfolgreichen Workflow-Run öffnen
3. Unter Artifacts: VokalPro-debug-XXXX.zip herunterladen
4. ZIP entpacken -> app-debug.apk auf Android installieren

---

## Persistente Notification (Drawer)

Laeuft als nativer Android ForegroundService:
- Nicht wischbar (ongoing: true)
- Ueberlebt App-Kill und RAM-Bereinigung
- Boot-persistent: erscheint nach Geraete-Neustart automatisch
- Buttons: Loesung / Gewusst / Nochmal / Vor / Zurueck

---

## Google Vision API (Scan-Funktion)

1. console.cloud.google.com -> Neues Projekt
2. Cloud Vision API aktivieren
3. API-Key erstellen: APIs & Services -> Credentials
4. In der App: Einstellungen -> API-Key eintragen

---

## App anpassen

| Was aendern | Datei |
|---|---|
| UI, Farben, Logik | index.html |
| App-ID, Plugins, Permissions | config.xml |
| Notification-Text/Buttons | VocabForegroundService.java |
| Boot-Verhalten | BootReceiver.java |
| JS/Native-Befehle | VocabBridgePlugin.java |

Nach jeder Aenderung: Datei(en) auf GitHub hochladen -> Commit -> CI baut automatisch neue APK.
