package id.kyassasin.donghuacomicid;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME = "https://anichin.moe/";
    private static final int FILE_CHOOSER = 71;
    private static final int BG = Color.rgb(10, 10, 16);
    private WebView webView;
    private ProgressBar progress;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private ValueCallback<Uri[]> fileCallback;

    private static final String BRAND_SCRIPT =
        "(function(){try{" +
        "if(!/^(https?:)?\\/\\/(www\\.)?anichin\\.moe/i.test(location.href))return;" +
        "var id='kyassasin-brand-style';" +
        "if(!document.getElementById(id)){" +
        "var css='header .logo img,header .site-logo img,header #logo img,.site-header .logo img,.site-branding img,#logo img,.logo img{display:none!important}header .logo a,header .site-logo a,header #logo a,.site-header .logo a,.site-branding a,#logo a,.logo a{font-size:0!important;text-decoration:none!important}header .logo a:after,header .site-logo a:after,header #logo a:after,.site-header .logo a:after,.site-branding a:after,#logo a:after,.logo a:after{content:attr(data-brand)!important;display:inline-block!important;font:800 22px/1.2 Arial,sans-serif!important;letter-spacing:.4px!important;color:#a78bfa!important;padding:8px 4px!important}html,body{overscroll-behavior-y:contain}video,iframe{max-width:100%}';" +
        "var st=document.createElement('style');st.id=id;st.textContent=css;(document.head||document.documentElement).appendChild(st);" +
        "}" +
        "document.querySelectorAll('header .logo img,header .site-logo img,header #logo img,.site-header .logo img,.site-branding img,#logo img,.logo img').forEach(function(img){img.style.display='none';});" +
        "document.querySelectorAll('header .logo a,header .site-logo a,header #logo a,.site-header .logo a,.site-branding a,#logo a,.logo a').forEach(function(a){a.setAttribute('data-brand','KyassasinDev');a.setAttribute('aria-label','KyassasinDev');});" +
        "}catch(e){}})();";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        webView = new WebView(this);
        webView.setBackgroundColor(BG);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(167, 139, 250)));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(35, 32, 48)));
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        root.addView(progress, new FrameLayout.LayoutParams(-1, dp(2)));
        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
                if ("http".equals(scheme) || "https".equals(scheme)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (Exception e) { Toast.makeText(MainActivity.this, "Tidak ada aplikasi untuk membuka tautan ini.", Toast.LENGTH_SHORT).show(); }
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(BRAND_SCRIPT, null);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) Toast.makeText(MainActivity.this,
                    "Koneksi bermasalah. Periksa internet lalu muat ulang.", Toast.LENGTH_LONG).show();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreenView != null) { callback.onCustomViewHidden(); return; }
                fullscreenView = view;
                fullscreenCallback = callback;
                ((ViewGroup) webView.getParent()).addView(view, new FrameLayout.LayoutParams(-1, -1));
                webView.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
            @Override public void onHideCustomView() { hideFullscreen(); }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), FILE_CHOOSER); }
                catch (Exception e) { fileCallback = null; return false; }
                return true;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle(filename);
                request.setDescription("Unduhan KyassasinDev");
                request.setMimeType(mimeType);
                request.addRequestHeader("User-Agent", userAgent);
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) request.addRequestHeader("Cookie", cookie);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
                Toast.makeText(this, "Unduhan dimulai.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Unduhan tidak dapat dimulai.", Toast.LENGTH_LONG).show();
            }
        });

        if (state != null) {
            if (webView.restoreState(state) == null) webView.loadUrl(HOME);
        } else webView.loadUrl(HOME);
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private void hideFullscreen() {
        if (fullscreenView == null) return;
        if (fullscreenView.getParent() instanceof ViewGroup)
            ((ViewGroup) fullscreenView.getParent()).removeView(fullscreenView);
        fullscreenView = null;
        webView.setVisibility(View.VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        if (fullscreenCallback != null) fullscreenCallback.onCustomViewHidden();
        fullscreenCallback = null;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER && fileCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (webView != null) webView.saveState(out);
    }

    @Override public void onBackPressed() {
        if (fullscreenView != null) { hideFullscreen(); return; }
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        if (webView != null) { webView.stopLoading(); webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
