package com.jetec.wifisearchconnectdemo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName() + "My";
    WifiBroadcastReceiver wifiBroadcastReceiver = new WifiBroadcastReceiver();
    WifiManager wifiManager;
    RecyclerViewAdapter mAdapter;
    ConnectivityManager.NetworkCallback mNetwork;
    /**
     * 賦予handler重複執行掃描工作
     */
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            wifiScan();
        }
    };
    /**
     * 建立Runnable, 使掃描可被重複執行
     */
    Runnable searchWifi = new Runnable() {
        @Override
        public void run() {
            handler.sendEmptyMessage(1);
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**取得定位權限(搜尋Wifi要記得開定位喔)*/
        getPermission();
        /**取得WifiManager*/
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
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
//        mAdapter.onItemClick = scanResult -> {};//Lambda 表達式
        mAdapter.onItemClick = onItemClick;//點選RecyclerView中的物件後所做的事
    }

    @Override
    protected void onStop() {
        super.onStop();
        /**跳出畫面則停止掃描*/
        handler.removeCallbacks(searchWifi);
        /**斷開現在正連線著的Wifi(Wifi下篇新增內容)*/
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                /**若為Android10的手機，則在此執行斷線*/
                @SuppressLint("ServiceCast")
                ConnectivityManager connectivityManager =(ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                assert connectivityManager != null;
                connectivityManager.unregisterNetworkCallback(mNetwork);
            } else {
                /**非Android10手機，則執行WifiManager的斷線*/
                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                for (WifiConfiguration configuration : list){
                    wifiManager.removeNetwork(configuration.networkId);
                }
                wifiManager.disconnect();
                unregisterReceiver(wifiBroadcastReceiver);
            }
        }catch (Exception e){
            Log.i(TAG, "onStop: "+e.toString());
        }


    }
    /**取得權限*/
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

    private void wifiScan() {
        new Thread(() -> {
            /**設置Wifi回傳可被使用*/
            wifiManager.setWifiEnabled(true);
            /**開始掃描*/
            wifiManager.startScan();
            /**取得掃描到的Wifi*/
            List<ScanResult> wifiList = wifiManager.getScanResults();
            runOnUiThread(() -> {
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
    /**點擊指定的Wifi後執行連線(Wifi下篇新增內容)*/
    private RecyclerViewAdapter.OnItemClick onItemClick = scanResult -> {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectWifiQ(scanResult.SSID, "12345678");
        } else {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            getApplicationContext().registerReceiver(wifiBroadcastReceiver,filter);
            connectWifi(scanResult.SSID, "12345678");
        }

    };
    /**Android10↑的連線(Wifi下篇新增內容)*/
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectWifiQ(String ssid, String password) {
        /**Android10以上的手機必須調用WifiNetworkSpecifier*/
        /**官方說明文件:https://developer.android.com/reference/android/net/wifi/WifiManager#reconnect() */
        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build();
        NetworkRequest request =
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                        .setNetworkSpecifier(specifier)
                        .build();

        @SuppressLint("ServiceCast")
        ConnectivityManager connectivityManager =(ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        mNetwork = new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                assert connectivityManager != null;
                /**將手機網路綁定到指定Wifi*/
                connectivityManager.bindProcessToNetwork(network);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.w(TAG, "onUnavailable: 連線失敗");
            }
        };
        connectivityManager.requestNetwork(request,mNetwork);
    }
    /**Android10↓的連線(Wifi下篇)*/
    private void connectWifi(String tagSsid, String tagPassword){
        String ssid = "\""+tagSsid+"\"";
        String password = "\"" + tagPassword + "\"";
        WifiConfiguration conf = new WifiConfiguration();
        conf.allowedProtocols.clear();
        conf.allowedAuthAlgorithms.clear();
        conf.allowedGroupCiphers.clear();
        conf.allowedKeyManagement.clear();
        conf.allowedPairwiseCiphers.clear();
        conf.SSID = ssid;
        conf.preSharedKey = password;
        conf.status = WifiConfiguration.Status.ENABLED;
        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiManager.addNetwork(conf);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration configuration : list){
            if (configuration.SSID != null && conf.SSID.equals(ssid)){
                /**斷開原先的Wifi*/
                wifiManager.disconnect();
                /**連接指定的Wifi*/
                wifiManager.enableNetwork(conf.networkId,true);
                wifiManager.reconnect();
                break;
            }
        }

    }
    /**廣播Wifi的所有狀態(Wifi下篇新增內容)*/
    private class WifiBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())){
                switch (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)){
                    case WifiManager.WIFI_STATE_DISABLED:
                        Log.i(TAG, "Wifi關閉中");
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        Log.i(TAG, "關閉Wifi中");
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        Log.i(TAG, "Wifi使用中");
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        Log.i(TAG, "開啟Wifi中");
                        break;
                }
            }else if((WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction()))){
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                assert info != null;
                if (NetworkInfo.State.DISCONNECTED == info.getState()){
                    Toast.makeText(context, "Wifi已斷線", Toast.LENGTH_SHORT).show();
                }else if(NetworkInfo.State.CONNECTED == info.getState()){
                    Toast.makeText(context, "Wifi已連接", Toast.LENGTH_SHORT).show();
                }else if(NetworkInfo.State.CONNECTING == info.getState()){
                    Log.i(TAG, "Wifi連線中");
                }
            }
        }
    }
}