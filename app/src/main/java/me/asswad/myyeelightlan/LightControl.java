package me.asswad.myyeelightlan;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.net.Socket;

public class LightControl {
    private String TAG = "LightControl";

    private int mCmdId = 93564;
    private Socket mSocket;
    private String mBulbIP = "192.168.1.14";
    private int mBulbPort = 55443;
    private BufferedOutputStream mBos;

    private static final int ACTION_TURN_ON = 1;
    private static final int ACTION_TURN_OFF = 0;

    private static final String CMD_ON = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"on\",\"smooth\",500]}\r\n" ;
    private static final String CMD_OFF = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"off\",\"smooth\",500]}\r\n" ;

    public void turnOn(){
        connect(ACTION_TURN_ON);
    }

    public void turnOff(){
        connect(ACTION_TURN_OFF);
    }

    private void connect(int action){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "run: Connecting");

                    mSocket = new Socket(mBulbIP, mBulbPort);

                    mSocket.setKeepAlive(true);
                    mBos = new BufferedOutputStream(mSocket.getOutputStream());
                    Log.d(TAG, "run: Connection Success");

                    handleAction(action);
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

        }
    }
}
