package me.asswad.myyeelightlan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import android.net.wifi.WifiManager.MulticastLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN_ACTIVITY";
    private static final int MSG_SHOWLOG = 0;
    private static final int MSG_FOUND_DEVICE = 1;
    private static final int MSG_DISCOVER_FINISH = 2;
    private static final int MSG_STOP_SEARCH = 3;

    private static final String UDP_HOST = "239.255.255.250";
    private static final int UDP_PORT = 1982;
    private static final String message = "M-SEARCH * HTTP/1.1\r\n" +
            "HOST:239.255.255.250:1982\r\n" +
            "MAN:\"ssdp:discover\"\r\n" +
            "ST:wifi_bulb\r\n"; //String to send
    private DatagramSocket mDSocket;
    private boolean mSeraching = true;
    private ListView mListView;
    private MyAdapter mAdapter;
    List<HashMap<String, String>> mDeviceList = new ArrayList<HashMap<String, String>>();
    private TextView mTextView;
    private TextView mRecentTitleTV;
    private TextView mRecentLocationTV;
    private TextView mRecentModelTV;
    private LinearLayout mRecentLayout;
    private CardView mRecentCard;
    private ImageButton mBtnSearch;

    private String recentDeviceID;
    private String recentDeviceName;
    private String recentDeviceFirmware;
    private String recentDeviceLocation;
    private String recentDeviceModel;

    private ProgressBar scanningSpinner;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_FOUND_DEVICE:
                case MSG_DISCOVER_FINISH:
                    mAdapter.notifyDataSetChanged();
                    scanningSpinner.setVisibility(View.GONE);
                    mBtnSearch.setVisibility(View.VISIBLE);
                    break;
                case MSG_SHOWLOG:
                    Toast.makeText(MainActivity.this, "" + msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case MSG_STOP_SEARCH:
                    mSearchThread.interrupt();
                    mAdapter.notifyDataSetChanged();
                    mSeraching = false;
                    scanningSpinner.setVisibility(View.GONE);
                    mBtnSearch.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };
    private MulticastLock multicastLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setTitle("Welcome");

        WifiManager wm = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = wm.createMulticastLock("test");
        multicastLock.acquire();

        mTextView = (TextView) findViewById(R.id.infotext);
        mRecentTitleTV = (TextView) findViewById(R.id.recent_light_title);
        mRecentLocationTV = (TextView) findViewById(R.id.recent_light_location);
        mRecentModelTV = (TextView) findViewById(R.id.recent_light_model);

        mRecentLayout = findViewById(R.id.recent_device_layout);
        mRecentCard = (CardView) findViewById(R.id.recent_light_card);

        mBtnSearch = findViewById(R.id.btn_search);

        scanningSpinner = findViewById(R.id.device_scan_progress);

        mBtnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchDevice();
            }
        });

        mListView = (ListView) findViewById(R.id.deviceList);
        mAdapter = new MyAdapter(this);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> bulbInfo = mDeviceList.get(position);
                Intent intent = new Intent(MainActivity.this, ControlActivity.class);
                String ipinfo = bulbInfo.get("Location").split("//")[1];
                String ip = ipinfo.split(":")[0];
                String port = ipinfo.split(":")[1];
                intent.putExtra("bulbinfo", bulbInfo);
                intent.putExtra("ip", ip);
                intent.putExtra("port", port);
                startActivity(intent);
            }
        });

        mRecentCard.setOnClickListener(v -> {
            HashMap<String, String> bulbInfo = new HashMap<String, String>();
            bulbInfo.put("id", recentDeviceID);
            bulbInfo.put("name", recentDeviceName);
            bulbInfo.put("fw_ver", recentDeviceFirmware);
            bulbInfo.put("model", recentDeviceModel);
            bulbInfo.put("Location", recentDeviceLocation);

            Intent intent = new Intent(MainActivity.this, ControlActivity.class);
            String ipinfo = bulbInfo.get("Location").split("//")[1];
            String ip = ipinfo.split(":")[0];
            String port = ipinfo.split(":")[1];
            intent.putExtra("bulbinfo", bulbInfo);
            intent.putExtra("ip", ip);
            intent.putExtra("port", port);
            startActivity(intent);
        });

        mRecentCard.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete recent device?")
                        .setMessage("Are you want to delete the recent device?")
                        .setNeutralButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> clearRecentDevice())
                        .show();

                return true;
            }
        });

        configDevice();
    }

    private void clearRecentDevice() {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_recent_key), Context.MODE_PRIVATE);
        sharedPref.edit().clear().apply();

        mRecentLayout.setVisibility(View.GONE);

        Toast.makeText(this, "Removed from recent device.", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "clearRecentDevice: Cleared Recent Device from Preferences");
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
            case R.id.menu_item_help:
                Intent intent = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_about:
