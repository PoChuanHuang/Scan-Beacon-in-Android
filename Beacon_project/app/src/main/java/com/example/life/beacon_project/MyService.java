package com.example.life.beacon_project;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyService extends Service {
    private MyBinder mBinder = new MyBinder();
    private GestureDetectorCompat mDetector;
    String TAG = "Service_TAG";
    String TAG2 = "Sensor_TAG";
    ArrayList<Double> average_rssi_30AEA40894C2 = new ArrayList<>();
    ArrayList<Double> average_rssi_30AEA40891BA = new ArrayList<>();
    ArrayList<Double> average_rssi_AAAAAAAAAA = new ArrayList<>();
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Double mean,sigma;//平均數、標準差
    float threshold = 0.2f;//閥值
    int weight=2;//時間權重
    int n = 3;//環境變數
    int txpower = -70;//bluetooth txpower
    Double distance=0.0;
    String move_state="";
    SensorManager mSersorManager;
    Sensor mAccrlerometers;
    Sensor mGyroscope;
    Sensor mMagnetometer;
    private int ongoingNotificationID = 42;
    String notifiation_content = "Background Service is open";
    NotificationManager mNotificationManager;


    private Handler handler = new Handler();
    private Runnable runnable = new Runnable()
    {
        @Override
        public void run()
        {
            Log.d("Run","run");
            //這是掃classic bluetooth的 也就是raspberrypi的bluetooth
            registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            mBluetoothAdapter.startDiscovery();

            //這是開始掃bluetooth low energy的
            scaniDevice(true);

            handler.postDelayed(this, 5000);//每五秒掃一次
        }
    };
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
                    Double d_rssi = Double.valueOf(rssi);//這裡收到的rssi值是short，但passdata的參數是double，所以要記得轉換
                    average_rssi_AAAAAAAAAA.add(d_rssi);
                    Log.d(TAG,"mac=AA:AA:AA:AA:AA:AA , rssi="+rssi);
                    function.PassData(d_rssi,3);//把rssi上傳server
                    //**AAAAAAAAAA Beacon的**//
                    if(average_rssi_AAAAAAAAAA.size()!=0)
                    {
                        fun_mean_and_sigma(average_rssi_AAAAAAAAAA);//算出平均數及標準差，進行高斯模糊，最後透過公式轉換出距離
                        Log.d(TAG,"rssi = "+average_rssi_AAAAAAAAAA+" , distance="+distance+"m");//秀出收到的rssi數據及距離
                        fun_remove_arraylist(average_rssi_AAAAAAAAAA);//讓arraylist裡的rssi維持只有六個，讓計算的數據是最近的六筆
                    }
                }
            } else
            { Log.d(TAG,"BluetoothDevice.action_Found not found"); }
        }
    };

    public void Sensor_function()
    {
        //從系統服務中獲得感測器管理器
        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // 從感測器管理器中獲得全部的感測器列表
        List<Sensor> allSensors = sm.getSensorList(Sensor.TYPE_ALL);
        // 顯示有多少個感測器
        Log.d(TAG2,"經檢測該手機有" + allSensors.size() + "個感測器，他們分別是：");

        for (Sensor s : allSensors)
        {
            switch (s.getType())
            {
                case Sensor.TYPE_ACCELEROMETER:
                    Log.d(TAG2,"sensor = "+s.getType());
                    mSersorManager.registerListener(accelerometerListener, mAccrlerometers, SensorManager.SENSOR_DELAY_UI);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    Log.d(TAG2,"sensor = "+s.getType());
                    mSersorManager.registerListener(accelerometerListener,mGyroscope,SensorManager.SENSOR_DELAY_UI);
                    break;
                case Sensor.TYPE_LIGHT:
                    //Log.d(TAG2, s.getType()+ " 環境光線感測器light");
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    //Log.d(TAG2, s.getType()+ " 電磁場感測器magnetic field");
                    break;
                case Sensor.TYPE_PRESSURE:
                    //Log.d(TAG2, s.getType()+ " 壓力感測器pressure");
                    break;
                case Sensor.TYPE_PROXIMITY:
                    //Log.d(TAG2, s.getType()+ " 距離感測器proximity");
                    break;
                default:
                    //Log.d(TAG2, s.getType()+ " 未知的手機sensor");
                    break;
            }
        }
    }

    private SensorEventListener accelerometerListener = new SensorEventListener(){

        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {

        }
//        第一個值(values[0])代表手機的水平旋轉
//        第二個值(values[1])代表手機的前後翻轉
//        第三個值(values[2])代表手機的左右翻轉
//        分别表示x,y,z轴的旋转的角速度

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch(event.sensor.getType())
            {
                case Sensor.TYPE_ACCELEROMETER:
                    Log.d(TAG2,"加速度感測器的數值: X: " + String.valueOf(event.values[0])+"\t"
                            +"Y: " + String.valueOf(event.values[1])+"\t"
                            +"Z: " + String.valueOf(event.values[2]));

                    break;
                case Sensor.TYPE_GYROSCOPE:
                    Log.d(TAG2,"陀螺儀感測器的數值: X: " + String.valueOf(event.values[0])+"\t"
                            +"Y: " + String.valueOf(event.values[1])+"\t"
                            +"Z: " + String.valueOf(event.values[2]));
                    break;
                default:
            }
        }};

    public void scaniDevice(boolean en)
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
                },10000);
                ScanCallback scallback = new SampleScanCallback();
                bluetoothLeScanner.startScan(null,settings,scallback);
            }
            catch (Exception e){}
        }else{
            ScanCallback scallback = new SampleScanCallback();
            bluetoothLeScanner.stopScan(scallback); //停止
        }
    }

    public class SampleScanCallback extends ScanCallback
    {
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
                Log.d(TAG,"掃描到的mac有 => "+scanResult.getDevice().getAddress());
                switch (scanResult.getDevice().getAddress())
                {
                    case "30:AE:A4:08:94:C2":
                        Log.d(TAG,"mac=30:AE:A4:08:94:C2 , rssi="+rssi);
                        average_rssi_30AEA40894C2.add(rssi);t=true;
                        function.PassData(rssi,1);//把rssi上傳server
                        break;
                    case "30:AE:A4:08:91:BA":
                        Log.d(TAG,"mac=30:AE:A4:08:91:BA , rssi="+rssi);
                        average_rssi_30AEA40891BA.add(rssi);t=true;
                        function.PassData(rssi,2);//把rssi上傳server
                        break;
                    default:
                        //Log.d(TAG,"不是我們的bluetooth");
                }
            }
            if(t)//製作這個bool的用意是 上面的掃瞄結果，要掃到指定的mac，t才會true，我才需要重新算一次距離。
            {
                //**30AEA40894C2 Beacon的**//
                if(average_rssi_30AEA40894C2.size()!=0)
                {
                    fun_mean_and_sigma(average_rssi_30AEA40894C2);//算出平均數及標準差，進行高斯模糊，最後透過公式轉換出距離
                    Log.d(TAG,"rssi = "+average_rssi_30AEA40894C2+" , distance="+distance+"m");//秀出收到的rssi數據及距離
                    fun_remove_arraylist(average_rssi_30AEA40894C2);//讓arraylist裡的rssi維持只有六個，讓計算的數據是最近的六筆
                }

                //**30AEA40891BA Beacon的**//
                if(average_rssi_30AEA40891BA.size()!=0)
                {
                    fun_mean_and_sigma(average_rssi_30AEA40891BA);//算出平均數及標準差，進行高斯模糊，最後透過公式轉換出距離
                    Log.d(TAG,"rssi = "+average_rssi_30AEA40891BA+" , distance="+distance+"m");//秀出收到的rssi數據及距離
                    fun_remove_arraylist(average_rssi_30AEA40891BA);//讓arraylist裡的rssi維持只有六個，讓計算的數據是最近的六筆
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode)//如果掃描失敗
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

    class MyBinder extends Binder //宣告一個繼承 Binder 的類別 MyBinder
    {
        public void startDownload()
        {
            Log.d(TAG, "================startDownload() executed================");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "================onBind================");
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        Log.d(TAG, "================onCreate================");
//        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        mSersorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//從系統服務中獲得感測器管理器
        mAccrlerometers = mSersorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//管理器取得加速度感測器
        mGyroscope = mSersorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);//管理器取得陀螺儀感測器
//        SENSOR_DELAY_UI適合在UI空間中獲得數據，為四種模式裡更新速度最慢的了
//        SensorManager_SENSOR_DELAY_FASTEST： 以最快的速度获得传感器数据
//        SENSOR_DELAY_GAME： 适合与在游戏中获得传感器数据
//        SENSOR_DELAY_NORMAL： 以一般的速度获得传感器数据
//        SENSOR_DELAY_UI ：适合于在ui空间中获得数据
        mSersorManager.registerListener(accelerometerListener, mAccrlerometers, SensorManager.SENSOR_DELAY_UI);//對加速度感測器進行監聽
        mSersorManager.registerListener(accelerometerListener, mGyroscope, SensorManager.SENSOR_DELAY_UI);//對陀螺儀感測器進行監聽
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "================onStartCommand================");
        new Thread() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                super.run();
                //...要做的事在這執行...
                Log.d(TAG,"run");

                startForeground(ongoingNotificationID, getOngoingNotification(notifiation_content+"is running"));

                handler.postDelayed(runnable, 5000);//開啟掃描 傳統藍芽 及sensor 及 BLE 的handler
            }
        }.start();
        return super.onStartCommand(intent, flags, startId);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification getOngoingNotification(String text)
    {
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle(notifiation_content);
        bigTextStyle.bigText(text);

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        String channelId = getString(R.string.app_name);
        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription(channelId);
        notificationChannel.setSound(null, null);

        mNotificationManager.createNotificationChannel(notificationChannel);
        Notification.Builder notification = new Notification.Builder(this,channelId);

        return notification.setContentTitle(notifiation_content)
                .setContentText("Connected through SQL to Server")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .build();
    }
    @Override
    public void onDestroy()
    {
        Log.d(TAG, "================onDestroy================");
        super.onDestroy();
    }

}
