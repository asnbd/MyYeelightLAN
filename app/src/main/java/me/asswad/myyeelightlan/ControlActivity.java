package me.asswad.myyeelightlan;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class ControlActivity extends AppCompatActivity {

    private String TAG = "Control";

    private static final int MSG_CONNECT_SUCCESS = 0;
    private static final int MSG_CONNECT_FAILURE = 1;
    private static final String CMD_TOGGLE = "{\"id\":%id,\"method\":\"toggle\",\"params\":[]}\r\n" ;
    private static final String CMD_GET_PROP = "{\"id\":%id,\"method\":\"get_prop\",\"params\":[\"power\",\"bright\",\"ct\",\"rgb\",\"hue\",\"sat\",\"name\"]}\r\n" ;
    private static final String CMD_ON = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"on\",\"smooth\",500]}\r\n" ;
    private static final String CMD_OFF = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"off\",\"smooth\",500]}\r\n" ;
    private static final String CMD_CT = "{\"id\":%id,\"method\":\"set_ct_abx\",\"params\":[%value, \"smooth\", 500]}\r\n";
    private static final String CMD_HSV = "{\"id\":%id,\"method\":\"set_hsv\",\"params\":[%value, 100, \"smooth\", 200]}\r\n";
    private static final String CMD_BRIGHTNESS = "{\"id\":%id,\"method\":\"set_bright\",\"params\":[%value, \"smooth\", 200]}\r\n";
    private static final String CMD_BRIGHTNESS_SCENE = "{\"id\":%id,\"method\":\"set_bright\",\"params\":[%value, \"smooth\", 500]}\r\n";
    private static final String CMD_COLOR_SCENE = "{\"id\":%id,\"method\":\"set_scene\",\"params\":[\"cf\",1,0,\"100,1,%color,1\"]}\r\n";

    private int mPropCmdId=-1;
    private int mCmdId;
    private Socket mSocket;
    private String mBulbIP;
    private int mBulbPort;
    private ProgressDialog mProgressDialog;
    private SeekBar mBrightness;
    private SeekBar mCT;
    private SeekBar mHue;
    private TextView mBrightnessValue;
    private TextView mCTValue;
    private TextView mHueValue;
    private Button mBtnOn;
    private Button mBtnOff;
    private Button mBtnMusic;
    private BufferedOutputStream mBos;
    private BufferedReader mReader;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_CONNECT_FAILURE:
                    mProgressDialog.dismiss();
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

        // Remove restriction "always run network operations on a thread or as an asynchronous task" and override the default behavior.
        // To avoid android.os.NetworkOnMainThreadException
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mBulbIP = getIntent().getStringExtra("ip");
        mBulbPort = Integer.parseInt(getIntent().getStringExtra("port"));
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Connecting...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        mBrightness = (SeekBar) findViewById(R.id.brightness);
        mHue = (SeekBar) findViewById(R.id.hue);
        mCT = (SeekBar) findViewById(R.id.ct);

        mBrightnessValue = (TextView) findViewById(R.id.brightness_value);
        mHueValue = (TextView) findViewById(R.id.hue_value);
        mCTValue = (TextView) findViewById(R.id.ct_value);

        mCT.setMax(4800);
        mHue.setMax(359);
        mBrightness.setMax(99);



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
                int brightnessVal = seekBar.getProgress() + 1;
                write(parseBrightnessCmd(brightnessVal));
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
                int ctVal = seekBar.getProgress() + 1700;
                write(parseCTCmd(ctVal));
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
                int hueVal = seekBar.getProgress();
                write(parseHSVCmd(hueVal));
            }
        });
        mBtnOn = (Button) findViewById(R.id.btn_on);
        mBtnOff = (Button) findViewById(R.id.btn_off);
        mBtnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write(parseSwitch(true));
            }
        });
        mBtnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write(parseSwitch(false));
            }
        });
        connect();
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
                    cmd_run = true;
                    mSocket = new Socket(mBulbIP, mBulbPort);
                    mSocket.setKeepAlive(true);
                    mBos= new BufferedOutputStream(mSocket.getOutputStream());
                    mHandler.sendEmptyMessage(MSG_CONNECT_SUCCESS);
                    mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    while (cmd_run){
                        try {
                            String value = mReader.readLine();
                            Log.d(TAG, "value = "+value);

                            JSONObject resultJson = new JSONObject(value);
                            if (resultJson.getInt("id") == mPropCmdId){
                                int currBrightness = resultJson.getJSONArray("result").getInt(1);
                                int currCT = resultJson.getJSONArray("result").getInt(2);
                                int currHue = resultJson.getJSONArray("result").getInt(4);


                                mBrightness.setProgress(currBrightness-1);
                                mCT.setProgress(currCT-1700);
                                mHue.setProgress(currHue);

                                mBrightnessValue.setText(String.valueOf(currBrightness));
                                mCTValue.setText(String.valueOf(currCT));
                                mHueValue.setText(String.valueOf(currHue));

                                Log.d(TAG, "run: Got current prop");
                            }
                        }catch (Exception e){

                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(MSG_CONNECT_FAILURE);
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            cmd_run = false;
            if (mSocket!=null)
                mSocket.close();
        }catch (Exception e){

        }

    }
    private String parseSwitch(boolean on){
        String cmd;
        if (on){
            cmd = CMD_ON.replace("%id", String.valueOf(++mCmdId));
        }else {
            cmd = CMD_OFF.replace("%id", String.valueOf(++mCmdId));
        }
        return cmd;
    }
    private String parseCTCmd(int ct){
        return CMD_CT.replace("%id",String.valueOf(++mCmdId)).replace("%value",String.valueOf(ct));
    }
    private String parseHSVCmd(int color){
        return CMD_HSV.replace("%id",String.valueOf(++mCmdId)).replace("%value",String.valueOf(color));
    }
    private String parseBrightnessCmd(int brightness){
        return CMD_BRIGHTNESS.replace("%id",String.valueOf(++mCmdId)).replace("%value",String.valueOf(brightness));
    }
    private String parseGetPropCmd(){
        return CMD_GET_PROP.replace("%id",String.valueOf(++mCmdId));
    }
    private void write(String cmd){
        if (mBos != null && mSocket.isConnected()){
            try {
                mBos.write(cmd.getBytes());
                mBos.flush();
            }catch (Exception e){
                e.printStackTrace();
            }
        } else {
            Log.d(TAG,"mBos = null or mSocket is closed");
        }
    }
}