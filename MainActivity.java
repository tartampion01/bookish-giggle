package com.example.ptourigny.myapplication;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.net.http.SslError;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.SslErrorHandler;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;

public class MainActivity extends Activity  {

    private WebView wv1;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setContentView(R.layout.activity_main);

        //String url = "http://www.interlivraison.com/login.php";
        //String url = "http://www.betainterlivraison.camionbeaudoin.com/login.php";

        String url = "https://interlivraison.reseaudynamique.com/login.php";
        //String url = "https://interlivraison.reseaudynamique.com/__DEV/login.php";

        wv1=findViewById(R.id.webView);
        wv1.setWebViewClient(new MyBrowser() {
            @Override
            public void onPageFinished(WebView view, String url) {
                //Is the url the login-page?
                if (url.equalsIgnoreCase("https://interlivraison.reseaudynamique.com/login.php")) {

                    //load javascript to set the values to input fields
                    Boolean callFunction = false;
                    SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                    String usr = prefs.getString("username", null);
                    String pwd = prefs.getString("password", null);

                    // Use callFunction to validate that we have values and that they are not null or undefined
                    if( usr != null && pwd != null ){
                        // values might still be 'undefined' even if not null
                        callFunction = !usr.equalsIgnoreCase("undefined") && !pwd.equalsIgnoreCase("undefined");
                    }

                    if (callFunction) {
                        view.loadUrl("javascript:fillUserNamePassword('" + usr + "','" + pwd + "');");
                    }

                    // VERIFY VERSION OF APP AGAINST VALUE STORED ON WEBSITE. IF NEEDED A LINK TO UPDATE WILL BE PRESENTED TO USER
                    try {
                        ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(),PackageManager.GET_META_DATA);
                        Object APK_version = ai.metaData.get("APK_version");
                        view.loadUrl("javascript:checkVersion('" + APK_version + "');");
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        wv1.getSettings().setLoadsImagesAutomatically(true);
        wv1.getSettings().setJavaScriptEnabled(true);
        wv1.addJavascriptInterface(new JavaScriptInterface(), "Android");

        wv1.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        CookieManager.getInstance().setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv1,true);

            ///* To enable web view debugging
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
            /**/
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
                    return false; // DISABLE BACK BUTTON FOR APPLICATION
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        wv1.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        wv1.restoreState(savedInstanceState);
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

    private class JavaScriptInterface {

        /**
         * this should be triggered when user and pwd is correct, maybe after
         * successful login
         */
        @JavascriptInterface
        public void saveValues (String usr, String pwd) {

            if (usr == null || pwd == null) {
                return;
            }

            //save the values in SharedPrefs
            SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
            editor.putString("username", usr);
            editor.putString("password", pwd);
            editor.apply();
        }
    }

}