# Scan-Beacon-in-Android
This is an app which developed in Android Studio.
You can use it to scan nearby bluetooth and get some information.
(E.g UUID Major Rssi)


<h2>基本設定</h2>

偵測該設備是否支援藍芽，以及是否開啟藍芽，如果未開啟會跳出是否開啟藍芽視窗。
```gherkin=
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
```
<h2>Scan the Bluetooth Low Energy</h2>

In OnBatchScanResults , You can set which beacon's mac is you want.
```gherkin=
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
```
<h2>Scan the Classic Bluetooth</h2>

剛剛是BLE協定的掃瞄，現在這個是傳統藍芽協定的掃瞄。
因為要一直掃描，所以我用runnable的方式進行重複掃描(每三秒掃一次)
```gherkin=
 //**這是scan傳統藍芽的handler 每三秒掃一次
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable()
    {
        @Override
        public void run()
        {
            registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            mBluetoothAdapter.startDiscovery();
            handler.postDelayed(this, 3000);
        }
    };
```
利用BroadcastReceiver接收收到的藍芽信號
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
```
<h2>接收到BLE跟classic bluetooth的rssi後，轉換成距離</h2>

 
 After get the Designated device,
 We try to get the rssi value arraylist and do some process.
```gherkin=
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
}    
```

First,calculate the mean and sigma.
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
```
Use to Gaussian_blur
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


Use fun_distance() translate rssi to distance(m)
```gherkin=
public void fun_distance(Double data)//將rssi值套入公式轉成距離(m)
    {
        distance=0.0;
        distance = Math.pow(10,Math.abs(data-txpower)/(10*n));
    }
    public void fun_remove_arraylist(ArrayList<Double> data) //刪掉過多數量
    {
        if(data.size()>5)
        {
            data.remove(0);
            fun_remove_arraylist(data);
        }
    }
```
<h2>最後，將數據上傳server</h2>

And I update rssi value to server,I use xampp and the database is mysql.
```gherkin=
public void PassData(Double rssi_data,int id)
    {
        String url;
        Task task;
        if (function.isConnected(MainActivity.this))
        {
            switch(id)
            {
                case 1:
                    url = "http://(自行設定IP)/rssipost.php?C2rssi="+rssi_data;
                    task = new Task();
                    task.execute(url);
                    break;
                case 2:
                    url = "http://(自行設定IP)/rssipost.php?BArssi="+rssi_data;
                    task = new Task();
                    task.execute(url);
                    break;
                case 3:
                    url = "http://(自行設定IP)/rssipost.php?AArssi="+rssi_data;
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
```