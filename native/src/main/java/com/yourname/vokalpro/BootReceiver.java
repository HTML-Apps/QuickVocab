package com.yourname.vokalpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * BootReceiver
 * ────────────
 * Empfängt BOOT_COMPLETED und MY_PACKAGE_REPLACED Broadcasts.
 * Startet VocabForegroundService neu, damit die Notification
 * auch nach Geräte-Neustart oder App-Update wieder erscheint.
 *
 * Benötigt in AndroidManifest.xml:
 *   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {

            startService(context);
        }
    }

    private void startService(Context context) {
        Intent serviceIntent = new Intent(context, VocabForegroundService.class);
        serviceIntent.setAction(VocabForegroundService.ACTION_UPDATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Ab Android 8 muss ForegroundService explizit als solcher gestartet werden
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
