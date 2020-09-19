package com.jetec.wifisearchconnectdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    WifiManager wifiManager;
    RecyclerViewAdapter mAdapter;
    /**賦予handler重複執行掃描工作*/
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            wifiScan();
        }
    };
    /**建立Runnable, 使掃描可被重複執行*/
    Runnable searchWifi = new Runnable() {
        @Override
        public void run() {
            handler.sendEmptyMessage(1);
            handler.postDelayed(this,5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**取得定位權限(搜尋Wifi要記得開定位喔)*/
        getPermission();
        /**取得WifiManager*/
        wifiManager  = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        /**設置顯示回傳的Recyclerview*/
        RecyclerView recyclerView = findViewById(R.id.recyclerVIew_SearchResult);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        /**為Recyclerview每個項目之間加入分隔線*/
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.HORIZONTAL));
        mAdapter = new RecyclerViewAdapter();
        recyclerView.setAdapter(mAdapter);
        /**按下按鈕則執行掃描*/
        Button btScan = findViewById(R.id.button);
        btScan.setOnClickListener(v -> {
            handler.post(searchWifi);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        /**跳出畫面則停止掃描*/
        handler.removeCallbacks(searchWifi);
    }

    private void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100
            );
        }//if
    }

    private void wifiScan(){
        new Thread(()->{
            /**設置Wifi回傳可被使用*/
            wifiManager.setWifiEnabled(true);
            /**開始掃描*/
            wifiManager.startScan();
            /**取得掃描到的Wifi*/
            List<ScanResult> wifiList = wifiManager.getScanResults();
            runOnUiThread(()->{
                /**更新掃描後的列表*/
                mAdapter.addItem(wifiList);
            });
        }).start();
        /**以下為掃描到的Wifi可被取得的相關資訊, 供參考*/

//                for (int i = 0; i <wifiList.size() ; i++) {
//                    ScanResult s = wifiList.get(i);
//                    Log.d(TAG, "run: "+s.SSID+"\n"
//                    +s.BSSID+"\n"
//                    +s.capabilities+"\n"
//                    +s.centerFreq0+"\n"
//                    +s.centerFreq1+"\n"
//                    +s.channelWidth+"\n"
//                    +s.frequency+"\n"
//                    +s.level+"\n"
//                    +s.operatorFriendlyName+"\n"
//                    +s.timestamp+"\n"
//                    +s.venueName+"\n");
//
//                }
    }
}