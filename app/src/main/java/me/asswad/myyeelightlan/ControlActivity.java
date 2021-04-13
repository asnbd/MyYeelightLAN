package me.asswad.myyeelightlan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class ControlActivity extends AppCompatActivity {

    private final String TAG = "ControlActivity";

    private static final int MSG_CONNECT_SUCCESS = 0;
    private static final int MSG_CONNECT_FAILURE = 1;
    private static final String CMD_TOGGLE = "{\"id\":%id,\"method\":\"toggle\",\"params\":[]}\r\n" ;
    private static final String CMD_GET_PROP = "{\"id\":%id,\"method\":\"get_prop\",\"params\":[\"power\",\"bright\",\"ct\",\"rgb\",\"hue\",\"sat\",\"name\"]}\r\n" ;
    private static final String CMD_ON = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"on\",\"smooth\",%smooth]}\r\n" ;
    private static final String CMD_OFF = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"off\",\"smooth\",%smooth]}\r\n" ;
    private static final String CMD_CT = "{\"id\":%id,\"method\":\"set_ct_abx\",\"params\":[%value, \"smooth\", %smooth]}\r\n";
    private static final String CMD_HSV = "{\"id\":%id,\"method\":\"set_hsv\",\"params\":[%hue, %saturation, \"smooth\", %smooth]}\r\n";
    private static final String CMD_COLOR = "{\"id\":%id,\"method\":\"set_rgb\",\"params\":[%value, \"smooth\", %smooth]}\r\n";
    private static final String CMD_BRIGHTNESS = "{\"id\":%id,\"method\":\"set_bright\",\"params\":[%value, \"smooth\", %smooth]}\r\n";
    private static final String CMD_BRIGHTNESS_SCENE = "{\"id\":%id,\"method\":\"set_bright\",\"params\":[%value, \"smooth\", %smooth]}\r\n";
    private static final String CMD_COLOR_SCENE = "{\"id\":%id,\"method\":\"set_scene\",\"params\":[\"cf\",1,0,\"100,1,%color,1\"]}\r\n";

    private int mPropCmdId = -1;
    private int mCmdId;
    private Socket mSocket;
    private String mBulbIP;
    private int mBulbPort;
    private int mCurrentSmoothnessValue;
    private ProgressDialog mProgressDialog;
    private SeekBar mBrightness;
    private SeekBar mCT;
    private SeekBar mHue;
    private SeekBar mSaturation;
    private SeekBar mSmoothness;
    private Paint mColor;
    private TextView mLightTitle;
    private TextView mLightPower;
    private TextView mBrightnessValue;
    private TextView mCTValue;
    private TextView mHueValue;
    private TextView mSaturationValue;
    private TextView mSmoothnessValue;
    private CardView mBtnDeviceInfo;
    private ImageButton mBtnOn;
    private ImageButton mBtnOff;
    private Button mBtnChangeColor;
    private BufferedOutputStream mBos;
    private BufferedReader mReader;

    private HashMap<String, String> bulbInfo;

    private final Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_CONNECT_FAILURE:
                    mProgressDialog.dismiss();

                    new AlertDialog.Builder(ControlActivity.this)
                            .setTitle("Connection Failed")
                            .setMessage("Please check your Wifi Connectivity.")
                            .setNeutralButton(android.R.string.ok, (dialog, which) -> finish())
                            .show();

                    break;
                case MSG_CONNECT_SUCCESS:
                    mProgressDialog.dismiss();
                    getProp();
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        getSupportActionBar().setTitle("Light Control");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mBulbIP = getIntent().getStringExtra("ip");
        mBulbPort = Integer.parseInt(getIntent().getStringExtra("port"));

        bulbInfo = (HashMap<String, String>) getIntent().getSerializableExtra("bulbinfo");

        saveRecentDevice(bulbInfo, mBulbIP, mBulbPort);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Connecting...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        mLightTitle = findViewById(R.id.light_title);
        mLightPower = findViewById(R.id.light_power);

        mBrightness = (SeekBar) findViewById(R.id.brightness);
        mHue = (SeekBar) findViewById(R.id.hue);
        mCT = (SeekBar) findViewById(R.id.ct);
        mSaturation = (SeekBar) findViewById(R.id.saturation);
        mSmoothness = (SeekBar) findViewById(R.id.smoothness);

        mColor = new Paint();

        mBrightnessValue = (TextView) findViewById(R.id.brightness_value);
        mHueValue = (TextView) findViewById(R.id.hue_value);
        mCTValue = (TextView) findViewById(R.id.ct_value);
        mSaturationValue = (TextView) findViewById(R.id.saturation_value);
        mSmoothnessValue = (TextView) findViewById(R.id.smoothness_value);

        mCT.setMax(4800);
        mHue.setMax(359);
        mBrightness.setMax(99);
        mSaturation.setMax(100);
        mSmoothness.setMax(97);

        mBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int brightnessVal = seekBar.getProgress() + 1;
                mBrightnessValue.setText(String.valueOf(brightnessVal));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setBrightness(seekBar.getProgress());
            }
        });

        mCT.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int ctVal = seekBar.getProgress() + 1700;
                mCTValue.setText(String.valueOf(ctVal));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setColorTemp(seekBar.getProgress());
            }
        });

        mHue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int hueVal = seekBar.getProgress();
                mHueValue.setText(String.valueOf(hueVal));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int saturationVal = Integer.parseInt(mSaturationValue.getText().toString());

                setHueSaturation(seekBar.getProgress(), saturationVal);
            }
        });

        mSaturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int saturationVal = seekBar.getProgress();
                mSaturationValue.setText(String.valueOf(saturationVal));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int hueVal = Integer.parseInt(mHueValue.getText().toString());

                setHueSaturation(hueVal, seekBar.getProgress());
            }
        });

        mSmoothness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: " + progress);
                int smoothnessVal = progress * 10 + 30;
                mCurrentSmoothnessValue = smoothnessVal;
                mSmoothnessValue.setText(String.valueOf(smoothnessVal));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSmoothness.setProgress(47);

        mBtnDeviceInfo = findViewById(R.id.device_info_btn);
        mBtnOn = findViewById(R.id.btn_on);
        mBtnOff = findViewById(R.id.btn_off);
        mBtnChangeColor = (Button) findViewById(R.id.btn_color_picker);
        
        setOnClickListeners();

        connect();
    }

    private void setOnClickListeners(){
        mBtnDeviceInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                String name = new String(Base64.decode(bulbInfo.get("name"), Base64.DEFAULT));
                String name = mLightTitle.getText().toString();

                String message = "Location: " + bulbInfo.get("Location") + "\n" +
                        "Model: " + bulbInfo.get("model") + "\n" +
                        "ID: " + bulbInfo.get("id") + "\n" +
                        "Firmware Version: " + bulbInfo.get("fw_ver") + "\n" +
                        "Name: " + name;

                AlertDialog.Builder builder = new AlertDialog.Builder(ControlActivity.this);
                builder.setTitle("Device Info");
                builder.setMessage(message);
                builder.setPositiveButton(android.R.string.ok, null);

                builder.show();
            }
        });

        mBtnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write(parseSwitch(true, mCurrentSmoothnessValue));
            }
        });

        mBtnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write(parseSwitch(false, mCurrentSmoothnessValue));
            }
        });

        mBtnChangeColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mColor.setColor(0xFF000000 | 255);

                Log.d(TAG, "onClick: " + mColor.getColor());

                new ColorPickerDialog(ControlActivity.this, color -> {
                    int colorDec = 0xFFFFFF & color;
                    mColor.setColor(color);
                    write(parseColorCmd(colorDec, mCurrentSmoothnessValue));
                    Log.d(TAG, "colorChanged: " + colorDec);
                }, mColor.getColor()).show();
            }
        });

        mBrightnessValue.setOnClickListener( (View v) -> {
            String val = mBrightnessValue.getText().toString();
            EditText editText = getEditText(val,"1-100", "1", "100");
            AlertDialog dialog = showInputAlertDialog("Brightness value", "Enter value between 1-100", editText);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int progress = getDialogValue(dialog, editText, val, "1", "100", mBrightness, -1, 1);
                    setBrightness(progress);
                }
            });
        });

        mCTValue.setOnClickListener( (View v) -> {
            String val = mCTValue.getText().toString();
            EditText editText = getEditText(val,"1700-6500", "1700", "6500");
            AlertDialog dialog = showInputAlertDialog("Color Temperature value", "Enter value between 1700-6500", editText);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int progress = getDialogValue(dialog, editText, val, "1700", "6500", mCT, -1700, 1);
                    setColorTemp(progress);
                }
            });
        });

        mSmoothnessValue.setOnClickListener( (View v) -> {
            String val = mSmoothnessValue.getText().toString();
            EditText editText = getEditText(val,"30-1000", "30", "1000");
            AlertDialog dialog = showInputAlertDialog("Smoothness value", "Enter value between 30-1000 ms", editText);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int progress = getDialogValue(dialog, editText, val, "30", "1000", mSmoothness, -3, 10);
                }
            });
        });

        mHueValue.setOnClickListener( (View v) -> {
            String val = mHueValue.getText().toString();
            EditText editText = getEditText(val,"0-359", "0", "359");
            AlertDialog dialog = showInputAlertDialog("Hue value", "Enter value between 0-359", editText);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int progress = getDialogValue(dialog, editText, val, "0", "359", mHue, 0, 1);
                    int saturationVal = Integer.parseInt(mSaturationValue.getText().toString());
                    setHueSaturation(progress, saturationVal);
                }
            });
        });

        mSaturationValue.setOnClickListener( (View v) -> {
            String val = mSaturationValue.getText().toString();
            EditText editText = getEditText(val,"0-100", "0", "100");
            AlertDialog dialog = showInputAlertDialog("Saturation value", "Enter value between 0-100", editText);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int progress = getDialogValue(dialog, editText, val, "0", "100", mSaturation, 0, 1);
                    int hueVal = Integer.parseInt(mHueValue.getText().toString());
                    setHueSaturation(hueVal, progress);
                }
            });
        });
    }

    private AlertDialog showInputAlertDialog(String title, String message, EditText input){
            LinearLayout parentLayout = new LinearLayout(this);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);

            // call the dimen resource having value in dp: 16dp
            int left = getPixelValue(20);
            int top = getPixelValue(0);
            int right = getPixelValue(20);
            int bottom = getPixelValue(0);

            // this will set the margins
            layoutParams.setMargins(left, top, right, bottom);

            input.setLayoutParams(layoutParams);
            parentLayout.addView(input);

        return new AlertDialog.Builder(ControlActivity.this)
                    .setTitle(title)
                    .setMessage(message)
                    .setView(parentLayout)
                    .setPositiveButton(android.R.string.ok, null).show();
    }

    private EditText getEditText(String value, String hint, String min, String max){
        int minVal = Integer.parseInt(min);

        if (minVal > 1){
            min = "1";
        }

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setRawInputType(Configuration.KEYBOARD_12KEY);
        input.setHint(hint);
        input.setFilters( new InputFilter[]{ new MinMaxFilter(min, max)}) ;
        input.setText(value);

        return input;
    }

    private int getDialogValue(AlertDialog dialog, EditText input, String value, String min, String max, SeekBar seekBar, int offset, int mul){
        int minVal = Integer.parseInt(min);
        int maxVal = Integer.parseInt(max);

        String inputStr = input.getText().toString();

        if (inputStr.isEmpty()){
            input.setText(value);
            return -1;
        }

        int inputVal = Integer.parseInt(inputStr);
        int progress = Integer.parseInt(value);

        Log.d(TAG, "onClick: Progress: " + progress);

        if (inputVal < minVal){
            Log.d(TAG, "onClick: MIN");
            progress = minVal;
        } else if (inputVal > maxVal) {
            Log.d(TAG, "onClick: MAX");
            progress = maxVal;
        } else {
            progress = inputVal;
        }

        input.setText(String.valueOf(progress));

        int actualProgress = progress/mul + offset;

        Log.d(TAG, "onClick: " + progress);

        seekBar.setProgress(actualProgress);

        dialog.dismiss();

        return progress + offset;
    }

    private void setBrightness(int value){
        write(parseBrightnessCmd(value + 1, mCurrentSmoothnessValue));
    }

    private void setColorTemp(int value){
        write(parseCTCmd(value + 1700, mCurrentSmoothnessValue));
    }

    private void setHueSaturation(int hueVal, int satVal){
        write(parseHSVCmd(hueVal, satVal, mCurrentSmoothnessValue));
    }

    private void saveRecentDevice(HashMap<String, String> bulbInfo, String ip, int port) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_recent_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(getString(R.string.preference_recent_firmware_key), bulbInfo.get("fw_ver"));
        editor.putString(getString(R.string.preference_recent_id_key), bulbInfo.get("id"));
        editor.putString(getString(R.string.preference_recent_name_key), bulbInfo.get("name"));
        editor.putString(getString(R.string.preference_recent_model_key), bulbInfo.get("model"));
        editor.putString(getString(R.string.preference_recent_location_key), bulbInfo.get("Location"));
        editor.putString(getString(R.string.preference_recent_ip_key), ip);
        editor.putInt(getString(R.string.preference_recent_port_key), port);

        editor.apply();

        Log.d(TAG, "saveRecentDevice: Saved Current Device to Preference");
    }

    private void getProp(){
        write(parseGetPropCmd());
        mPropCmdId = mCmdId;
    }

    private boolean cmd_run = true;
    private void connect(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                    if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
                        cmd_run = true;
                        mSocket = new Socket(mBulbIP, mBulbPort);
                        mSocket.setKeepAlive(true);
                        mBos = new BufferedOutputStream(mSocket.getOutputStream());
                        mHandler.sendEmptyMessage(MSG_CONNECT_SUCCESS);
                        mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                        while (cmd_run) {
                            try {
                                String value = mReader.readLine();
                                Log.d(TAG, "value = " + value);

                                JSONObject resultJson = new JSONObject(value);

                                if (resultJson.has("method") && resultJson.getString("method").equals("props")) {
                                    runOnUiThread(() -> {
                                        updateProps(resultJson);
                                    });
                                }

                                if (resultJson.has("id") && resultJson.getInt("id") == mPropCmdId) {
//                                    Log.d(TAG, "run: " + resultJson);
                                    String power = resultJson.getJSONArray("result").getString(0);
                                    int currBrightness = resultJson.getJSONArray("result").getInt(1);
                                    int currCT = resultJson.getJSONArray("result").getInt(2);
                                    int currColor = resultJson.getJSONArray("result").getInt(3);
                                    int currHue = resultJson.getJSONArray("result").getInt(4);
                                    int currSaturation = resultJson.getJSONArray("result").getInt(5);
                                    String name = resultJson.getJSONArray("result").getString(6);

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateName(name);
                                            updatePower(power);
                                        }
                                    });


                                    updateBrightness(currBrightness);
                                    updateCT(currCT);
                                    updateColor(currColor);
                                    updateHue(currHue);
                                    updateSaturation(currSaturation);

                                    Log.d(TAG, "run: Got current prop");
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "connect() -> Thread -> run: " + e);
                            }

                        }
                    } else {
                        Log.d(TAG, "run: Connection Failed, Wifi Not Connected");
                        mHandler.sendEmptyMessage(MSG_CONNECT_FAILURE);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(MSG_CONNECT_FAILURE);
                }
            }
        }).start();
    }

    private void updateProps(JSONObject resultJson) {
        try {
            JSONObject params = resultJson.getJSONObject("params");
            if(params.has("name")){ updateName(params.getString("name")); }
            if(params.has("power")){ updatePower(params.getString("power")); }
            if(params.has("ct")){ updateCT(params.getInt("ct")); }
            if(params.has("rgb")){ updateColor(params.getInt("rgb")); }
            if(params.has("bright")){ updateBrightness(params.getInt("bright")); }
            if(params.has("hue")){ updateHue(params.getInt("hue")); }
            if(params.has("sat")){ updateSaturation(params.getInt("sat")); }
//            if(params.has("smooth")){ updateSaturation(params.getInt("smooth")); }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updatePower(String power) {
        mLightPower.setText(power.toUpperCase());

        if (power.toUpperCase().contains("ON")){
            mLightPower.setTextColor(0xFF32CB00);
            mBtnOn.setVisibility(View.GONE);
            mBtnOff.setVisibility(View.VISIBLE);
        } else {
            mLightPower.setTextColor(Color.GRAY);
            mBtnOn.setVisibility(View.VISIBLE);
            mBtnOff.setVisibility(View.GONE);
        }
    }

    private void updateName(String name) { mLightTitle.setText(new String(Base64.decode(name, Base64.DEFAULT))); }
    private void updateCT(int ct) { mCT.setProgress(ct-1700); }
    private void updateColor(int color) { mColor.setColor(0xFF000000 | color); }
    private void updateBrightness(int brightness) { mBrightness.setProgress(brightness-1); }
    private void updateHue(int hue) { mHue.setProgress(hue); }
    private void updateSaturation(int saturation) { mSaturation.setProgress(saturation); }
    private void updateSmoothness(int smoothness) { mSmoothness.setProgress(smoothness); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            cmd_run = false;
            if (mSocket!=null)
                mSocket.close();
        } catch (Exception e){
            Log.d(TAG, "onDestroy: Error: " + e);
        }
    }

    private String parseSwitch(boolean on, int smoothness){
        String cmd;
        if (on){
            cmd = CMD_ON.replace("%id", String.valueOf(++mCmdId)).replace("%smooth", String.valueOf(smoothness));
        }else {
            cmd = CMD_OFF.replace("%id", String.valueOf(++mCmdId)).replace("%smooth", String.valueOf(smoothness));
        }
        return cmd;
    }

    private String parseCTCmd(int ct, int smoothness){
        return CMD_CT.replace("%id",String.valueOf(++mCmdId))
                .replace("%value",String.valueOf(ct))
                .replace("%smooth", String.valueOf(smoothness));
    }

    private String parseHSVCmd(int hue, int saturation, int smoothness){
        return CMD_HSV.replace("%id",String.valueOf(++mCmdId))
                .replace("%hue",String.valueOf(hue))
                .replace("%saturation",String.valueOf(saturation))
                .replace("%smooth", String.valueOf(smoothness));
    }

    private String parseBrightnessCmd(int brightness, int smoothness){
        return CMD_BRIGHTNESS.replace("%id", String.valueOf(++mCmdId))
                .replace("%value", String.valueOf(brightness))
                .replace("%smooth", String.valueOf(smoothness));
    }

    private String parseColorCmd(int color, int smoothness){
        return CMD_COLOR.replace("%id",String.valueOf(++mCmdId))
                .replace("%value",String.valueOf(color))
                .replace("%smooth", String.valueOf(smoothness));
    }

    private String parseGetPropCmd(){
        return CMD_GET_PROP.replace("%id",String.valueOf(++mCmdId));
    }

    private void write(String cmd){
        Log.d(TAG, "write: cmd: " + cmd);
        new Thread(() -> {
            if (mBos != null && mSocket.isConnected()) {
                try {
                    mBos.write(cmd.getBytes());
                    mBos.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "mBos = null or mSocket is closed");
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_about:
//                Toast.makeText(this, "Developed By Asswad Sarker Nomaan", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ControlActivity.this, AboutActivity.class);
                startActivity(intent);

                return true;
            case R.id.menu_item_exit:
                this.finishAffinity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* Utils */
    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    private int getPixelValue(int dp) {
        Resources resources = getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dp, resources.getDisplayMetrics());
    }

    public class MinMaxFilter implements InputFilter {
        private int mIntMin , mIntMax ;
        public MinMaxFilter ( int minValue , int maxValue) {
            this . mIntMin = minValue ;
            this . mIntMax = maxValue ;
        }
        public MinMaxFilter (String minValue , String maxValue) {
            this . mIntMin = Integer. parseInt (minValue) ;
            this . mIntMax = Integer. parseInt (maxValue) ;
        }
        @Override
        public CharSequence filter (CharSequence source , int start , int end , Spanned dest , int dstart , int dend) {
            try {
                int input = Integer. parseInt (dest.toString() + source.toString()) ;
                if (isInRange( mIntMin , mIntMax , input))
                    return null;
            } catch (NumberFormatException e) {
                e.printStackTrace() ;
            }
            return "";
        }
        private boolean isInRange ( int a , int b , int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a ;
        }
    }
}