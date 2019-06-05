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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.SyncStateContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

public class MainActivity extends AppCompatActivity {

    Context context;
    private final static int REQUEST_ENABLE_BT = 1;
    String TAG = "MainActivity";
    String TAG_bluetooth = "bluetooth";
    Double total_rssi_5C313E310285=0.0,total_rssi_7CEC796F4764=0.0,total_rssi_A81B6AB30F67=0.0;
    ArrayList<Double> average_rssi_5C313E310285 = new ArrayList<>();
    ArrayList<Double> average_rssi_7CEC796F4764 = new ArrayList<>();
    ArrayList<Double> average_rssi_A81B6AB30F67 = new ArrayList<>();
    ArrayList<Double> average_rssi_D9D082EBDC69 = new ArrayList<>();
    Boolean go_to_adslab_bool=false;
    int record_7CEC796F4764=0;int record_A81B6AB30F67=0;int record_average_rssi_D9D082EBDC69=0;

    //Double[] data = {-69.0, -72.0, -71.0 , -64.0, -68.0};
    Double mean,sigma;//平均數、標準差
    float threshold = 0.2f;//閥值
    int weight=2;//時間權重
    int n = 3;//環境變數
    int txpower = -70;//bluetooth txpower
    Double distance=0.0;
    String move_state="";
    ArrayList<Double> array_data = new ArrayList<>();
    ArrayList<Double> distance_arraylist = new ArrayList<>();
    TextView distance_textview,rssi_textview,check_textview;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        distance_textview = findViewById(R.id.distance_textview);
        rssi_textview = findViewById(R.id.rssi_textview);
        check_textview = findViewById(R.id.check_textview);
//        //把資料加進去
//        for(int i=0;i<data.length;i++)
//        { array_data.add(data[i]); }
//        //算出平均數
//        mean = fun_mean(array_data);
//        Log.e(TAG_bluetooth,"mean="+mean);
//        //計算標準差
//        sigma = fun_sigma(array_data,mean);
//        Log.e(TAG_bluetooth,"sigma="+sigma);
//        //高斯濾波
//        fun_Gaussian_blur(array_data,mean,sigma);
//        fun_time_of_weight(array_data);
//        move_state = fun_away_or_close(array_data);
//        distance = fun_distance(array_data);
//
//        for(int i=0;i<array_data.size();i++)
//        {
//            Log.e(TAG_bluetooth,"distance("+i+")="+array_data.get(i));
//        }
//        Log.e(TAG_bluetooth,"move_state="+move_state+",distance = "+distance);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        { Log.d(TAG,"設備不支持藍牙"); }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // 檢查手機是否開啟藍芽裝置
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            Toast.makeText(this, "請開啟藍芽裝置", Toast.LENGTH_SHORT).show();
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }else{scaniDevice(true);}
    }
    //然而你如果想下載完這個開源項目就想運行看效果，
    // 伙計，不得不說你是絕對不會成功的，
    // buildScanFilters裡面添加了掃描過濾規則，
    // uuid無法匹配你是看不到任何東西的，
    // 根據代碼提示注釋掉下面的過濾條件可以看到所有的藍牙設備。
    private List buildScanFilters()
    {
        List scanFilters = new ArrayList<>();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        //注釋掉下面的掃描過濾規則，才能掃描到（uuid不匹配沒法掃描到的）
        //builder.setServiceUuid(SyncStateContract.Constants.Service_UUID);
        //scanFilters.add(builder.build());
        return scanFilters;
    }
    //在低功耗模式下執行藍牙LE掃描。這是默認掃描模式,因為它消耗最小功率
    private ScanSettings buildScanSettings()
    {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }
    private void scaniDevice(final boolean en)
    {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        final Handler mHandler = new Handler();
        final ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000).build();
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (en){
            try{
                mHandler.postDelayed(new Runnable(){
                    @Override
                    public void run(){
                        ScanCallback scallback = new SampleScanCallback();
                        bluetoothLeScanner.stopScan(scallback);
                    }
                },5000);
                ScanCallback scallback = new SampleScanCallback();
                bluetoothLeScanner.startScan(null,settings,scallback);
            }
            catch (Exception e){Log.d(TAG,"e = "+e);}
        }else{
            ScanCallback scallback = new SampleScanCallback();
            bluetoothLeScanner.stopScan(scallback); //停止
        }
    }
    public class SampleScanCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {super.onScanResult(callbackType, result); }

        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            super.onBatchScanResults(results);
            boolean t=false;
            for(ScanResult scanResult : results)
            {
                Double rssi = Double.valueOf(scanResult.getRssi());
                switch (scanResult.getDevice().getAddress())
                {
                    case "5C:31:3E:31:02:85":
                        average_rssi_5C313E310285.add(rssi);t=true;break;
                    case "7C:EC:79:6F:47:64":
                        average_rssi_7CEC796F4764.add(rssi);t=true;record_7CEC796F4764++;break;
                    case "A8:1B:6A:B3:0F:67":
                        average_rssi_A81B6AB30F67.add(rssi);
                        //Log.e(TAG_bluetooth,"rssi = "+rssi);
                        t=true;record_A81B6AB30F67++;break;
                    /*case "D9:D0:82:EB:DC:69":
                        average_rssi_D9D082EBDC69.add(rssi);
                        t=true;break;*/
                    case "A4:34:F1:8A:1D:B0":
                        average_rssi_D9D082EBDC69.add(rssi);
                        t=true;break;
                    default:
                        //Log.d(TAG,"不是我們的bluetooth");
                }
            }
            if(t)
            {
                  //**mac=5C313E310285的bluetooth**//
//                for(int i=0;i<average_rssi_5C313E310285.size();i++)
//                {total_rssi_5C313E310285 +=average_rssi_5C313E310285.get(i); }
//                total_rssi_5C313E310285 = total_rssi_5C313E310285/average_rssi_5C313E310285.size();
//                distance = Math.pow(10,(Math.abs(total_rssi_5C313E310285)-127)/(10*n));
//                Log.d(TAG,"total_rssi_5C313E310285 = "+total_rssi_5C313E310285+",distance_5C313E310285 = "+distance);
//                total_rssi_5C313E310285 = 0.0;
//                if(average_rssi_5C313E310285.size()>10)
//                {average_rssi_5C313E310285.clear();}
                  //**mac=5C313E310285的bluetooth**//

//                if(average_rssi_7CEC796F4764.size()>=100)
//                {
//                    for(int i=0;i<average_rssi_7CEC796F4764.size();i++)
//                    {
//                        //Log.e(TAG_7CEC796F4764,"average_rssi_7CEC796F4764.get("+i+") = "+average_rssi_7CEC796F4764.get(i));//秀出每一筆
//                        total_rssi_7CEC796F4764 +=average_rssi_7CEC796F4764.get(i);//全部加起來
//                    }
//                    total_rssi_7CEC796F4764 = total_rssi_7CEC796F4764/average_rssi_7CEC796F4764.size();//算出平均數
//                    Log.e(TAG_bluetooth,"record_7CEC796F4764 = "+record_7CEC796F4764);
//                    Log.e(TAG_bluetooth,"total_rssi_7CEC796F4764 = "+total_rssi_7CEC796F4764);
//                    average_rssi_7CEC796F4764.clear();total_rssi_7CEC796F4764=0.0;
//                    Log.e(TAG_bluetooth,"刪除100筆7CEC796F4764");
//                }


//                array_data = new ArrayList(average_rssi_A81B6AB30F67);
//                rssi_textview.setText("rssi[]="+array_data);
//                fun_mean_and_sigma(array_data);
//                distance_textview.setText("distance="+distance+"m");
//                if(distance<2)
//                {
//                    if(!go_to_adslab_bool)//false
//                    {
//                        go_to_adslab_bool=true;
//                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                        builder.setTitle("感測到ADS實驗室在這附近");
//                        builder.setMessage("是否查看相關訊息?");
//                        builder.setPositiveButton("前往", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                Intent go_intent = new Intent(MainActivity.this,adslab_activity.class);
//                                startActivity(go_intent);
//                            }
//                        });
//                        builder.setNegativeButton("不了", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) { }
//                        });
//                        AlertDialog dialog=builder.create();
//                        dialog.show();
//                    }
//                }
//                if(distance>5)
//                {
//                    go_to_adslab_bool=false;
//                }
//                //Log.e(TAG_bluetooth,distance+"");
//                distance_arraylist.add(distance);
//                if(distance_arraylist.size()>=100)
//                {
//                    Log.e(TAG_bluetooth,distance_arraylist+"");
//                    distance_arraylist.clear();
//                    check_textview.setText("get it");
//                }
//                fun_remove_arraylist(average_rssi_A81B6AB30F67);


                //**D9D082EBDC69 Beacon的**//
                array_data = new ArrayList(average_rssi_D9D082EBDC69);
                rssi_textview.setText("rssi[]="+array_data);
                fun_mean_and_sigma(array_data);
                distance_textview.setText("distance="+distance+"m");
                distance_arraylist.add(distance);
                fun_remove_arraylist(average_rssi_D9D082EBDC69);

            }
        }

        @Override
        public void onScanFailed(int errorCode)
        {
            super.onScanFailed(errorCode);

            Log.d(TAG,"onScanFailed 進入");
            final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Log.d(TAG,"onScanFailed errorCode = "+errorCode);

            if (mBluetoothAdapter != null)
            {
                Log.d(TAG,"mBluetoothAdapter != null");
                // 一旦发生错误，除了重启蓝牙再没有其它解决办法
                mBluetoothAdapter.disable();
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        while(true) {
                            try {
                                Log.d(TAG,"Thread.sleep(500)");
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //要等待蓝牙彻底关闭，然后再打开，才能实现重启效果
                            if(mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF)
                            {
                                Log.d(TAG,"mBluetoothAdapter.enable");
                                mBluetoothAdapter.enable();
                                break;
                            }
                        }
                    }

                }).start();
            }
            Log.d(TAG,"onScanFailed 出去");
        }
    }

    public void fun_mean_and_sigma(ArrayList<Double> data)
    {
        Double sum=0.0;Double fun_mean=0.0;
        for(int i=0;i<data.size();i++)
        {
            fun_mean +=data.get(i);
        }
        mean = fun_mean/data.size();
        for(int i=0;i<data.size();i++)
        {
            sum += Math.pow(data.get(i)-mean,2);
        }
        sigma = Math.sqrt(sum/data.size());
        if(sigma==0)
        {
            fun_distance(mean);
        }
        else{

            fun_Gaussian_blur_and_time_of_weight(data,mean,sigma);
        }

    }
    public void fun_Gaussian_blur_and_time_of_weight(ArrayList<Double> data,Double mean,Double sigma)
    {
//        int mid_size = data.size()/2;
//        for(int i=0;i<data.size();i++){
//            if(i<mid_size) {
//                data.set(i,data.get(i)*Math.exp(-Math.pow(data.get(i)-mean,2)/(2*Math.pow(sigma,2)))/(Math.sqrt(2*Math.PI)*sigma)*weight);
//            }
//            else {
//                data.set(i,data.get(i)*Math.exp(-Math.pow(data.get(i)-mean,2)/(2*Math.pow(sigma,2)))/(Math.sqrt(2*Math.PI)*sigma));
//            }
//        }
//        fun_distance(data);
        ArrayList<Double> gussir = new ArrayList<>();Double g_mean=0.0;
        for(int i=0;i<data.size();i++)
        {
            gussir.add(i,Math.exp(-Math.pow(data.get(i)-mean,2)/(2*Math.pow(sigma,2)))/(Math.sqrt(2*Math.PI)*sigma));
            g_mean += gussir.get(i);
        }
        g_mean = g_mean/gussir.size();
        Double gussir_max  = Collections.max(gussir);
        Double gussir_min  = Collections.min(gussir);
        Double present = (gussir_max-gussir_min)/100;
        Double g_little_max = g_mean+present*20;
        Double g_little_min = g_mean-present*20;
        ArrayList<Double> gussir_process = new ArrayList<>();
        for(int i=0;i<gussir.size();i++)
        {
            if(gussir.get(i)<=g_little_max&&gussir.get(i)>=g_little_min)
            {
                gussir_process.add(gussir.get(i));
            }
        }
        g_mean=0.0;
        for(int i=0;i<gussir_process.size();i++)
        {
            g_mean += gussir_process.get(i);
        }
        g_mean = g_mean/gussir_process.size();
        Double new_data_mean;
        if(gussir_process.size()==0)
        {
            fun_distance(mean);
        }else{
            new_data_mean = Math.sqrt(-(Math.log(g_mean*(Math.sqrt(2*Math.PI)*sigma))*2*Math.pow(sigma,2)))+mean;
            fun_distance(new_data_mean);
        }
    }
