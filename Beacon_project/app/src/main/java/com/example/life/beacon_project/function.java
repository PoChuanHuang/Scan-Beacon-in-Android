package com.example.life.beacon_project;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class function {
    public static boolean isConnected(Activity activity)
    {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {return true;}
        return false;
    }
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
}
