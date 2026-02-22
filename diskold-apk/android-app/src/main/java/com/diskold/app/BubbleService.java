package com.diskold.app;

import android.app.Service;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/**
 * Burbuja flotante que se dibuja SOBRE todas las apps (como Discord).
 * Requiere permiso SYSTEM_ALERT_WINDOW.
 */
public class BubbleService extends Service {

    private WindowManager wm;
    private FrameLayout bubbleView;
    private LinearLayout panelView;
    private WindowManager.LayoutParams bubbleParams;
    private boolean muted = false;
    private String channelName = "Voz";
    private boolean panelOpen = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        if ("UPDATE_MIC".equals(intent.getAction())) {
            muted = intent.getBooleanExtra("muted", false);
            refreshBubbleIcon();
            return START_STICKY;
        }

        if (intent.hasExtra("channelName")) channelName = intent.getStringExtra("channelName");
        muted = intent.getBooleanExtra("muted", false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf(); return START_NOT_STICKY;
        }

        if (wm == null) {
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            createBubble();
        }
        return START_STICKY;
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void createBubble() {
        // Contenedor circular
        bubbleView = new FrameLayout(this);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#111114"));
        bg.setStroke(dp(3), Color.parseColor("#39ffc0"));
        bubbleView.setBackground(bg);
        bubbleView.setElevation(dp(8));

        // Icono
        TextView icon = new TextView(this);
        icon.setId(android.R.id.text1);
        icon.setText("🎤");
        icon.setTextSize(22);
        icon.setGravity(Gravity.CENTER);
        bubbleView.addView(icon, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        bubbleParams = new WindowManager.LayoutParams(
            dp(60), dp(60), overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.BOTTOM | Gravity.END;
        bubbleParams.x = dp(16);
        bubbleParams.y = dp(90);

        wm.addView(bubbleView, bubbleParams);
        setupDrag();
    }

    private void setupDrag() {
        final int[] startX = {0}, startY = {0};
        final float[] touchX = {0}, touchY = {0};
        final boolean[] dragging = {false};

        bubbleView.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX[0] = bubbleParams.x; startY[0] = bubbleParams.y;
                    touchX[0] = e.getRawX(); touchY[0] = e.getRawY();
                    dragging[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = touchX[0] - e.getRawX();
                    float dy = touchY[0] - e.getRawY();
                    if (Math.abs(dx) > dp(4) || Math.abs(dy) > dp(4)) dragging[0] = true;
                    if (dragging[0]) {
                        bubbleParams.x = (int)(startX[0] + dx);
                        bubbleParams.y = (int)(startY[0] + dy);
                        if (wm != null) try { wm.updateViewLayout(bubbleView, bubbleParams); } catch (Exception ex) {}
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragging[0]) togglePanel();
                    return true;
            }
            return false;
        });
    }

    private void togglePanel() {
        if (panelOpen) closePanel();
        else openPanel();
    }

    private void openPanel() {
        if (panelOpen) return;
        panelOpen = true;

        panelView = new LinearLayout(this);
        panelView.setOrientation(LinearLayout.VERTICAL);
        panelView.setPadding(dp(14), dp(12), dp(14), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(Color.parseColor("#111114"));
        bg.setStroke(dp(1), Color.parseColor("#2a2a35"));
        panelView.setBackground(bg);
        panelView.setElevation(dp(16));

        // Status
        TextView status = new TextView(this);
        status.setText("🟢  EN LLAMADA");
        status.setTextColor(Color.parseColor("#39ffc0"));
        status.setTextSize(10f);
        status.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        panelView.addView(status);

        // Channel name
        TextView chTv = new TextView(this);
        chTv.setText("# " + channelName);
        chTv.setTextColor(Color.parseColor("#e8eaf0"));
        chTv.setTextSize(15f);
        chTv.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams chP = new LinearLayout.LayoutParams(-2, -2);
        chP.topMargin = dp(5); chP.bottomMargin = dp(10);
        panelView.addView(chTv, chP);

        // Mic button
        panelView.addView(makeBtn(
            muted ? "🔇  Activar micrófono" : "🎤  Silenciar",
            muted ? "#2a0a15" : "#1a1a22",
            muted ? "#ff4466" : "#a8d8ff",
            v -> {
                sendBroadcast(new Intent("com.diskold.TOGGLE_MIC"));
                closePanel();
            }));

        // Hang up button
        LinearLayout.LayoutParams hangP = new LinearLayout.LayoutParams(-1, -2);
        hangP.topMargin = dp(7);
        Button hangBtn = makeBtn("📵  Colgar", "#2a0a0a", "#ff4466", v -> {
            sendBroadcast(new Intent("com.diskold.HANG_UP"));
            stopSelf();
        });
        panelView.addView(hangBtn, hangP);

        // Open app button
        LinearLayout.LayoutParams appP = new LinearLayout.LayoutParams(-1, -2);
        appP.topMargin = dp(5);
        Button appBtn = makeBtn("↩  Abrir Diskold", "#0d1a2a", "#a8d8ff", v -> {
            Intent open = new Intent(this, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(open);
            closePanel();
        });
        panelView.addView(appBtn, appP);

        WindowManager.LayoutParams pp = new WindowManager.LayoutParams(
            dp(210), -2, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT);
        pp.gravity = Gravity.BOTTOM | Gravity.END;
        pp.x = dp(16);
        pp.y = bubbleParams.y + dp(70);

        wm.addView(panelView, pp);

        // Close on outside touch
        panelView.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_OUTSIDE) closePanel();
            return false;
        });
    }

    private void closePanel() {
        if (!panelOpen || panelView == null) return;
        panelOpen = false;
        try { wm.removeView(panelView); } catch (Exception e) {}
        panelView = null;
    }

    private Button makeBtn(String text, String bgHex, String textHex, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor(textHex));
        btn.setTextSize(13f);
        btn.setPadding(dp(12), dp(9), dp(12), dp(9));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(Color.parseColor(bgHex));
        btn.setBackground(bg);
        btn.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        btn.setLayoutParams(p);
        return btn;
    }

    private void refreshBubbleIcon() {
        if (bubbleView == null) return;
        TextView icon = bubbleView.findViewById(android.R.id.text1);
        if (icon != null) icon.setText(muted ? "🔇" : "🎤");
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#111114"));
        bg.setStroke(dp(3), muted ? Color.parseColor("#ff4466") : Color.parseColor("#39ffc0"));
        bubbleView.setBackground(bg);
    }

    @Override
    public void onDestroy() {
        closePanel();
        if (bubbleView != null && wm != null) try { wm.removeView(bubbleView); } catch (Exception e) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}
