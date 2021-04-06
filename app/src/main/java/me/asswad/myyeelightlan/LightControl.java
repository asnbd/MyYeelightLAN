package me.asswad.myyeelightlan;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatViewInflater;

import java.io.BufferedOutputStream;
import java.net.Socket;

public class LightControl {
    private final String TAG = "LightControl";

    private int mCmdId = 93564;
    private Socket mSocket;
    private String mBulbIP;
    private int mBulbPort;
    private BufferedOutputStream mBos;

    private final Context context;

    private static final int ACTION_TURN_OFF = 0;
    private static final int ACTION_TURN_ON = 1;
    private static final int ACTION_TOGGLE = 2;
    private static final int ACTION_BRIGHT = 3;
    private static final int ACTION_DIM = 4;
    private static final int ACTION_MEDIUM = 5;

    private static final int MSG_CONNECT_SUCCESS = 700;
    private static final int MSG_CONNECT_FAILURE = 701;

    private static final String CMD_ON = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"on\",\"smooth\",500]}\r\n" ;
    private static final String CMD_OFF = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"off\",\"smooth\",500]}\r\n" ;
    private static final String CMD_TOGGLE = "{\"id\":%id,\"method\":\"toggle\",\"params\":[]}\r\n" ;
    private static final String CMD_BRIGHT = "{\"id\":%id,\"method\":\"set_bright\",\"params\":[100, \"smooth\", 500]}\r\n";
    private static final String CMD_MEDIUM = "{\"id\":%id,\"method\":\"set_bright\",\"params\":[50, \"smooth\", 500]}\r\n";
    private static final String CMD_DIM = "{\"id\":%id,\"method\":\"set_bright\",\"params\":[1, \"smooth\", 500]}\r\n";

    private final Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_CONNECT_FAILURE:
                    Toast.makeText(context, "Wifi Not Connected!", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public LightControl(Context context){
        this.context = context;
    }

    public void turnOn(){ connect(ACTION_TURN_ON); }

    public void turnOff(){ connect(ACTION_TURN_OFF); }

    public void toggle(){ connect(ACTION_TOGGLE); }

    public void bright(){ connect(ACTION_BRIGHT); }

    public void medium(){ connect(ACTION_MEDIUM); }

    public void dim(){ connect(ACTION_DIM); }

    private void connect(int action){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "run: Connecting");

                    if(!configDevice()){
                        Log.d(TAG, "run: IP Not Set");
                        return;
                    }

                    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
                        mSocket = new Socket(mBulbIP, mBulbPort);

                        mSocket.setKeepAlive(true);
                        mBos = new BufferedOutputStream(mSocket.getOutputStream());
                        Log.d(TAG, "run: Connection Success");

                        handleAction(action);
                    } else {
                        mHandler.sendEmptyMessage(MSG_CONNECT_FAILURE);
                        Log.d(TAG, "run: Connection Failed, Wifi Not Connected");
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "run: Connection Failed");
                }
            }
        }).start();
    }

    private void handleAction(int action) {
        switch (action){
            case ACTION_TURN_OFF:
                Log.d(TAG, "handleAction: Sending Turn Off Command");
                sendCommand(parseCmd(CMD_OFF));
                break;
            case ACTION_TURN_ON:
                Log.d(TAG, "handleAction: Sending Turn On Command");
                sendCommand(parseCmd(CMD_ON));
                break;
            case ACTION_TOGGLE:
                Log.d(TAG, "handleAction: Sending Toggle Command");
                sendCommand(parseCmd(CMD_TOGGLE));
                break;
            case ACTION_BRIGHT:
                Log.d(TAG, "handleAction: Sending Bright Command");
                sendCommand(parseCmd(CMD_BRIGHT));
                break;
            case ACTION_MEDIUM:
                Log.d(TAG, "handleAction: Sending Medium Command");
                sendCommand(parseCmd(CMD_MEDIUM));
                break;
            case ACTION_DIM:
                Log.d(TAG, "handleAction: Sending Dim Command");
                sendCommand(parseCmd(CMD_DIM));
                break;
        }
    }

    private void sendCommand(String cmd){
        if (mBos != null && mSocket.isConnected()){
            try {
                mBos.write(cmd.getBytes());
                mBos.flush();
                Log.d(TAG, "sendCommand: Command Sent");
            }catch (Exception e){
                e.printStackTrace();
            }

            closeConn();
        } else {
            Log.d(TAG,"mBos = null or mSocket is closed");
        }
    }

    private String parseCmd(String cmd){
        return cmd.replace("%id", String.valueOf(++mCmdId));
    }

    private void closeConn() {
        try{
            if (mSocket!=null) {
                mSocket.close();
                Log.d(TAG, "closeConn: Connection Closed");
            }
        } catch (Exception e){
            Log.d(TAG, "closeConn: Exception");
        }
    }

    private boolean configDevice(){
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_recent_key), Context.MODE_PRIVATE);
        if(sharedPref.contains(context.getString(R.string.preference_recent_ip_key))){
            mBulbIP = sharedPref.getString(context.getString(R.string.preference_recent_ip_key), "192.168.1.14");
            mBulbPort = sharedPref.getInt(context.getString(R.string.preference_recent_port_key), 55443);
            return true;
        }

        return false;
    }
}
