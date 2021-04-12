package me.asswad.myyeelightlan;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

public class AboutActivity extends AppCompatActivity {

    private final String TAG = "AboutActivity";
    private TextView tv_version;
    private Button check_update_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        getSupportActionBar().setTitle("About");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tv_version = findViewById(R.id.app_version);
        check_update_btn = findViewById(R.id.check_update_btn);


        setButtonActions();
        showVersion();
    }

    private void setButtonActions(){
        check_update_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkForUpdates();
            }
        });
    }

    private void showVersion(){
        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;

        String versionText = "v" + versionName;

        tv_version.setText(versionText);
    }

    private void checkForUpdates(){
        new GetUpdate().execute();
    }

    private void goToUpdateUrl(){
        String update_url = "https://github.com/asnbd/MyYeelightLAN/releases/latest";
        Uri uri = Uri.parse(update_url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private String removeInitialV(String version){
        if(version.startsWith("v") || version.startsWith("V")){
            return version.substring(1);
        }

        return version;
    }

    public int compareVersionNames(String oldVersionName, String newVersionName) {
        int res = 0;

        String[] oldNumbers = oldVersionName.split("\\.");
        String[] newNumbers = newVersionName.split("\\.");

        // To avoid IndexOutOfBounds
        int maxIndex = Math.min(oldNumbers.length, newNumbers.length);

        for (int i = 0; i < maxIndex; i ++) {
            int oldVersionPart = Integer.valueOf(oldNumbers[i]);
            int newVersionPart = Integer.valueOf(newNumbers[i]);

            if (oldVersionPart < newVersionPart) {
                res = -1;
                break;
            } else if (oldVersionPart > newVersionPart) {
                res = 1;
                break;
            }
        }

        // If versions are the same so far, but they have different length...
        if (res == 0 && oldNumbers.length != newNumbers.length) {
            res = (oldNumbers.length > newNumbers.length)?1:-1;
        }

        return res;
    }


    // Check for updates in background
    public class GetUpdate extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progressDialog = new ProgressDialog(AboutActivity.this);
        String responseString = "";

        String updateUrl = "https://api.github.com/repos/asnbd/MyYeelightLAN/releases/latest";

        @Override
        protected Void doInBackground(Void... params) {
            try {
                responseString = getData(updateUrl);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog.setMessage("Checking for updates...");
            progressDialog.setCancelable(true);
            progressDialog.show();
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    GetUpdate.this.cancel(true);
                }
            });
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "onPostExecute: " + responseString);

//            check_update_btn.setVisibility(View.VISIBLE);
            this.progressDialog.dismiss();

            if(responseString != null && !responseString.isEmpty()){
                String currentVersion = BuildConfig.VERSION_NAME;
                String onlineVersion = removeInitialV(responseString);

                int difference = compareVersionNames(currentVersion, onlineVersion);

                if(difference < 0){
//                    Toast.makeText(About.this, "New Version Available", Toast.LENGTH_SHORT).show();

                    new AlertDialog.Builder(AboutActivity.this)
                            .setTitle("New Version Available")
                            .setMessage("A new version of the app is available. Do you want to download it now?")
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> goToUpdateUrl())
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                } else {
                    new AlertDialog.Builder(AboutActivity.this)
                            .setTitle("Information")
                            .setMessage("You are already using the latest version.")
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            } else {
                new AlertDialog.Builder(AboutActivity.this)
                        .setTitle("Failed")
                        .setMessage("Failed checking updates.")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }
    }

    private String getData(String url) throws IOException, JSONException {
        JSONObject json = readJsonFromUrl(url);
        try {
            String response = json.getString("tag_name");
            Log.d(TAG, "getData: " + response);
            return response;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }
}