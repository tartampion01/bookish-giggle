package com.example.ptourigny.myapplication;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.net.http.SslError;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.SslErrorHandler;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class MainActivity extends Activity  {

    private WebView wv1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //String url = "http://www.interlivraison.com/login.php";
        //String url = "http://www.betainterlivraison.camionbeaudoin.com/login.php";
        String url = "https://interlivraison.reseaudynamique.com/login.php";

        wv1=(WebView)findViewById(R.id.webView);
        wv1.setWebViewClient(new MyBrowser());
/*git*/
        wv1.getSettings().setLoadsImagesAutomatically(true);
        wv1.getSettings().setJavaScriptEnabled(true);
        wv1.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        CookieManager.getInstance().setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv1,true);
        }

        wv1.getSettings().setAppCacheMaxSize( 5 * 1024 * 1024 ); // 5MB
        wv1.getSettings().setAppCachePath( getApplicationContext().getCacheDir().getAbsolutePath() );
        wv1.getSettings().setAllowFileAccess(true);
        wv1.getSettings().setAppCacheEnabled(true);
        wv1.getSettings().setUseWideViewPort(true);
        wv1.getSettings().setCacheMode( WebSettings.LOAD_DEFAULT ); // load online by default
        wv1.getSettings().setDomStorageEnabled(true); // Otherwise we cant navigate pages offline and keep signature data. Use of window.localStorage in js files


        /*
        if ( !isNetworkAvailable() ) { // loading offline
            wv1.getSettings().setCacheMode( WebSettings.LOAD_CACHE_ELSE_NETWORK );
            url = "file:///android_asset/horsligne.html";
            wv1 =(WebView)findViewById(R.id.webview);
            wv1.loadUrl(url);
        }
        else // online
        {
            wv1.loadUrl(url);
        }
        */

        wv1.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {

                Log.d("MyApplication", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }
        });

        if ( !isNetworkAvailable() ) { // loading offline
            wv1.getSettings().setCacheMode( WebSettings.LOAD_CACHE_ELSE_NETWORK );
        }

        wv1.loadUrl(url);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (wv1.canGoBack()) {
                        wv1.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private class MyBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //view.loadUrl(url);
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }
    }


}