package me.asswad.myyeelightlan;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class About extends AppCompatActivity {

    private TextView tv_version;
    private Button check_update_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        setTitle("About");

        tv_version = findViewById(R.id.app_version);
        check_update_btn = findViewById(R.id.check_update_btn);


        setButtonActions();
        showVersion();
    }

    private void setButtonActions(){
        check_update_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String update_url = "https://github.com/asnbd/MyYeelightLAN/releases/latest";
                Uri uri = Uri.parse(update_url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    private void showVersion(){
        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;

        String versionText = "v" + versionName;

        tv_version.setText(versionText);
    }
}