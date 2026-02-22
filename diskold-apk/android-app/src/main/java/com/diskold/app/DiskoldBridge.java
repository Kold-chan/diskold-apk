package com.diskold.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

/**
 * Bridge JS → Android.
 * El JS llama: AndroidBridge.startVoiceBubble("canal", false)
 */
public class DiskoldBridge {
    private final Activity activity;

    public DiskoldBridge(Activity activity) { this.activity = activity; }

    /** Llamado desde JS al entrar a llamada de voz */
    @JavascriptInterface
    public void startVoiceBubble(String channelName, boolean muted) {
        activity.runOnUiThread(() -> {
            // Foreground service (mantiene CPU despierta)
            Intent vs = new Intent(activity, VoiceService.class);
            vs.putExtra("channelName", channelName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                activity.startForegroundService(vs);
            else
                activity.startService(vs);

            // Burbuja flotante (solo si tiene permiso)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(activity)) {
                Intent bs = new Intent(activity, BubbleService.class);
                bs.putExtra("channelName", channelName);
                bs.putExtra("muted", muted);
                activity.startService(bs);
            }
        });
    }

    /** Llamado desde JS al salir de la llamada */
    @JavascriptInterface
    public void stopVoiceBubble() {
        activity.runOnUiThread(() -> {
            activity.stopService(new Intent(activity, VoiceService.class));
            activity.stopService(new Intent(activity, BubbleService.class));
        });
    }

    /** Llamado desde JS al mutear/desmutear */
    @JavascriptInterface
    public void updateMicState(boolean muted) {
        Intent i = new Intent(activity, BubbleService.class);
        i.setAction("UPDATE_MIC");
        i.putExtra("muted", muted);
        activity.startService(i);
    }

    /** Verifica si tiene permiso de overlay */
    @JavascriptInterface
    public boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(activity);
    }

    /** Abre la pantalla de permisos de overlay */
    @JavascriptInterface
    public void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + activity.getPackageName())));
        }
    }

    /** Toast nativo */
    @JavascriptInterface
    public void showToast(String msg) {
        activity.runOnUiThread(() ->
            android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_SHORT).show());
    }
}