//    public void fun_away_or_close(ArrayList<Double> data)
//    {
//        Double little_max = Collections.max(data)-threshold;
//        Double little_min = Collections.min(data)+threshold;
//        int little_max_size=0,little_min_size=0;
//        for(int i=0;i<data.size();i++)
//        {
//            if(data.get(i)>little_max) little_max_size++;
//            if(data.get(i)<little_min) little_min_size++;
//        }
//        if(data.size()/3<little_max_size)
//        {
//            for(int i=0;i<data.size();i++)
//            {if(data.get(i)>little_max) data.remove(i);i--;  }
//            move_state = "遠離中";fun_distance(data);
//        }else if(data.size()/3<little_min_size){
//            for(int i=0;i<data.size();i++)
//            {
//                if(data.get(i)<little_min) data.remove(i);i--;
//            }
//            move_state = "靠近中";fun_distance(data);
//        }else{
//            move_state = "無法偵測有無遠離或靠近";fun_distance(data);
//        }
//    }

    public void fun_distance(Double data)
    {
        distance=0.0;
        distance = Math.pow(10,Math.abs(data-txpower)/(10*n));
    }
    public void fun_remove_arraylist(ArrayList<Double> data)
    {
        if(data.size()>5)
        {
            data.remove(0);
            fun_remove_arraylist(data);
        }
    }

}





