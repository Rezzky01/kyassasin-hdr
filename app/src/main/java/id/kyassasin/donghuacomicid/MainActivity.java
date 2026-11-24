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
    private static final int BG = Color.rgb(9, 10, 16);
    private WebView webView;
    private ProgressBar progress;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private ValueCallback<Uri[]> fileCallback;

    // App-wide rebrand plus a clean, responsive dark catalog skin on the source domain.
    private static final String BRAND_SCRIPT =
        "(function(){'use strict';try{" +
        "var host=(location.hostname||'').toLowerCase();var onSource=/(^|\\\\.)anichin\\\\.moe$/.test(host);" +
        "var cssId='kyassasin-clean-ui';" +
        "var css=" +
        "' :root{color-scheme:dark;--kbg:#080a12;--kpanel:#111522;--kpanel2:#171d2c;--kline:#252c40;--ktxt:#f4f6ff;--kmuted:#9aa4bb;--kpurple:#9875ff;--kblue:#5e8dff}' +" +
        "'html,body{background:var(--kbg)!important;color:var(--ktxt)!important;font-family:Inter,Roboto,\"Segoe UI\",Arial,sans-serif!important;font-size:14px!important;line-height:1.55!important}' +" +
        "'body{margin:0!important;overflow-x:hidden!important}a{color:#c6b7ff!important;text-decoration:none!important}a:hover{color:#fff!important}h1,h2,h3,h4,h5,.entry-title,.post-title{color:#f7f7ff!important;font-weight:800!important;letter-spacing:-.35px!important;line-height:1.25!important}' +" +
        "'header,.site-header,.c-header,.header,.top-header,.main-header,.c-header__top,.c-header__main,.c-header__nav,.main-navigation,.site-navigation{background:#0d111d!important;border:0!important;border-bottom:1px solid var(--kline)!important;box-shadow:0 8px 28px #0006!important}' +" +
        "'.container,.container-fluid,.site-content,.c-page,.main-col,.main-content{max-width:1240px!important}' +" +
        "'.site-content,.content-area,.main-col,.main-content,.widget-area,.sidebar,.c-page{color:var(--ktxt)!important}' +" +
        "'.c-tabs-item,.page-listing-item,.manga,.item,.post,.c-blog__item,.wp-manga,.popular-item-wrap,.widget,.anime-item,.bs,.listupd .bs,.listupd .utao,.listupd .bsx{background:var(--kpanel)!important;border:1px solid var(--kline)!important;border-radius:13px!important;overflow:hidden!important;box-shadow:0 7px 22px #0003!important}' +" +
        "'.c-tabs-item,.page-listing-item,.manga,.item,.post,.listupd .bs{margin:0 0 14px!important}' +" +
        "'.c-tabs-item__content,.item-summary,.post-content,.entry-content,.widget-content,.tab-content,.tt{color:#d6dbea!important}' +" +
        "'.listupd,.listupd .list-update,.listupd .listupd{gap:13px!important}' +" +
        "'.listupd .bs .bsx,.listupd .bs .bsx .limit,.listupd .bs .bsx .limit img{border-radius:10px!important}' +" +
        "'.listupd .bs .bsx .tt,.listupd .bs .bsx .limit .typez{color:#f3f4ff!important;font-weight:750!important}' +" +
        "'.listupd .bs .bsx .epx,.listupd .bs .bsx .sb,.listupd .bs .bsx .year{color:var(--kmuted)!important}' +" +
        "'img{max-width:100%;height:auto} .img-responsive,.item-thumb img,.c-image-hover img{object-fit:cover!important}' +" +
        "'input,select,textarea,.form-control,.search-field{background:#151b2a!important;color:#fff!important;border:1px solid #303951!important;border-radius:11px!important;min-height:43px!important;padding:9px 12px!important}' +" +
        "'button,.btn,.btn-primary,input[type=submit]{background:linear-gradient(135deg,#9673ff,#6259d9)!important;color:#fff!important;border:0!important;border-radius:10px!important;font-weight:750!important;box-shadow:0 5px 16px #7658ed33!important}' +" +
        "'.pagination a,.pagination span,.page-numbers{background:#151b2a!important;border:1px solid var(--kline)!important;border-radius:9px!important;padding:8px 12px!important;color:#eee!important}' +" +
        "'.breadcrumb,.c-breadcrumb,.nav-links,.chapter-list,.listing-chapters_wrap,.wp-manga-chapter{background:#111725!important;color:var(--kmuted)!important;border-color:var(--kline)!important;border-radius:10px!important}' +" +
        "'.entry-content,.content-area,.postbody,.post-content,.c-blog__content{color:#d9deec!important}' +" +
        "'video,iframe{max-width:100%;border:0!important;border-radius:12px!important;background:#000!important} .reading-content img{border-radius:0!important}' +" +
        "'#header,.header,.site-header{position:relative!important;z-index:20!important}' +" +
        "'@media(max-width:767px){.container,.container-fluid{padding-left:12px!important;padding-right:12px!important}.site-content{padding-top:10px!important}.listupd{gap:9px!important}.c-tabs-item,.page-listing-item,.manga,.item,.post{border-radius:11px!important}}' +" +
        "'html,body{overscroll-behavior-y:contain}';" +
        "if(onSource&&!document.getElementById(cssId)){var st=document.createElement('style');st.id=cssId;st.textContent=css;(document.head||document.documentElement).appendChild(st);}" +
        "function clean(s){return s.replace(/anichin\\\\s*care/ig,'KyassasinDev').replace(/anichin/ig,'KyassasinDev');}" +
        "function rebrand(){if(!document.body)return;" +
        "var walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);var node;while((node=walker.nextNode())){var parent=node.parentElement;if(parent&&/^(SCRIPT|STYLE|NOSCRIPT|TEXTAREA)$/i.test(parent.tagName))continue;if(/anichin/i.test(node.nodeValue))node.nodeValue=clean(node.nodeValue);}" +
        "document.querySelectorAll('[alt],[title],[aria-label],[placeholder]').forEach(function(e){['alt','title','aria-label','placeholder'].forEach(function(k){var v=e.getAttribute(k);if(v&&/anichin/i.test(v))e.setAttribute(k,clean(v));});});" +
        "document.querySelectorAll('input[value],meta[content]').forEach(function(e){['value','content'].forEach(function(k){var v=e.getAttribute(k);if(v&&/anichin/i.test(v))e.setAttribute(k,clean(v));});});" +
        "if(document.title&&/anichin/i.test(document.title))document.title=clean(document.title);" +
        "document.querySelectorAll('img,svg').forEach(function(e){var s=((e.getAttribute('alt')||'')+' '+(e.getAttribute('src')||'')+' '+(e.getAttribute('href')||''));if(/anichin/i.test(s)&&e.closest('header,.site-header,.logo,.site-logo,#logo,.c-header__logo'))e.style.setProperty('display','none','important');});" +
        "document.querySelectorAll('header .logo a,header .site-logo a,header #logo a,.site-header .logo a,.site-branding a,#logo a,.logo a,.c-header__logo a').forEach(function(a){a.textContent='KyassasinDev';a.setAttribute('aria-label','KyassasinDev');a.style.setProperty('font-size','20px','important');a.style.setProperty('font-weight','900','important');a.style.setProperty('color','#c6b7ff','important');});" +
        "}" +
        "var queued=false;function schedule(){if(queued)return;queued=true;setTimeout(function(){queued=false;rebrand();},80);}rebrand();" +
        "if(!window.__kyassasinObserver){window.__kyassasinObserver=new MutationObserver(schedule);window.__kyassasinObserver.observe(document.documentElement,{childList:true,subtree:true,characterData:true,attributes:true,attributeFilter:['title','alt','aria-label','placeholder','value']});}" +
        "window.addEventListener('DOMContentLoaded',rebrand);window.addEventListener('load',rebrand);" +
        "}catch(e){}})();";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR & 0);
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
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

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
