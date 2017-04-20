package cn.triplez.materialdesigndemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntegerRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class NavActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    BluetoothSPP bt;
    ToggleButton pm_device;
    TextView bt_strength, pm_number;
    private Thread thread;
    private Handler handler;
    ProgressBar pb;
    byte number = 0;
    int counter = 0;
    String s = "";
    int pm_val;
    boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        pm_number = (TextView)findViewById(R.id.pm_number);
        pm_device = (ToggleButton) findViewById(R.id.pm_device);
        bt_strength = (TextView) findViewById(R.id.bt_strength);
        pb = (ProgressBar)findViewById(R.id.pb);

        // Progress Bar invisble.
        pb.setVisibility(View.INVISIBLE);
        // Registering Broadcast. this will fire when Bluetooth device Found
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));


        // Floating action button
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.fab_mesg, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                testSend();
            }
        });

        // Drawer action
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Bluetooth
        bt = new BluetoothSPP(this);
        Intent intent;

        // Init Bluetooth.
        if(!bt.isBluetoothAvailable()){
            Log.d("BT", "BT is not available!");
            Toast.makeText(getApplicationContext(), R.string.bluetooth_not_avi, Toast.LENGTH_SHORT).show();
            pm_device.setChecked(false);
            pm_device.setText(R.string.connect_device);
            pb.setVisibility(View.INVISIBLE);
        } else{
            pm_device.setChecked(true);
            pm_device.setText(R.string.main_location);
            pb.setVisibility(View.INVISIBLE);
        }
        if (!bt.isBluetoothEnabled()) {
            Log.d("BT", "BT is not enabled!");
            pm_device.setChecked(false);
            pm_device.setText(R.string.turn_on_bt);
            pb.setVisibility(View.INVISIBLE);
        } else {
            Log.d("BT", "BT is enabled.");
            if (!bt.isServiceAvailable()) {
                Log.d("BT-Service", "BT service is not available!");
                pm_device.setChecked(false);
                pm_device.setText(R.string.connect_device);
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                pb.setVisibility(View.INVISIBLE);
            } else {
                Log.d("BT-Service", "BT service is available.");
                pm_device.setChecked(true);
                pm_device.setText(R.string.main_location);
                pb.setVisibility(View.INVISIBLE);
            }
        }

        // State Listener.
        bt.setBluetoothStateListener(new BluetoothSPP.BluetoothStateListener() {
            @Override
            public void onServiceStateChanged(int state) {
                if(state == BluetoothState.STATE_CONNECTED){
                    // Do something when successfully connected
                    Log.d("BT-State", "Connected");
                    pm_device.setChecked(true);
                    pm_device.setText(R.string.main_location);
                    pb.setVisibility(View.INVISIBLE);

                } else if(state == BluetoothState.STATE_CONNECTING) {
                    // Do something while connecting
                    Log.d("BT-State", "Connecting");
                    pm_device.setChecked(false);
                    pm_device.setText(R.string.connecting);
                    pb.setVisibility(View.VISIBLE);

                } else if(state == BluetoothState.STATE_LISTEN){
                    // Do something when device is waiting for connection
                    Log.d("BT-State", "Listen for connection");
                    pm_device.setChecked(false);
                    pm_device.setText(R.string.connect_device);
                    pb.setVisibility(View.INVISIBLE);

                } else if(state == BluetoothState.STATE_NONE){
                    // Do something when device don't have any connection
                    Log.d("BT-State", "None");
                    pm_device.setChecked(false);
                    pm_device.setText(R.string.connect_device);
                    pb.setVisibility(View.INVISIBLE);

                }
            }
        });

        // Connection Listener.
        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {
                pm_device.setChecked(true);
                pm_device.setText(R.string.main_location);
                Toast.makeText(NavActivity.this, "Connect to " + name + " successfully", Toast.LENGTH_SHORT).show();
                Log.d("BT-Connect", "Device " + name + " connected");
            }

            @Override
            public void onDeviceDisconnected() {
                pm_device.setChecked(false);
                pm_device.setText(R.string.connect_device);
                Toast.makeText(NavActivity.this, R.string.connect_lost, Toast.LENGTH_SHORT).show();
                Log.d("BT-Connect", "Device disconnected");
            }

            @Override
            public void onDeviceConnectionFailed() {
                pm_device.setChecked(false);
                pm_device.setText(R.string.connect_device);
                Toast.makeText(NavActivity.this, R.string.connect_failed, Toast.LENGTH_SHORT).show();
                Log.d("BT-Connect", "Connection failed!");
            }
        });

        // TODO: Bluetooth data receive listener.
        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            @Override
            public void onDataReceived(byte[] data, String message) {
//                if(counter %3 == 0 && counter != 0){
//                    s = "";
//                    counter = 0;
//                } else {
//                    s += message;
//                    counter++;
//                    if(counter == 1){
//                        // TODO: 2017/4/21 Open resetCounter();
////                        resetCounter();
//                        pb.setVisibility(View.VISIBLE);
//                    }
//                }
                s = message;
                Log.d("BT-Receive", "Data:" + s + " | Message: "+message);
//                Toast.makeText(NavActivity.this, "Data:" + s + " | Message: "+message, Toast.LENGTH_SHORT).show();
                // TIPS: End with "0x0D";
                for (int i = 0; i < data.length ;i++){
                    Log.d("BT-Data", "data[" + i + "] :" + data[i]);
                }
                Log.d("counter", ""+ counter);

                // Update pm_number
//                if(counter %3 == 0 && counter != 0 && !s.equals("")){
                if(!s.equals("")){
                    pb.setVisibility(View.INVISIBLE);
                    // Different color;
                    pm_val = Integer.parseInt(s);

                    SpannableString content = new SpannableString(s);
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    pm_number.setText(content);
                    if(pm_val >= 0 && pm_val <= 50){// Good;
                        pm_number.setTextColor(Color.parseColor("#388E3C"));
                    }else if (pm_val >= 51 && pm_val <= 100){// Moderate;
                        pm_number.setTextColor(Color.parseColor("#FBC02D"));
                    }else if (pm_val >= 101 && pm_val <= 150){// Unhealthy for Sensitive Groups;
                        pm_number.setTextColor(Color.parseColor("#F57C00"));
                    }else if (pm_val >= 151 && pm_val <= 200){// Unhealthy;
                        pm_number.setTextColor(Color.parseColor("#7B1FA2"));
                    }else if (pm_val >= 201 && pm_val <= 300){// Very Unhealthy;
                        pm_number.setTextColor(Color.parseColor("#9C27B0"));
                    }else{// Hazardous;
                        pm_number.setTextColor(Color.parseColor("#5D4037"));
                    }
                    // TODO: Broadcast string s;
                    Intent i = new Intent("value");
                    i.putExtra("value", s);
                    sendBroadcast(i);
                }
            }
        });


        // PM_Device button operation.
        pm_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if (pm_device.isChecked()){
                    // pm_device in ON state.
                    if(bt.isBluetoothEnabled()){
                        // BT is enabled, but not connected.
                        pm_device.setChecked(false);
                        pm_device.setText(R.string.connect_device);
                        Log.d("BT-Service", "Select a device.");
                        // Only shows paired devices.
                        intent = new Intent(getApplicationContext(), DeviceList.class);
                        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                        pb.setVisibility(View.VISIBLE);
                        bt.setupService();
                        bt.startService(BluetoothState.DEVICE_OTHER);
                    }
                    else {
                        // BT is not enabled.
                        Log.d("BT", "Turning on BT.");
                        intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
                        pb.setVisibility(View.VISIBLE);
                        // TODO: Crete a new listener to listen the change of bluetooth
                        if(bt.isBluetoothEnabled()){
                            pm_device.setChecked(false);
                            pm_device.setText(R.string.connect_device);
                        } else{
                            pm_device.setChecked(false);
                            pm_device.setText(R.string.turn_on_bt);
                        }
                    }
                } else {
                    // pm_device in OFF state.
                    Log.d("BT", "Stop BT service...");
                    bt.stopService();
                    pm_device.setChecked(false);
                    pm_device.setText(R.string.turn_on_bt);
                    pb.setVisibility(View.INVISIBLE);
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        if (drawer.isDrawerOpen(GravityCompat.START)) {
//            drawer.closeDrawer(GravityCompat.START);
//        } else {
//            super.onBackPressed();
//        }

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.doubleback, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nav, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent;
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Intent intent;
        if(id == R.id.nav_graph){
            intent = new Intent(this, FullscreenActivity.class);
            startActivity(intent);
        } else if(id == R.id.nav_log){
            intent = new Intent(this, LogActivity.class);
            startActivity(intent);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (bt.isServiceAvailable()) {
//            bt.stopService();
//        }
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (bt.isServiceAvailable()) {
//            bt.stopService();
//        }
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    // TODO: Byte to Integer;
    public static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if(resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
            } else {
                // Do something if user doesn't choose any device (Pressed back)
            }
        }
    }

    // Get bluetooth strength.
    private final BroadcastReceiver receiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String mIntentAction = intent.getAction();
            int rssi = 0;
            if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(mIntentAction)) {
                rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String mDeviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                Log.i("BT-Strength", "RSSI: " + rssi + "dBm");
                Log.i("BT-Name", "Name: " + mDeviceName);
                if (rssi < -500 || rssi > 100){
                    bt_strength.setText(R.string.no_rssi);
                } else {
                    bt_strength.setText(getString(R.string.rssi, rssi));
                }
            }
        }
    };

    // TODO: New handler event;
