package com.diskold.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // ⚠️ CAMBIA ESTA URL por la de tu servidor desplegado
    public static final String SERVER_URL = "https://diskold.onrender.com";

    private WebView webView;
    private BroadcastReceiver bubbleReceiver;

    @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);
        setupWebView();
        setupBubbleReceiver();
        requestOverlayPermissionIfNeeded();
        webView.loadUrl(SERVER_URL);
    }

    @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
    private void setupWebView() {
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setUserAgentString(ws.getUserAgentString() + " DiskoldNativeApp/4.4");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        webView.addJavascriptInterface(new DiskoldBridge(this), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (url.startsWith(SERVER_URL) || url.startsWith("diskold://")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) {}
                return true;
            }
            @Override
            public void onReceivedError(WebView v, int code, String desc, String url) {
                v.loadData(offlinePage(), "text/html", "UTF-8");
            }
        });
    }

    private void setupBubbleReceiver() {
        bubbleReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) return;
                switch (intent.getAction()) {
                    case "com.diskold.TOGGLE_MIC":
                        runOnUiThread(() -> webView.evaluateJavascript(
                            "if(typeof toggleMic==='function')toggleMic();", null));
                        break;
                    case "com.diskold.HANG_UP":
                        runOnUiThread(() -> webView.evaluateJavascript(
                            "if(typeof leaveVoice==='function')leaveVoice();", null));
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.diskold.TOGGLE_MIC");
        filter.addAction("com.diskold.HANG_UP");
        registerReceiver(bubbleReceiver, filter);
    }

    private void requestOverlayPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                .setTitle("Burbuja de llamada")
                .setMessage("Para mostrar la burbuja de voz sobre otras apps, Diskold necesita permiso de \"Mostrar sobre otras apps\".")
                .setPositiveButton("Conceder", (d, w) -> {
                    startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())), 1001);
                })
                .setNegativeButton("Más tarde", null)
                .show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.post(() -> webView.evaluateJavascript(
            "document.dispatchEvent(new Event('visibilitychange'));", null));
    }

    @Override
    protected void onPause() { super.onPause(); webView.onPause(); }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(bubbleReceiver); } catch (Exception e) {}
        webView.destroy();
        super.onDestroy();
    }

    private String offlinePage() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/><meta name='viewport' content='width=device-width,initial-scale=1'/>"
            + "<style>body{background:#0a0a0c;color:#e8eaf0;font-family:monospace;display:flex;flex-direction:column;"
            + "align-items:center;justify-content:center;height:100vh;gap:20px;text-align:center;padding:24px;}"
            + "h1{color:#a8d8ff;font-size:2.5rem;letter-spacing:6px;}p{color:#6b6b80;font-size:.9rem;}"
            + "button{background:#a8d8ff;border:none;border-radius:10px;padding:12px 32px;font-size:1rem;font-weight:700;cursor:pointer;color:#0a0a0c;}"
            + "</style></head><body><h1>DISKOLD</h1><p>Sin conexión al servidor.</p>"
            + "<button onclick='location.reload()'>Reintentar</button></body></html>";
    }
}
