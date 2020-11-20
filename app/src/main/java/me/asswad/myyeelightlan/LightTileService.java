package me.asswad.myyeelightlan;

import android.app.ProgressDialog;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

@RequiresApi(api = Build.VERSION_CODES.N)
public class LightTileService extends TileService {

    private String TAG = "TileService";

    private int mPropCmdId=-1;
    private int mCmdId;
    private Socket mSocket;
    private String mBulbIP = "192.168.1.14";
    private int mBulbPort = 55443;
    private BufferedOutputStream mBos;
    private BufferedReader mReader;

    private static final int MSG_CONNECT_SUCCESS = 0;
    private static final int MSG_CONNECT_FAILURE = 1;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_CONNECT_FAILURE:
                    break;
                case MSG_CONNECT_SUCCESS:
                    Log.d(TAG, "handleMessage: Connected");
                    turnOn();
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground));
        tile.setLabel("Light On");
        tile.setContentDescription("Turn On Yeelight");
//        tile.setState(Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        connect();
    }

    private void turnOn(){
        String CMD_ON = "{\"id\":1,\"method\":\"set_power\",\"params\":[\"on\",\"smooth\",500]}\r\n" ;
        String cmd = CMD_ON;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mBos != null && mSocket.isConnected()){
                    try {
                        mBos.write(cmd.getBytes());
                        mBos.flush();
                        Log.d(TAG, "run: Turn On CMD Sent");
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    closeConn();
                } else {
                    Log.d(TAG,"mBos = null or mSocket is closed");
                }
            }
        }).start();
    }

    private void closeConn() {
        try{
            if (mSocket!=null) {
                mSocket.close();
                Log.d(TAG, "closeConn: Connection Closed");
            }
        } catch (Exception e){

        }
    }


    private void connect(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "run: Connecting");

                    mSocket = new Socket(mBulbIP, mBulbPort);
                    mSocket.setKeepAlive(true);
                    mBos = new BufferedOutputStream(mSocket.getOutputStream());
                    mHandler.sendEmptyMessage(MSG_CONNECT_SUCCESS);
                    mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                    try {
                        String value = mReader.readLine();
                        Log.d(TAG, "value = "+value);
//
//                        JSONObject resultJson = new JSONObject(value);
//                        if (resultJson.getInt("id") == mPropCmdId){
//                            int currBrightness = resultJson.getJSONArray("result").getInt(1);
//                            int currCT = resultJson.getJSONArray("result").getInt(2);
//                            int currHue = resultJson.getJSONArray("result").getInt(4);
//
//                            Log.d(TAG, "run: Got current prop");
//                        }
                    }catch (Exception e){

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(MSG_CONNECT_FAILURE);
                }
            }
        }).start();
    }
}
