package com.example.life.beacon_project;


import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Context context;
    private final static int REQUEST_ENABLE_BT = 1;
    String TAG = "MainActivity_TAG";
    Button scan_bt;
    //private GestureDetectorCompat mDetector;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scan_bt = findViewById(R.id.scan_bt);
//        mDetector = new GestureDetectorCompat(this, new MyGestureListener());

        if (mBluetoothAdapter == null)
        { Log.d(TAG,"設備不支持藍牙"); }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }

        scan_bt.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())//檢查是否開啟藍芽
            {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
            }
            else if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    ||ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)//檢查是否開啟該應用程式的位置權限
            {
                request_permissions();//沒有開啟的話就在跳出開啟該應用程式位置權限視窗
            }
            else {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("按下點擊後，即會收集手機資訊\n並且上傳本實驗研究之server")
                        .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                scan_bt.setText("已開啟偵測");
                                scan_bt.setEnabled(false);
                                Intent service = new Intent(MainActivity.this, MyService.class);
                                //service.putExtra("startType", 1);
                                if (Build.VERSION.SDK_INT >= 26) {
                                    Log.d("MyService", "SDK_INT>26, startforegroundService");
                                    MainActivity.this.startForegroundService(service);
                                } else {
                                    Log.d("MyService", "SDK_INT<26, startService");
                                    MainActivity.this.startService(service);
                                }
                            }
                        })
                        .setNegativeButton("拒絕", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                }
            }
        });
    }

    private void request_permissions()//檢查權限
    {
        // 創建一個權限列表，把需要使用而沒有授權的的權限存放在這裡
        List<String> permissionList = new ArrayList<>();

        // 判斷權限是否已經授予，没有就把該權限添加到列表中
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // 如果列表為空，代表全部權限都有了，反之則代表有權限還沒有被加到list，要申请權限
        if (!permissionList.isEmpty())//列表不是空的
        {
            ActivityCompat.requestPermissions(this,permissionList.toArray(new String[permissionList.size()]), 1002);
        }
    }
    // 請求權限回調方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "================Activity onDestroy================");

        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        Log.d(TAG, "================Activity onResume123================");
		
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())//檢查是否開啟藍芽
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }
        else if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)//檢查是否開啟該應用程式的位置權限
        {
            request_permissions();//沒有開啟的話就在跳出開啟該應用程式位置權限視窗
        }
        else {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("按下點擊後，即會收集手機資訊\n並且上傳本實驗研究之server")
                    .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            scan_bt.setText("已開啟偵測");
                            scan_bt.setEnabled(false);
                            Intent service = new Intent(MainActivity.this, MyService.class);
                            //service.putExtra("startType", 1);
                            if (Build.VERSION.SDK_INT >= 26) {
                                Log.d("MyService", "SDK_INT>26, startforegroundService");
                                MainActivity.this.startForegroundService(service);
                            } else {
                                Log.d("MyService", "SDK_INT<26, startService");
                                MainActivity.this.startService(service);
                            }
                        }
                    })
                    .setNegativeButton("拒絕", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
            }
            super.onResume();

    }

}





