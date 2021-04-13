package me.asswad.myyeelightlan;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class HelpActivity extends AppCompatActivity {

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        getSupportActionBar().setTitle("Help");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        webView = findViewById(R.id.helpWebView);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                if(url.contains(".com/asnbd")){
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    view.getContext().startActivity(intent);
                } else {
                    view.loadUrl(url);
                }

                return true;
            }
        });

        String url = "https://asnbd.github.io/MyYeelightLAN/help.html";
        webView.loadUrl(url);
    }
}