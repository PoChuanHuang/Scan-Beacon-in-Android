# Scan-Beacon-in-Android
This is an app which developed in Android Studio.
You can use it to scan nearby bluetooth and get some information.
(E.g UUID Major Rssi)


## 第一部分:Activity基本設定


<h3>開啟藍芽</h3>

偵測該設備是否支援藍芽以及BLE，以及是否開啟藍芽，如果未開啟會跳出是否開啟藍芽視窗。
    
```gherkin=
if (mBluetoothAdapter == null)
{ Log.d(TAG,"設備不支持藍牙"); }

if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
{
    Toast.makeText(this, "不支持BLE", Toast.LENGTH_SHORT).show();
    finish();
}
        
final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
mBluetoothAdapter = bluetoothManager.getAdapter();
// 檢查手機是否開啟藍芽裝置
if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
{
    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);//開啟打開藍芽的視窗
}
```

<h3>開啟位置權限</h3>

偵測是否開啟該應用程式的位置權限，如未開啟會跳出是否開啟位置權限視窗。
```gherkin=
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
```
<h3>同意視窗</h3>

開啟權限後，應彈出[視窗](https://github.com/PoChuanHuang/Scan-Beacon-in-Android/blob/master/Image/%E5%90%8C%E6%84%8F%E8%A6%96%E7%AA%97.jpg)告知使用者「是否同意開啟手機收集資訊功能並上傳本實驗研究之伺服器」
* **<h4>自動開啟</h4>**
    * 程式碼要放在Activity的生命週期的onResume()
* **<h4>手動開啟</h4>**
    * 設定一個button來讓同意視窗手動彈出

```gherkin=
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

```
## 第二部分:Service背景服務
由於要讓手機收集的資訊在背景程式運作，並且上傳server，所以包含傳統藍芽、BLE、感測器資訊監聽都會在service運作。

<h3>BLE低功耗藍芽</h3>

In OnBatchScanResults , 你可以找到所有掃到的device，然後再依照所指定的mac去做判斷處理，並且存入該指定device的rssi值，最後轉換成距離。
```gherkin=
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
```
<h3>傳統藍芽</h3>

剛剛是BLE協定的掃瞄，現在這個是傳統藍芽協定的掃瞄。
因為要一直掃描，所以我用runnable的方式進行重複掃描(每五秒掃一次)
```gherkin=
private Handler handler = new Handler();
private Runnable runnable = new Runnable()
{
    @Override
    public void run()
    {
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        mBluetoothAdapter.startDiscovery();
        handler.postDelayed(this, 5000);
    }
};
```
利用BroadcastReceiver接收收到的藍芽信號，取RSSI值，最後轉換成距離。
```gherkin=
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
```
<h3>轉換距離公式</h3>

fun_mean_and_sigma()是將現有的arraylist算其平均數與標準差
若標準差為0，代表該arraylist的數值都一樣，就可直接取其平均數轉換成距離
若不為0，則將其進行下一步處理-高斯模糊
```gherkin=
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
```

高斯模糊
```gherkin=
public void fun_Gaussian_blur_and_time_of_weight(ArrayList<Double> data,Double mean,Double sigma)
{
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
```


將處理好的數值取其最平均後，透過公式轉換成距離(單位:m)
```gherkin=
public void fun_distance(Double data)//將rssi值套入公式轉成距離(m)
{
    distance = 0.0;
    distance = Math.pow(10,Math.abs(data-txpower)/(10*n));
}
```
若arraylist的數值大於5筆了，則刪除第一筆
```gherkin=
public void fun_remove_arraylist(ArrayList<Double> data)//將data這個arraylist保持在5個元素，多出來就刪掉
{
    if(data.size()>5)
    {
        data.remove(0);
        fun_remove_arraylist(data);
    }
}
```

<h3>感測器數據</h3>

在onCreate()利用SensorManager偵測加速度與陀螺儀感測器，並進行監聽，取其資訊

```gherkin=
mSersorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//從系統服務中獲得感測器管理器
mAccrlerometers = mSersorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//管理器取得加速度感測器
mGyroscope = mSersorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);//管理器取得陀螺儀感測器

//SENSOR_DELAY_UI適合在UI空間中獲得數據，為四種模式裡更新速度最慢的了
//SensorManager_SENSOR_DELAY_FASTEST： 以最快的速度獲得感測器數據
//SENSOR_DELAY_GAME： 適合遊戲模式去獲得感測器數據
//SENSOR_DELAY_NORMAL： 以一般的速度獲得感測器數據
//SENSOR_DELAY_UI ：適合在UI空間中獲得感測器數據

mSersorManager.registerListener(accelerometerListener, mAccrlerometers, SensorManager.SENSOR_DELAY_UI);//對加速度感測器進行監聽
mSersorManager.registerListener(accelerometerListener, mGyroscope, SensorManager.SENSOR_DELAY_UI);//對陀螺儀感測器進行監聽

private SensorEventListener accelerometerListener = new SensorEventListener(){
@Override
public void onAccuracyChanged(Sensor arg0, int arg1) {}
//第一個值(values[0])代表手機的水平旋轉
//第二個值(values[1])代表手機的前後翻轉
//第三個值(values[2])代表手機的左右翻轉
//分别表示x,y,z轴的旋转的角速度
@Override
public void onSensorChanged(SensorEvent event) 
{
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
```
<h3>最後，將數據上傳server</h3>

最後，我將數據上傳至server的database，並採用mysql，利用[php檔](https://github.com/PoChuanHuang/Scan-Beacon-in-Android/blob/master/php/rssipost.php)將數據上傳。
這段code寫在function.java
「https://adslab.tk」是架在adslab最前面那台，用xampp架設。
```gherkin=
public static void PassData(Double rssi_data, int id)
{
    String url;
    Task task;
    switch(id)
    {
        case 1:
            url = "https://adslab.tk/rssipost.php?C2rssi="+rssi_data;
            task = new Task();
            task.execute(url);
            break;
        case 2:
            url = "https://adslab.tk/rssipost.php?BArssi="+rssi_data;
            task = new Task();
            task.execute(url);
            break;
        case 3:
            url = "https://adslab.tk/rssipost.php?AArssi="+rssi_data;
            task = new Task();
            task.execute(url);
            break;
        default:
            //nothing
    }
}
```

謝謝~