//    private Runnable number_fresh = new Runnable() {
//        @Override
//        public void run() {
//            Message m = new Message();
////            m.arg1 = (int value);
//            m.arg1 = number;
//            handler.sendMessage(m);
//
//        }
//    };
//    @SuppressLint("HandlerLeak")
//    Handler handler = new Handler(){
//        @Override
//        public void handleMessage(Message msg) {
//            String str = m.arg1 + "";
//            handler.post(number_fresh);
//        }
//    };

    // Create a new thread to send messages.
    public void testSend(){
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                bt.send(new byte[] {83}, false);
                Log.d("BT-testSend", "Send byte[]{83}");
                try{
                    Thread.sleep(1000);
                } catch (Exception e){
                    e.printStackTrace();
                }
                bt.send(new byte[] {89}, false);
                Log.d("BT-testSend", "Send byte[]{89}");
//                SystemClock.sleep(1000);
                try{
                    Thread.sleep(1000);
                } catch (Exception e){
                    e.printStackTrace();
                }
                bt.send(new byte[] {78}, false);
                Log.d("BT-testSend", "Send byte[]{78}");
            }
        });
        thread.start();
    }

    // TODO: BT Receiver Interrupt;
    public void resetCounter(){
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("BT_Receive", "Counter reseting...");
                try{
                    Thread.sleep(10);// Wait 15 sec;
                    counter = 0;
                    s = "";
                    Log.d("BT-Receive", "Counter Reset");
                } catch (Exception e){
                    e.printStackTrace();
                    Log.d("BT-Receive", "Counter Reset Error");
                }
            }
        });
        thread.start();
    }
}
