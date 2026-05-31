package com.yourname.vokalpro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * VocabForegroundService
 * ──────────────────────
 * Läuft als Android ForegroundService dauerhaft im Hintergrund.
 * Zeigt eine persistente, nicht-wischbare Notification in der Drawer
 * mit aktuellem Vokabelpaar + Action-Buttons (Lösung / Vor / Zurück / Gewusst).
 *
 * Gestartet von MainActivity (via Cordova JS-Bridge) und BootReceiver.
 */
public class VocabForegroundService extends Service {

    // ── Konstanten ───────────────────────────────────────────────────────
    public static final String CHANNEL_ID        = "vokalpro_vocab_channel";
    public static final int    NOTIFICATION_ID   = 1337;
    public static final String PREFS_NAME        = "VokalProPrefs";

    // Intent-Actions für Notification-Buttons
    public static final String ACTION_SHOW_ANSWER = "com.yourname.vokalpro.SHOW_ANSWER";
    public static final String ACTION_NEXT        = "com.yourname.vokalpro.NEXT";
    public static final String ACTION_PREV        = "com.yourname.vokalpro.PREV";
    public static final String ACTION_KNEW_IT     = "com.yourname.vokalpro.KNEW_IT";
    public static final String ACTION_DIDNT_KNOW  = "com.yourname.vokalpro.DIDNT_KNOW";
    public static final String ACTION_UPDATE      = "com.yourname.vokalpro.UPDATE";

