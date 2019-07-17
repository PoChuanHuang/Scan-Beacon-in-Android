package com.example.life.beacon_project;


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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Context context;
    private final static int REQUEST_ENABLE_BT = 1;
    String TAG = "Bluetooth_TAG";
    ArrayList<Double> average_rssi_30AEA40894C2 = new ArrayList<>();
    ArrayList<Double> average_rssi_30AEA40891BA = new ArrayList<>();
    ArrayList<Short> average_rssi_AAAAAAAAAA = new ArrayList<>();

//    Boolean go_to_adslab_bool=false;
    Double mean,sigma;//平均數、標準差
    float threshold = 0.2f;//閥值
    int weight=2;//時間權重
    int n = 3;//環境變數
    int txpower = -70;//bluetooth txpower
    Double distance=0.0;
    String move_state="";
    ArrayList<Double> distance_arraylist = new ArrayList<>();
    ArrayList<Double> distance_arraylist2 = new ArrayList<>();
    ArrayList<Double> distance_arraylist3 = new ArrayList<>();
    TextView distance_textview,rssi_textview,check_textview;
    TextView distance_textview2,rssi_textview2,check_textview2;
    TextView distance_textview3,rssi_textview3,check_textview3;
    TextView mac_textview,pass_check_textview;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    SensorManager mSersorManager;
    Sensor mAccrlerometers;
    Sensor mGyroscope;
    Sensor mMagnetometer;


    //**這是scan傳統藍芽的handler 每三秒掃一次
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable()
    {
        @Override
        public void run()
        {
            registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            mBluetoothAdapter.startDiscovery();
            Sensor_function();
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        distance_textview   = findViewById(R.id.distance_textview);
        rssi_textview       = findViewById(R.id.rssi_textview);
        check_textview      = findViewById(R.id.check_textview);
        distance_textview2  = findViewById(R.id.distance_textview2);
        rssi_textview2      = findViewById(R.id.rssi_textview2);
        check_textview2     = findViewById(R.id.check_textview2);
        distance_textview3  = findViewById(R.id.distance_textview3);
        rssi_textview3      = findViewById(R.id.rssi_textview3);
        check_textview3     = findViewById(R.id.check_textview3);
        mac_textview        = findViewById(R.id.mac_textview);
        pass_check_textview  = findViewById(R.id.pass_check_textview);

        startHandleLoop();////開啟掃描傳統藍芽的handler
        mSersorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccrlerometers = mSersorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSersorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnetometer = mSersorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
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
    private void startHandleLoop()
    {handler.postDelayed(runnable, 1000);}

    private final BroadcastReceiver receiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action))
            {
                Short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getAddress();
                if(name.contains("AA:AA:AA:AA:AA:AA"))
                {
                    average_rssi_AAAAAAAAAA.add(rssi);
                    Double d_rssi = Double.valueOf(rssi);//這裡收到的rssi值是short，但passdata的參數是double，所以要記得轉換
                    PassData(d_rssi,3);
                    check_textview3.setText("mac=AA:AA:AA:AA:AA:AA");
                    rssi_textview3.setText("rssi="+average_rssi_AAAAAAAAAA);
                    distance_textview3.setText("distance="+distance+"m");
                    distance_arraylist3.add(distance);
                    fun_remove_arraylist2(average_rssi_AAAAAAAAAA);
                }
            } else
            {
                check_textview3.setText("BluetoothDevice.action_Found not found");
                rssi_textview3.setText("BluetoothDevice.action_Found not found");
                distance_textview3.setText("BluetoothDevice.action_Found not found");
            }

        }
    };
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
                mac_textview.setText(scanResult.getDevice().getAddress());//顯示掃描出來的所有device的mac address
                switch (scanResult.getDevice().getAddress())
                {
                    case "30:AE:A4:08:94:C2":
                        average_rssi_30AEA40894C2.add(rssi);t=true;
                        check_textview.setText("mac=30:AE:A4:08:94:C2");
                        PassData(rssi,1);
                        break;
                    case "30:AE:A4:08:91:BA":
                        average_rssi_30AEA40891BA.add(rssi);t=true;
                        check_textview2.setText("mac=30:AE:A4:08:91:BA");
                        PassData(rssi,2);
                        break;
                    default:
                        //Log.d(TAG,"不是我們的bluetooth");
                }
            }
            if(t)
            {
                //**30AEA40894C2 Beacon的**//
                if(average_rssi_30AEA40894C2.size()!=0)
                {
                    rssi_textview.setText("rssi="+average_rssi_30AEA40894C2);//先秀出收到的rssi數據
                    fun_mean_and_sigma(average_rssi_30AEA40894C2);//算出平均數及標準差，進行高斯模糊，最後透過公式轉換出距離
                    distance_textview.setText("distance="+distance+"m");//秀出距離
                    distance_arraylist.add(distance);//加到一個arraylist
                    fun_remove_arraylist(average_rssi_30AEA40894C2);//讓arraylist裡的rssi維持只有六個，讓計算的數據是最近的六筆
                }

                //**30AEA40891BA Beacon的**//
                if(average_rssi_30AEA40891BA.size()!=0)
                {
                    rssi_textview2.setText("rssi="+average_rssi_30AEA40891BA);//先秀出收到的rssi數據
                    fun_mean_and_sigma(average_rssi_30AEA40891BA);//算出平均數及標準差，進行高斯模糊，最後透過公式轉換出距離
                    distance_textview2.setText("distance="+distance+"m");//秀出距離
                    distance_arraylist2.add(distance);//加到一個arraylist
                    fun_remove_arraylist(average_rssi_30AEA40891BA);//讓arraylist裡的rssi維持只有六個，讓計算的數據是最近的六筆
                }
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

    public void fun_mean_and_sigma(ArrayList<Double> data)//算出平均數與標準差
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
    public void fun_Gaussian_blur_and_time_of_weight(ArrayList<Double> data,Double mean,Double sigma)//高斯模糊
    {
        ArrayList<Double> gussir = new ArrayList<>();Double g_mean=0.0;
        for(int i=0;i<data.size();i++)
        {
            gussir.add(i,Math.exp(-Math.pow(data.get(i)-mean,2)/(2*Math.pow(sigma,2)))/(Math.sqrt(2*Math.PI)*sigma));
            g_mean += gussir.get(i);
        }
        g_mean = g_mean/gussir.size();
        Double gussir_max  = Collections.max(gussir);//取max
        Double gussir_min  = Collections.min(gussir);//取min
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
    public void fun_away_or_close(ArrayList<Double> data)//偵測遠離靠近
    {
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
    }

    public void fun_distance(Double data)//將rssi值套入公式轉成距離(m)
    {
        distance = 0.0;
        distance = Math.pow(10,Math.abs(data-txpower)/(10*n));
    }

    public void fun_remove_arraylist(ArrayList<Double> data)//將data這個arraylist保持在5個元素，多出來就刪掉
    {
        if(data.size()>5)
        {
            data.remove(0);
            fun_remove_arraylist(data);
        }
    }

    public void fun_remove_arraylist2(ArrayList<Short> data)///將data這個arraylist保持在5個元素，多出來就刪掉
    {
        if(data.size()>5)
        {
            data.remove(0);
            fun_remove_arraylist2(data);
        }
    }

    public void PassData(Double rssi_data,int id)
    {
        String url;
        Task task;
        if (function.isConnected(MainActivity.this))
        {
            switch(id)
            {
                case 1:
                    url = "http://120.101.4.52/rssipost.php?C2rssi="+rssi_data;
                    task = new Task();
                    task.execute(url);
                    break;
                case 2:
                    url = "http://120.101.4.52/rssipost.php?BArssi="+rssi_data;
                    task = new Task();
                    task.execute(url);
                    break;
                case 3:
                    url = "http://120.101.4.52/rssipost.php?AArssi="+rssi_data;
                    task = new Task();
                    task.execute(url);
                    break;
                default:
                    //nothing
            }
            pass_check_textview.setText("Pass data good");
        }else{
            pass_check_textview.setText("No Network");
        }
    }

    public void Sensor_function()
    {
        //從系統服務中獲得感測器管理器
        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // 從感測器管理器中獲得全部的感測器列表
        List<Sensor> allSensors = sm.getSensorList(Sensor.TYPE_ALL);
        // 顯示有多少個感測器
        Log.d(TAG,"經檢測該手機有" + allSensors.size() + "個感測器，他們分別是：");

        for (Sensor s : allSensors)
        {
            switch (s.getType())
            {
                case Sensor.TYPE_ACCELEROMETER:
                    Log.d(TAG,"加速度感測器:");
                    mSersorManager.registerListener(accelerometerListener, s, SensorManager.SENSOR_DELAY_NORMAL);

                    break;
                case Sensor.TYPE_GYROSCOPE:
                    Log.d(TAG, "陀螺儀感測器:");
                    mSersorManager.registerListener(accelerometerListener,mGyroscope,SensorManager.SENSOR_DELAY_NORMAL);
                    break;
                case Sensor.TYPE_LIGHT:
                    //Log.d(TAG, s.getType()+ " 環境光線感測器light");
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    //Log.d(TAG, s.getType()+ " 電磁場感測器magnetic field");
                    break;
                case Sensor.TYPE_PRESSURE:
                    //Log.d(TAG, s.getType()+ " 壓力感測器pressure");
                    break;
                case Sensor.TYPE_PROXIMITY:
                    //Log.d(TAG, s.getType()+ " 距離感測器proximity");
                    break;
                default:
                    //Log.d(TAG, s.getType()+ " 未知的手機sensor");
                    break;
            }
        }
    }
    private SensorEventListener accelerometerListener = new SensorEventListener(){

        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            switch(event.sensor.getType())
            {
                case Sensor.TYPE_ACCELEROMETER:
//                    Log.d(TAG,"加速度感測器的數值: X: " + String.valueOf(event.values[0])+"\t"
//                            +"Y: " + String.valueOf(event.values[1])+"\t"
//                            +"Z: " + String.valueOf(event.values[2]));
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    Log.d(TAG,"陀螺儀感測器的數值: X: " + String.valueOf(event.values[0])+"\t"
                            +"Y: " + String.valueOf(event.values[1])+"\t"
                            +"Z: " + String.valueOf(event.values[2]));
                    break;
                default:

            }

        }};
}





