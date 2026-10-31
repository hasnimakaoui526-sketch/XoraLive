package com.xoralive.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.PermissionRequest;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    private static final int MEDIA_PERMISSION_REQUEST = 4102;
    private static final int FILE_CHOOSER_REQUEST = 4103;
    private static final String APP_ASSET_HOST = "appassets.androidplatform.net";

    private WebView webView;
    private PermissionRequest pendingWebPermission;
    private String[] pendingAndroidPermissions = new String[0];
    private ValueCallback<Uri[]> filePathCallback;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(9,7,13));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(9,7,13));

        WebView.setWebContentsDebuggingEnabled(false);
        webView = new WebView(this);
        setContentView(webView);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                if ("https".equalsIgnoreCase(scheme) && !APP_ASSET_HOST.equalsIgnoreCase(uri.getHost())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handleWebPermissionRequest(request));
            }

            @Override public void onPermissionRequestCanceled(PermissionRequest request) {
                runOnUiThread(() -> {
                    if (pendingWebPermission == request) {
                        pendingWebPermission = null;
                        pendingAndroidPermissions = new String[0];
                    }
                });
            }

            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "تعذر فتح اختيار الملفات", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl("https://" + APP_ASSET_HOST + "/assets/index.html");
    }

    private void handleWebPermissionRequest(PermissionRequest request) {
        if (request == null) return;

        Uri origin = request.getOrigin();
        if (origin == null || !"https".equalsIgnoreCase(origin.getScheme())
                || !APP_ASSET_HOST.equalsIgnoreCase(origin.getHost())) {
            request.deny();
            return;
        }

        boolean needsCamera = false;
        boolean needsMicrophone = false;
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) needsCamera = true;
            else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) needsMicrophone = true;
        }
        if (!needsCamera && !needsMicrophone) {
            request.deny();
            return;
        }

        if (pendingWebPermission != null && pendingWebPermission != request) {
            try { pendingWebPermission.deny(); } catch (Exception ignored) {}
        }
        pendingWebPermission = request;

        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        if (needsCamera && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.CAMERA);
        }
        if (needsMicrophone && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECORD_AUDIO);
        }

        if (missing.isEmpty()) {
            grantPendingWebPermission();
        } else {
            pendingAndroidPermissions = missing.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, pendingAndroidPermissions, MEDIA_PERMISSION_REQUEST);
        }
    }

    private void grantPendingWebPermission() {
        PermissionRequest request = pendingWebPermission;
        pendingWebPermission = null;
        pendingAndroidPermissions = new String[0];
        if (request == null) return;
        try { request.grant(request.getResources()); }
        catch (Exception ignored) { try { request.deny(); } catch (Exception ignoredAgain) {} }
    }

    private void denyPendingWebPermission() {
        PermissionRequest request = pendingWebPermission;
        pendingWebPermission = null;
        pendingAndroidPermissions = new String[0];
        if (request != null) {
            try { request.deny(); } catch (Exception ignored) {}
        }
    }

    @Override public void onBackPressed() {
        if (webView == null) {
            return;
        }

        // First return through WebView navigation history when available.
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }

        // The app is a single-page interface: dispatch a back event so the
        // active screen/dialog can handle it without destroying the Activity.
        webView.evaluateJavascript(
                "(function(){"
                + "try {"
                + " if (document.activeElement && document.activeElement.blur) document.activeElement.blur();"
                + " window.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',code:'Escape',keyCode:27,which:27,bubbles:true}));"
                + " window.dispatchEvent(new CustomEvent('xora:hardwareback'));"
                + "} catch(e) {}"
                + "})();",
                null
        );
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MEDIA_PERMISSION_REQUEST) {
            boolean cameraNeeded = false;
            boolean microphoneNeeded = false;
            for (String permission : pendingAndroidPermissions) {
                if (Manifest.permission.CAMERA.equals(permission)) cameraNeeded = true;
                if (Manifest.permission.RECORD_AUDIO.equals(permission)) microphoneNeeded = true;
            }
            boolean cameraGranted = !cameraNeeded || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            boolean microphoneGranted = !microphoneNeeded || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

            if (cameraGranted && microphoneGranted) {
                grantPendingWebPermission();
            } else {
                denyPendingWebPermission();
                Toast.makeText(this, "اسمح للكاميرا والميكروفون من إعدادات التطبيق لبدء البث.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(result);
            filePathCallback = null;
        }
    }

    @Override protected void onDestroy() {
        denyPendingWebPermission();
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
