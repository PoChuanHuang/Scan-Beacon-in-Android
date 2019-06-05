# Scan-Beacon-in-Android
This is an app which developed in Android Studio.
You can use it to scan nearby bluetooth and get some information.
(E.g UUID Major Rssi)
In OnBatchScanResults , You can set which beacon's mac is you want.
 ```bluetooth_scan=
super.onBatchScanResults(results);
boolean t=false;
for(ScanResult scanResult : results)
{
    Double rssi = Double.valueOf(scanResult.getRssi());
    switch (scanResult.getDevice().getAddress())
    {
        case "5C:31:3E:31:02:85":
            average_rssi_5C313E310285.add(rssi);t=true;
            break;
        case "7C:EC:79:6F:47:64":
            average_rssi_7CEC796F4764.add(rssi);t=true;
            break;
        case "A8:1B:6A:B3:0F:67":
            average_rssi_A81B6AB30F67.add(rssi);t=true;
            break;
        default:
            Log.d(TAG,"不是我們的bluetooth");
    }
} 
```
 
 After get the Designated device,We try to get the rssi value arraylist and do some process.
```gherkin=
if(t)
{
    //將剛剛得到裡面有很多rssi的average_rssi_5C313E310285，放到array_data
    array_data = new ArrayList(average_rssi_5C313E310285);
    //然後就開啟進行處理
    fun_mean_and_sigma(array_data);
    //最後將rssi和distance print 出來
    rssi_textview.setText("rssi[]="+array_data);
    distance_textview.setText("distance="+distance+"m");
    //再將前幾筆的刪掉，讓arraylist倒持只有一定數量
    fun_remove_arraylist(average_rssi_D9D082EBDC69);
}    
```



