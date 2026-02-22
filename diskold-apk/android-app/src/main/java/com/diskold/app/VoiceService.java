package com.diskold.app;

import android.app.*;
import android.content.Intent;
import android.os.*;

/**
 * Foreground Service — mantiene el proceso vivo durante llamadas de voz.
 * Muestra notificación persistente con botón de colgar.
 */
public class VoiceService extends Service {

    private static final String CH_ID = "diskold_voice";
    private static final int NOTIF_ID = 1001;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String chName = intent != null ? intent.getStringExtra("channelName") : null;
        if (chName == null) chName = "Voz";
        startForeground(NOTIF_ID, buildNotif(chName));
        return START_STICKY;
    }

    private Notification buildNotif(String chName) {
        PendingIntent openApp = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent hangUp = PendingIntent.getBroadcast(this, 1,
            new Intent("com.diskold.HANG_UP"),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CH_ID)
            .setContentTitle("🎤 Diskold — En llamada")
            .setContentText("Canal: " + chName + "  •  Toca para volver")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openApp)
            .setOngoing(true)
            .addAction(new Notification.Action.Builder(
                android.R.drawable.ic_menu_call, "Colgar", hangUp).build())
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CH_ID, "Llamadas de voz", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Mantiene activa la llamada de voz");
            ch.setSound(null, null);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "diskold:voice");
            wakeLock.acquire(4 * 60 * 60 * 1000L); // máx 4h
        }
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}
