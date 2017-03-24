package com.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.reactivestreams.Subscriber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

public class MainActivity extends Activity {

    private static final int configTimeout = 120000;//120s

    boolean ConnectAPProFlag = false;//user need to connect ap
    boolean ConfigureAPProFlag = false;//user need to connect ap
    boolean TimesupFlag_cfg = true;
    boolean ShowCfgSteptwo = false;

    private ProgressDialog pd;

    protected WifiManager mWifiManager;
    private WifiUtils wifiUtils;
    Thread backgroundThread = null;

    // handler for the background updating
    Handler progressHandler = new Handler() {
        public void handleMessage(Message msg) {
            pd.incrementProgressBy(1);
        }
    };

    Handler handler_pd = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    pd.dismiss();
                    break;
                case 1:
                    int timeout = 10;
                    int coutDown = timeout;

                    while (coutDown > 0) {

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        coutDown--;
                        if (coutDown == 0) {
                            pd.dismiss();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    //private SCLibrary SCLib = new SCLibrary();

/*    static {
        System.loadLibrary("simpleconfiglib");
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //// TODO: 2017/3/15 初始化
        //SCLib.rtk_sc_init();
        //SCLib.TreadMsgHandler = new MsgHandler();
        wifiController = new WifiController(this);
        editText = (EditText) findViewById(R.id.et_password);
        //wifi manager init
        //SCLib.WifiInit(this);
        wifiUtils=new WifiUtils(this);

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

    }


    @Override
    protected void onDestroy() {
        wifiUtils.SCLib.rtk_sc_exit();
        super.onDestroy();
    }

    private EditText editText;
    String wifiPassword;
    WifiController wifiController;
    String tempSSID = "";

    //<func>
    public void configNewDevice_OnClick(View view) {

        //SCLib.WifiStartScan();
        // TODO: 2017/3/15 获取wifi连接的SSID
        tempSSID=wifiUtils.getSSID();
        Log.i("tempSSID",tempSSID+"");
        // TODO: 2017/3/15 wifi密码
        wifiPassword = editText.getText().toString().trim();
        Log.i("wifiPassword", wifiPassword + "");
        Log.i("isPasswordRight", wifiUtils.isPasswordRight(wifiPassword) + "");
        if(wifiUtils.isPasswordRight(wifiPassword)){
            wifiUtils.setWifiPassword(wifiPassword);
            startToConfigure();
        };
        //// TODO: 2017/3/15

    }

    //<func>
    public void startToConfigure() {
        ConfigureAPProFlag = true;
        pd = new ProgressDialog(MainActivity.this);
        pd.setCancelable(true);
        pd.setTitle("Configure New Device");
        pd.setCancelable(false);
        pd.setMessage("Configuring...");
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setProgress(0);
        pd.setMax(100);
        pd.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                ConfigureAPProFlag = false;
                TimesupFlag_cfg = true;
                wifiUtils.SCLib.rtk_sc_stop();
                backgroundThread.interrupt();
            }
        });
        pd.show();
        // create a thread for updating the progress bar
        backgroundThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (pd.getProgress() <= pd.getMax()) {
                        Thread.sleep(1200);
                        progressHandler.sendMessage(progressHandler.obtainMessage());
                    }
                } catch (java.lang.InterruptedException e) {
                }
            }
        });
        backgroundThread.start();

        Thread ConfigDeviceThread = new Thread() {
            @Override
            public void run() {
                Configure_action();
                //wait dialog cancel
                if (ConnectAPProFlag == false) {
                    pd.setProgress(100);
                    backgroundThread.interrupt();
                    handler_pd.sendEmptyMessage(0);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ConfigureAPProFlag == true) {
                            //show "start to configure"
                            ConfigureAPProFlag = false;
                            Log.i("showConfiguredList", "showConfiguredList");
                            showConfiguredList();
                        }
                    }
                });
            }
        };
        ConfigDeviceThread.start();
    }

    //<func>
    private void Configure_action() {
        Log.i("Configure_action", "Configure_action");
        int stepOneTimeout = 30000;
        wifiUtils.setConfigure();

        TimesupFlag_cfg = false;

        int watchCount = 0;
        try {
            do {
                Thread.sleep(1000);
                watchCount += 1000;
            } while (!wifiUtils.getFlag() && watchCount < stepOneTimeout);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //==================== 2 =========================
        if (TimesupFlag_cfg == false) {
            int count = 0;
			wifiUtils.initSCLibrary(2);

            try {
                do {
                    Thread.sleep(1000);
                    count++;
                    if ((((configTimeout - stepOneTimeout) / 1000) - count) < 0)
                        break;
                } while (!wifiUtils.getFlag());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            TimesupFlag_cfg = true;
            //Log.d("=== Configure_action ===","rtk_sc_stop 2");
            wifiUtils.stopSCLibrary();
        }

    }

    List<ConfigurationDevice.DeviceInfo> configuredDevices = new ArrayList<>();

    //<func>
    private void showConfiguredList() {
        ShowCfgSteptwo = false;
        ConfigureAPProFlag = false;
        wifiUtils.stopSCLibrary();
        handler_pd.sendEmptyMessage(0);

        configuredDevices=wifiUtils.getConfiguredDevices();
        int itemNum = configuredDevices.size();

        final boolean[] isSelectedArray = new boolean[itemNum];
        Arrays.fill(isSelectedArray, Boolean.TRUE);
        String[] deviceList;
        //input data
        if (itemNum > 0) {
            deviceList = new String[itemNum];
            for (int i = 0; i < itemNum; i++) {
                deviceList[i] = configuredDevices.get(i).getName();
            }
        } else {
            if (TimesupFlag_cfg == true) {
                AlertDialog.Builder alert_timeout = new AlertDialog.Builder(MainActivity.this);
                alert_timeout.setCancelable(false);
                //switch password input type
                alert_timeout.setTitle("Configure Timeout");
                alert_timeout.setCancelable(false);
                alert_timeout.setPositiveButton("OK", null);
                alert_timeout.show();
            }

            handler_pd.sendEmptyMessage(0);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(false);

        builder.setTitle("Configured Device");
        builder.setIcon(android.R.drawable.ic_dialog_info);

        builder.setMultiChoiceItems(deviceList, isSelectedArray,
                new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        isSelectedArray[which] = isChecked;
                    }
                });
        builder.setPositiveButton("Confirm", null);
        builder.create();
        builder.show();
    }

    /**
     * Definition of the list adapter...uses the View Holder pattern to
     * optimize performance.
     */
    private Runnable Cfg_changeMessage = new Runnable() {
        @Override
        public void run() {
            //Log.v(TAG, strCharacters);
            Context context;

            pd.setMessage("Waiting for the device");
        }
    };



   /* *//**
     * Handler class to receive send/receive message
     *//*
    private class MsgHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "msg.what: " + msg.what);
            switch (msg.what) {
                case ~SCCtlOps.Flag.CfgSuccessACK://Config Timeout
                    Log.d("MsgHandler", "Config Timeout");
                    wifiUtils.SCLib.rtk_sc_stop();
                    break;
                case SCCtlOps.Flag.CfgSuccessACK: //Not Showable
                    Log.d("MsgHandler", "Config SuccessACK");
                    wifiUtils.SCLib.rtk_sc_stop();
                    TimesupFlag_cfg = true;
                    if (ShowCfgSteptwo)
                        runOnUiThread(Cfg_changeMessage);
                    List<HashMap<String, Object>> InfoList = new ArrayList<>();
                    wifiUtils.SCLib.rtk_sc_get_connected_sta_info(InfoList);
                    break;
                default:
                    Log.d("MsgHandler", "default");
                    break;
            }
        }
    }*/
}