//                Toast.makeText(this, "Developed By Asswad Sarker Nomaan", Toast.LENGTH_SHORT).show();
                intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_exit:
                this.finishAffinity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private Thread mSearchThread = null;
    private void searchDevice() {
        scanningSpinner.setVisibility(View.VISIBLE);
        mBtnSearch.setVisibility(View.GONE);

        mDeviceList.clear();
        mAdapter.notifyDataSetChanged();
        mSeraching = true;
        mSearchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mDSocket = new DatagramSocket();
                    DatagramPacket dpSend = new DatagramPacket(message.getBytes(),
                            message.getBytes().length, InetAddress.getByName(UDP_HOST),
                            UDP_PORT);
                    mDSocket.send(dpSend);
                    mHandler.sendEmptyMessageDelayed(MSG_STOP_SEARCH,2000);
                    while (mSeraching) {
                        byte[] buf = new byte[1024];
                        DatagramPacket dpRecv = new DatagramPacket(buf, buf.length);
                        mDSocket.receive(dpRecv);
                        byte[] bytes = dpRecv.getData();
                        StringBuffer buffer = new StringBuffer();
                        for (int i = 0; i < dpRecv.getLength(); i++) {
                            // parse /r
                            if (bytes[i] == 13) {
                                continue;
                            }
                            buffer.append((char) bytes[i]);
                        }
                        Log.d("socket", "got message:" + buffer.toString());
                        if (!buffer.toString().contains("yeelight")) {
                            mHandler.obtainMessage(MSG_SHOWLOG, "Received a message, not Yeelight bulb").sendToTarget();
                            return;
                        }
                        String[] infos = buffer.toString().split("\n");
                        HashMap<String, String> bulbInfo = new HashMap<String, String>();
                        for (String str : infos) {
                            int index = str.indexOf(":");
                            if (index == -1) {
                                continue;
                            }
                            String title = str.substring(0, index);
                            String value = str.substring(index + 1);
                            bulbInfo.put(title, value);
                        }
                        if (!hasAdd(bulbInfo)){
                            mDeviceList.add(bulbInfo);
                        }

                    }
                    mHandler.sendEmptyMessage(MSG_DISCOVER_FINISH);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "run: Error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Connection failed!'", Toast.LENGTH_SHORT).show();
                            scanningSpinner.setVisibility(View.GONE);
                            mBtnSearch.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
        mSearchThread.start();

    }

    private boolean mNotify = true;
    @Override
    protected void onResume() {
        configDevice();

        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //DatagramSocket socket = new DatagramSocket(UDP_PORT);
                    InetAddress group = InetAddress.getByName(UDP_HOST);
                    MulticastSocket socket = new MulticastSocket(UDP_PORT);
                    socket.setLoopbackMode(true);
                    socket.joinGroup(group);
                    Log.d(TAG, "join success");
                    mNotify = true;
                    while (mNotify){
                        byte[] buf = new byte[1024];
                        DatagramPacket receiveDp = new DatagramPacket(buf,buf.length);
                        Log.d(TAG, "waiting device....");
                        socket.receive(receiveDp);
                        byte[] bytes = receiveDp.getData();
                        StringBuffer buffer = new StringBuffer();
                        for (int i = 0; i < receiveDp.getLength(); i++) {
                            // parse /r
                            if (bytes[i] == 13) {
                                continue;
                            }
                            buffer.append((char) bytes[i]);
                        }
                        if (!buffer.toString().contains("yeelight")){
                            Log.d(TAG,"Listener receive msg:" + buffer.toString()+" but not a response");
                            return;
                        }
                        String[] infos = buffer.toString().split("\n");
                        HashMap<String, String> bulbInfo = new HashMap<String, String>();
                        for (String str : infos) {
                            int index = str.indexOf(":");
                            if (index == -1) {
                                continue;
                            }
                            String title = str.substring(0, index);
                            String value = str.substring(index + 1);
                            Log.d(TAG, "title = " + title + " value = " + value);
                            bulbInfo.put(title, value);
                        }
                        if (!hasAdd(bulbInfo)){
                            mDeviceList.add(bulbInfo);
                        }
                        mHandler.sendEmptyMessage(MSG_FOUND_DEVICE);
                        Log.d(TAG, "get message:" + buffer.toString());
                    }
                }catch (Exception e){
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Connection failed!'", Toast.LENGTH_SHORT).show();
                            scanningSpinner.setVisibility(View.GONE);
                            mBtnSearch.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNotify = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        multicastLock.release();
    }

    private class MyAdapter extends BaseAdapter {

        private LayoutInflater mLayoutInflater;
        private int mLayoutResource;

        public MyAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
//            mLayoutResource = android.R.layout.simple_list_item_2;
            mLayoutResource = R.layout.device_list_item;
        }

        @Override
        public int getCount() {
            return mDeviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDeviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            HashMap<String, String> data = (HashMap<String, String>) getItem(position);
            if (convertView == null) {
                view = mLayoutInflater.inflate(mLayoutResource, parent, false);
            } else {
                view = convertView;
            }


            String device_name = "Yeelight";

            try {
                device_name = new String(Base64.decode(data.get("name"), Base64.DEFAULT));
            } catch (IllegalArgumentException exception){
                Log.d(TAG, "getView: " + exception.getMessage());
                if (!data.get("name").isEmpty()){
                    device_name = data.get("name");
                }
            }

            TextView textTitle = (TextView) view.findViewById(R.id.device_title);
            textTitle.setText(device_name);

            TextView textView = (TextView) view.findViewById(R.id.device_model);
            textView.setText("Model:" + data.get("model") );

            Log.d(TAG, "name = " + textView.getText().toString());
            TextView textSub = (TextView) view.findViewById(R.id.device_location);
            textSub.setText("Location:" + data.get("Location"));
            return view;
        }
    }
    private boolean hasAdd(HashMap<String,String> bulbinfo){
        for (HashMap<String,String> info : mDeviceList){
            Log.d(TAG, "location params = " + bulbinfo.get("Location"));
            if (info.get("Location").equals(bulbinfo.get("Location"))){
                return true;
            }
        }
        return false;
    }

    private void configDevice(){
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_recent_key), Context.MODE_PRIVATE);
        if(sharedPref.contains(getString(R.string.preference_recent_location_key))){
            recentDeviceID = sharedPref.getString(getString(R.string.preference_recent_id_key), "0");
            recentDeviceName = sharedPref.getString(getString(R.string.preference_recent_name_key), "Yeelight");
            recentDeviceFirmware = sharedPref.getString(getString(R.string.preference_recent_firmware_key), "0");
            recentDeviceLocation = sharedPref.getString(getString(R.string.preference_recent_location_key), "yeelight://192.168.1.14:55443");
            recentDeviceModel = sharedPref.getString(getString(R.string.preference_recent_model_key), "color");

            String device_name = "Yeelight";

            try {
                device_name = new String(Base64.decode(recentDeviceName, Base64.DEFAULT));
            } catch (IllegalArgumentException exception){
                Log.d(TAG, "configDevice: " + exception.getMessage());
                if (!recentDeviceName.isEmpty()){
                    device_name = recentDeviceName;
                }
            }

            mRecentTitleTV.setText(device_name);
            mRecentLocationTV.setText("Location:" + recentDeviceLocation);
            mRecentModelTV.setText("Model:" + recentDeviceModel);

            mRecentLayout.setVisibility(View.VISIBLE);
        }
    }

}