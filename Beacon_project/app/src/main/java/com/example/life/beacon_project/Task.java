package com.example.life.beacon_project;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Task extends AsyncTask<String,Void,Void> {
    String TAG = "Task_log";
    @Override
    protected Void doInBackground(String...params)
    {
        // 執行中，在背景做任務
        BufferedReader bufferedReader = null;
        StringBuilder stringBuilder;
        try
        {
            URL url = new URL(params[0]);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            Log.d(TAG,"params: "+params[0]);
            Log.d(TAG,"url: "+url);
            connection.setRequestMethod("POST");// 使用甚麼方法做連線
            connection.setDoOutput(true);// 是否添加參數(ex : json...等)
            connection.setReadTimeout(15*1000); // 設定TimeOut時間
            connection.connect();
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {stringBuilder.append(line + "\n");}

        } catch (Exception e) {e.printStackTrace();Log.d(TAG,"Network connect failed!");}
        finally {if (bufferedReader != null) {try {bufferedReader.close();} catch (IOException e) {e.printStackTrace();}}}
        return null;
    }
}
