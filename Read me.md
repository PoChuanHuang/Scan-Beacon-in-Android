# Scan-Beacon-in-Android
This is an app which developed in Android Studio.
You can use it to scan nearby bluetooth and get some information.
(E.g UUID Major Rssi)
In OnBatchScanResults , You can set which beacon's mac is you want.
```gherkin=
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
 
 After get the Designated device,
 We try to get the rssi value arraylist and do some process.
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
    
    //再將前幾筆的刪掉，讓arraylist保持只有一定數量
    fun_remove_arraylist(average_rssi_D9D082EBDC69);
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
    else
    {
       fun_Gaussian_blur_and_time_of_weight(data,mean,sigma);
    }
}
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
    }
    else
    {
        new_data_mean = Math.sqrt(-(Math.log(g_mean*(Math.sqrt(2*Math.PI)*sigma))*2*Math.pow(sigma,2)))+mean;
        fun_distance(new_data_mean);
    }
}
```

fun_distance去做距離數算轉換
fun_remoe_arraylist去做arraylist將前幾筆的刪掉，保持只有一定數量
```gherkin=
public void fun_distance(Double data)
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









