    // ── Service-Lifecycle ────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            // Neustart nach Kill → Notification wiederherstellen
            showCurrentVocab(false);
            return START_STICKY;
        }

        String action = intent.getAction();
        if (action == null) action = ACTION_UPDATE;

        switch (action) {
            case ACTION_SHOW_ANSWER:
                showCurrentVocab(true);
                break;
            case ACTION_NEXT:
                navigateVocab(+1);
                break;
            case ACTION_PREV:
                navigateVocab(-1);
                break;
            case ACTION_KNEW_IT:
                recordAnswer(true);
                break;
            case ACTION_DIDNT_KNOW:
                recordAnswer(false);
                break;
            case ACTION_UPDATE:
            default:
                showCurrentVocab(false);
                break;
        }

        // START_STICKY → Service wird nach Kill vom System neu gestartet
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Kein Binding nötig
    }

    // ── Notification-Kanal (Android 8+) ─────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Vokabeltrainer",
                NotificationManager.IMPORTANCE_LOW  // LOW = kein Sound, aber sichtbar
            );
            channel.setDescription("Persistente Lernkarte");
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    // ── Vokabel-Daten aus SharedPreferences lesen ────────────────────────

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Liest die Vokabelliste für die aktive Sprache aus SharedPreferences.
     * Die JS-Seite schreibt bei jeder Änderung in dieselben Prefs.
     */
    private JSONArray getVocabs() {
        SharedPreferences prefs = getPrefs();
        String activeLang = prefs.getString("activeLang", "DE_ES");
        String json = prefs.getString("vocab_" + activeLang, "[]");
        try {
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private int getCurrentIndex() {
        return getPrefs().getInt("nb_index", 0);
    }

    private void setCurrentIndex(int idx) {
        getPrefs().edit().putInt("nb_index", idx).apply();
    }

    private String getActiveLang() {
        return getPrefs().getString("activeLang", "DE_ES");
    }

    /** Gibt Name der Quellsprache zurück */
    private String srcLangName() {
        String key = getActiveLang();
        String src = key.split("_")[0];
        switch (src) {
            case "DE": return "Deutsch";
            case "EN": return "Englisch";
            case "ES": return "Spanisch";
            case "FR": return "Französisch";
            case "IT": return "Italienisch";
            default:   return src;
        }
    }

    /** Gibt Name der Zielsprache zurück */
    private String tgtLangName() {
        String key = getActiveLang();
        String[] parts = key.split("_");
        String tgt = parts.length > 1 ? parts[1] : "ES";
        switch (tgt) {
            case "DE": return "Deutsch";
            case "EN": return "Englisch";
            case "ES": return "Spanisch";
            case "FR": return "Französisch";
            case "IT": return "Italienisch";
            default:   return tgt;
        }
    }

    // ── Notification aufbauen und anzeigen ───────────────────────────────

    private void showCurrentVocab(boolean revealAnswer) {
        JSONArray vocabs = getVocabs();
        if (vocabs.length() == 0) {
            showEmptyNotification();
            return;
        }

        int idx = getCurrentIndex() % vocabs.length();
        setCurrentIndex(idx);

        try {
            JSONObject vocab = vocabs.getJSONObject(idx);
            String source = vocab.optString("source", "—");
            String target = vocab.optString("target", "—");
            buildAndShow(source, target, revealAnswer, idx, vocabs.length());
        } catch (Exception e) {
            showEmptyNotification();
        }
    }

    private void navigateVocab(int direction) {
        JSONArray vocabs = getVocabs();
        if (vocabs.length() == 0) { showEmptyNotification(); return; }
        int idx = ((getCurrentIndex() + direction) + vocabs.length()) % vocabs.length();
        setCurrentIndex(idx);
        showCurrentVocab(false);
    }

    private void recordAnswer(boolean knew) {
        // Statistik zurück in Prefs schreiben (JS liest beim nächsten App-Start)
        SharedPreferences prefs = getPrefs();
        SharedPreferences.Editor ed = prefs.edit();
        int total = prefs.getInt("stat_total", 0) + 1;
        int correct = prefs.getInt("stat_correct", 0) + (knew ? 1 : 0);
        ed.putInt("stat_total", total);
        ed.putInt("stat_correct", correct);

        // Heute-Zähler erhöhen
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd",
            java.util.Locale.getDefault()).format(new java.util.Date());
        int todayCount = prefs.getInt("today_" + today, 0) + 1;
        ed.putInt("today_" + today, todayCount);
        ed.apply();

        // Zur nächsten Vokabel springen
        navigateVocab(+1);
    }

    /**
     * Baut die eigentliche Notification.
     * Nutzt NotificationCompat.BigTextStyle für die erweiterte Ansicht.
     */
    private void buildAndShow(String source, String target, boolean showAnswer,
                               int idx, int total) {

        Context ctx = getApplicationContext();

        // PendingIntent → App öffnen bei Tap auf Notification-Body
        Intent openApp = new Intent(ctx, getMainActivityClass());
        openApp.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent openAppPI = PendingIntent.getActivity(ctx, 0, openApp, piFlags);

        // Action-PendingIntents
        PendingIntent piShowAnswer = makeActionPI(ctx, ACTION_SHOW_ANSWER, 1, piFlags);
        PendingIntent piNext       = makeActionPI(ctx, ACTION_NEXT, 2, piFlags);
        PendingIntent piPrev       = makeActionPI(ctx, ACTION_PREV, 3, piFlags);
        PendingIntent piKnew       = makeActionPI(ctx, ACTION_KNEW_IT, 4, piFlags);
        PendingIntent piNoKnew     = makeActionPI(ctx, ACTION_DIDNT_KNOW, 5, piFlags);

        // Notification-Text
        String titleText  = "📚 Vokabeltrainer  " + getLangFlags();
        String bodyWord   = source;
        String bodyLang   = "← " + srcLangName();
        String answerLine = showAnswer ? target + "  ←  " + tgtLangName() : "";
        String counter    = (idx + 1) + " / " + total;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)   // Eigenes Icon: R.drawable.ic_vocab
            .setContentTitle(titleText)
            .setContentText(showAnswer ? answerLine : bodyWord + "  " + bodyLang)
            .setSubText(counter)
            .setOngoing(true)           // Nicht wischbar
            .setAutoCancel(false)
            .setSilent(true)            // Kein Sound / Vibration
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPI)
            .setColor(0xFF7C3AED);       // Lila Akzentfarbe

        // Erweiterte Ansicht mit BigText
        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle()
            .bigText(
                bodyWord + "\n" + bodyLang +
                (showAnswer ? "\n\n✦  " + answerLine : "")
            )
            .setBigContentTitle(titleText)
            .setSummaryText(counter);
        builder.setStyle(bigStyle);

        // Action-Buttons (in expandierter Ansicht sichtbar)
        if (!showAnswer) {
            builder.addAction(0, "Lösung", piShowAnswer);
        } else {
            builder.addAction(0, "✓ Gewusst", piKnew);
            builder.addAction(0, "✗ Nochmal", piNoKnew);
        }
        builder.addAction(0, "←", piPrev);
        builder.addAction(0, "→", piNext);

        Notification notification = builder.build();

        // Als ForegroundService starten / aktualisieren
        try {
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            // Fallback: direkt über NotificationManager
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification);
        }
    }

    private void showEmptyNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
            getApplicationContext(), CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📚 Vokabeltrainer")
            .setContentText("Füge zuerst Vokabeln hinzu 📖")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW);
        startForeground(NOTIFICATION_ID, builder.build());
    }

    private PendingIntent makeActionPI(Context ctx, String action, int reqCode, int flags) {
        Intent i = new Intent(ctx, VocabForegroundService.class);
        i.setAction(action);
        return PendingIntent.getService(ctx, reqCode, i, flags);
    }

    /** Versucht MainActivity-Klasse dynamisch zu laden (Cordova-Standard) */
    @SuppressWarnings("unchecked")
    private Class<?> getMainActivityClass() {
        try {
            return Class.forName(getPackageName() + ".MainActivity");
        } catch (ClassNotFoundException e) {
            return getClass(); // Fallback
        }
    }

    private String getLangFlags() {
        String lang = getActiveLang();
        String[] parts = lang.split("_");
        return langToFlag(parts[0]) + "→" + (parts.length > 1 ? langToFlag(parts[1]) : "");
    }

    private String langToFlag(String code) {
        switch (code) {
            case "DE": return "🇩🇪";
            case "EN": return "🇬🇧";
            case "ES": return "🇪🇸";
            case "FR": return "🇫🇷";
            case "IT": return "🇮🇹";
            case "PT": return "🇵🇹";
            case "NL": return "🇳🇱";
            case "PL": return "🇵🇱";
            case "TR": return "🇹🇷";
            case "RU": return "🇷🇺";
            default:   return "🏳";
        }
    }
}
