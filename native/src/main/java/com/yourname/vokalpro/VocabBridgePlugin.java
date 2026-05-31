package com.yourname.vokalpro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * VocabBridgePlugin
 * ─────────────────
 * Cordova-Plugin das JavaScript-Aufrufe aus index.html
 * an den nativen VocabForegroundService weiterleitet.
 *
 * JS-Aufruf:
 *   cordova.exec(success, error, "VocabBridge", "startService", []);
 *   cordova.exec(success, error, "VocabBridge", "updateVocabs", [langKey, vocabsJsonString]);
 *   cordova.exec(success, error, "VocabBridge", "stopService", []);
 *   cordova.exec(success, error, "VocabBridge", "syncSettings", [settingsJsonString]);
 *
 * Registrierung in res/xml/config.xml:
 *   <feature name="VocabBridge">
 *     <param name="android-package" value="com.yourname.vokalpro.VocabBridgePlugin" />
 *   </feature>
 */
public class VocabBridgePlugin extends CordovaPlugin {

    private static final String PREFS_NAME = "VokalProPrefs";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
            throws JSONException {

        Context ctx = cordova.getActivity().getApplicationContext();

        switch (action) {

            // Startet den ForegroundService (und damit die persistente Notification)
            case "startService":
                startForegroundService(ctx);
                callbackContext.success("Service gestartet");
                return true;

            // Stoppt den ForegroundService
            case "stopService":
                ctx.stopService(new Intent(ctx, VocabForegroundService.class));
                callbackContext.success("Service gestoppt");
                return true;

            // Schreibt Vokabeln für eine Sprache in SharedPreferences
            // args[0] = langKey (z.B. "DE_ES"), args[1] = JSON-String der Vokabelliste
            case "updateVocabs":
                if (args.length() >= 2) {
                    String langKey = args.getString(0);
                    String vocabsJson = args.getString(1);
                    SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit()
                        .putString("vocab_" + langKey, vocabsJson)
                        .apply();
                    // Notification aktualisieren
                    startForegroundService(ctx);
                    callbackContext.success("Vokabeln gespeichert: " + langKey);
                } else {
                    callbackContext.error("Argumente fehlen: langKey, vocabsJson");
                }
                return true;

            // Synct allgemeine Einstellungen (aktive Sprache, tägliches Ziel)
            // args[0] = JSON-String mit Einstellungen
            case "syncSettings":
                if (args.length() >= 1) {
                    String settingsJson = args.getString(0);
                    JSONObject s = new JSONObject(settingsJson);
                    SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor ed = prefs.edit();
                    if (s.has("activeLang"))  ed.putString("activeLang",  s.getString("activeLang"));
                    if (s.has("dailyGoal"))   ed.putInt("dailyGoal",      s.getInt("dailyGoal"));
                    if (s.has("nb_index"))    ed.putInt("nb_index",        s.getInt("nb_index"));
                    ed.apply();
                    // Notification mit neuer Sprache aktualisieren
                    startForegroundService(ctx);
                    callbackContext.success("Einstellungen synchronisiert");
                } else {
                    callbackContext.error("Argumente fehlen: settingsJson");
                }
                return true;

            // Liest Statistiken aus Prefs zurück an JS
            case "getStats":
                SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                JSONObject stats = new JSONObject();
                stats.put("totalLearned", prefs.getInt("stat_total", 0));
                stats.put("totalCorrect", prefs.getInt("stat_correct", 0));
                callbackContext.success(stats);
                return true;

            default:
                callbackContext.error("Unbekannte Action: " + action);
                return false;
        }
    }

    private void startForegroundService(Context ctx) {
        Intent intent = new Intent(ctx, VocabForegroundService.class);
        intent.setAction(VocabForegroundService.ACTION_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent);
        } else {
            ctx.startService(intent);
        }
    }
}
