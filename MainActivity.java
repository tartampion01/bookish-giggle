package com.example.ptourigny.myapplication;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.net.http.SslError;

import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.Toast;

public class MainActivity extends Activity  {

    private WebView wv1;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setContentView(R.layout.activity_main);

        String url = "https://interlivraison.reseaudynamique.com/login.php";
        //String url = "https://interlivraison.reseaudynamique.com/__DEV/login.php";

        wv1 = findViewById(R.id.webView);
        wv1.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        wv1.setWebViewClient(new MyBrowser() {
            @Override
            public void onPageFinished(WebView view, String url) {
                //Is the url the login-page?
                if ("https://interlivraison.reseaudynamique.com/login.php".equalsIgnoreCase(url)) {

                    //load javascript to set the values to input fields
                    boolean callFunction = false;
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

        /* SETTINGS */
        wv1.getSettings().setLoadsImagesAutomatically(true);
        wv1.getSettings().setJavaScriptEnabled(true);
        wv1.getSettings().setAppCacheMaxSize( 5 * 1024 * 1024 ); // 5MB
        wv1.getSettings().setAppCachePath( getApplicationContext().getCacheDir().getAbsolutePath() );
        wv1.getSettings().setAllowFileAccess(true);
        wv1.getSettings().setAppCacheEnabled(true);
        wv1.getSettings().setUseWideViewPort(true);
        wv1.getSettings().setCacheMode( WebSettings.LOAD_DEFAULT ); // load online by default
        wv1.getSettings().setDomStorageEnabled(true);               // Otherwise we cant navigate pages offline and keep signature data. Use of window.localStorage in js files

        wv1.addJavascriptInterface(new JavaScriptInterface(), "Android");

        CookieManager.getInstance().setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv1,true);

            ///* To enable web view debugging
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
            /**/
        }

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

            Context mContext = MainActivity.this;
            final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;

            // handle different requests for different type of files
            // this example handles downloads requests for .apk
            // everything else the webview can handle normally
            if (url.endsWith(".apk")) {
                try {

                    if (Build.VERSION.SDK_INT >= 23) {
                        String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        if (!hasPermissions(mContext, PERMISSIONS)) {
                            ActivityCompat.requestPermissions((Activity) mContext, PERMISSIONS, REQUEST_WRITE_EXTERNAL_STORAGE );
                        } else {
                            //do here
                        }
                    } else {
                        //do here
                    }

                    Uri source = Uri.parse(url);
                    // Make a new request pointing to the .apk url
                    DownloadManager.Request request = new DownloadManager.Request(source);
                    // appears the same in Notification bar while downloading
                    request.setDescription("Description for the DownloadManager Bar");
                    request.setTitle("Interlivraison.apk");
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                    // save the file in the "Downloads" folder
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Interlivraison.apk");

                    // get download service and enqueue file
                    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    manager.enqueue(request);

                    Thread.sleep(2000);
                    Toast toast = Toast.makeText(MainActivity.this, "Le fichier a été téléchargé", Toast.LENGTH_LONG);
                    toast.show();

                    // Here we have file
                    /* TODO Automatically install APK
                    Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Application context = (Application) getApplicationContext();
                    File file = new File(Environment.getExternalStorageDirectory() + "/Download/Interlivraison.apk");
                    Uri data = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider" ,file);
                    intent.setDataAndType(data, "application/vnd.android.package-archive");
                    startActivity(intent);
                    */
                }
                catch (Exception e)
                {
                    Log.d("MyApplication", e.getMessage());
                }

            }
            // if there is a link to anything else than .apk or .mp3 load the URL in the webview
            else view.loadUrl(url);

            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
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

    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    public static boolean CheckPermission(final Context context){
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if( currentAPIVersion >= Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                    alertBuilder.setCancelable(true);
                    alertBuilder.setTitle("Permission necessary");
                    alertBuilder.setMessage("External storage permission is necessary");
                    alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions((Activity) context,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                        }
                    });
                    AlertDialog alert = alertBuilder.create();
                    alert.show();
                } else {
                    ActivityCompat.requestPermissions((Activity) context,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

}