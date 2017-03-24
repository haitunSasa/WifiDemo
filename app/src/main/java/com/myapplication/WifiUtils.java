package com.myapplication;

import android.content.Context;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.realtek.simpleconfiglib.SCLibrary;
import com.myapplication.ConfigurationDevice.DeviceInfo;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

/**
 * Created by Administrator on 2017/3/13.
 */

public class WifiUtils {
    List<DeviceInfo> configuredDevices = new ArrayList<>();
    public static SCLibrary SCLib = new SCLibrary();
    private WifiController wifiController;
    private Context context;
    private String wifiPassword;
    private String SSID;

    public static final int CfgSuccessACK = 0x00;

    /**
     * 加载动态链接库
     */
    static {
        System.loadLibrary("simpleconfiglib");
    }

    public WifiUtils(Context context) {
        this.context = context;
        wifiController = new WifiController(context);
        SCLib.rtk_sc_init();
        SCLib.TreadMsgHandler = new MsgHandler();
        SCLib.WifiInit(context);
    }

    /**
     * 获取SSID
     *
     * @return
     */
    public String getSSID() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        wifiController.getConfiguration();
        if (info.getSupplicantState() == SupplicantState.COMPLETED) {
            SSID = info.getSSID();
            Log.d("connectSSID", SSID);
        }
        //没有连接热点
        if (SSID == "<unknown ssid>") {
            return null;
        }
        return SSID;
    }

    /**
     * 判断wifi密码是否正确
     *
     * @param wifiPassword
     * @return
     */
    public boolean isPasswordRight(String wifiPassword) {
        int netWorkId = wifiController.getConfigurationNetWorkId(SSID);

        while (netWorkId != -1) {
            netWorkId = wifiController.localWifiManager.getConnectionInfo().getNetworkId();
            wifiController.localWifiManager.disconnect();
            wifiController.getConfiguration();
        }
        if (!wifiPassword.equals("")) {
            int netId = wifiController.AddWifiConfig(SSID, wifiPassword);
            if (netId != -1) {
                wifiController.getConfiguration();//添加了配置信息，要重新得到配置信息
                //boolean connectWifi = ;
                return wifiController.ConnectWifi(netId);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public String getWifiPassword() {
        return wifiPassword;
    }

    public void setWifiPassword(String wifiPassword) {
        this.wifiPassword = wifiPassword;
    }

    public List<DeviceInfo> getConfiguredDevices(){
        List<HashMap<String, Object>> InfoList = new ArrayList<>();
        SCLib.rtk_sc_get_connected_sta_info(InfoList);
        for (int i = 0; i < InfoList.size(); i++) {
            ConfigurationDevice.DeviceInfo configDevice = new ConfigurationDevice.DeviceInfo();
            configDevice.setaliveFlag(1);
            configDevice.setName((String) InfoList.get(i).get("MAC"));
            configDevice.setmacAdrress((String) InfoList.get(i).get("MAC"));
            configDevice.setIP((String) InfoList.get(i).get("IP"));
            configuredDevices.add(configDevice);
        }
        return configuredDevices;
    }

    public void setConfigure() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        //get wifi ip
        int wifiIP = info.getIpAddress();
        Log.i("wifiIP", wifiIP + "");

        while (wifiIP == 0) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            info = wifiManager.getConnectionInfo();
            wifiIP = info.getIpAddress();
        }
        Log.i("wifiIP",String.format("%d.%d.%d.%d",
                wifiIP >> 0 & 0xFF,
                wifiIP >> 8 & 0xFF,
                wifiIP >> 16 & 0xFF,
                wifiIP >> 24 & 0xFF));
        SCLib.rtk_sc_reset();
        SCLib.rtk_sc_set_pin(null);
        SCLib.rtk_sc_set_ssid(SSID);
        SCLib.rtk_sc_set_password(wifiPassword);
        SCLib.rtk_sc_set_ip(wifiIP);
        SCLib.rtk_sc_build_profile();
        initSCLibrary(1);
        exception_action();
        SCLib.rtk_sc_start();
    }

    public void initSCLibrary(int flag) {
        switch (flag) {
            case 1:
                SCLibrary.ProfileSendTimeIntervalMs = 50; //50ms
                /* Time interval(ms) between sending two packets. */
                SCLibrary.PacketSendTimeIntervalMs = 5; //0ms
                /* Each packet sending counts. */
                SCLibrary.EachPacketSendCounts = 1;
                break;
            case 2:
                /* Time interval(ms) between sending two profiles. */
                SCLibrary.ProfileSendTimeIntervalMs = 200; //200ms
			    /* Time interval(ms) between sending two packets. */
                SCLibrary.PacketSendTimeIntervalMs = 10; //10ms
			    /* Each packet sending counts. */
                SCLibrary.EachPacketSendCounts = 1;
                break;
            default:
                break;
        }
    }
    public void stopSCLibrary(){
        SCLib.rtk_sc_stop();
    }

    public void exit() {
        SCLib.rtk_sc_exit();
    }

    private void exception_action() {
        if (Build.MANUFACTURER.equalsIgnoreCase("Samsung")) {
            //SCLibrary.PacketSendTimeIntervalMs  = 5;
            if (Build.MODEL.equalsIgnoreCase("G9008")) { //Samsung Galaxy S5 SM-G9008
                SCLibrary.PacketSendTimeIntervalMs = 10;
            } else if (Build.MODEL.contains("SM-G9208")) { //samsun Galaxy S6
                SCLibrary.PacketSendTimeIntervalMs = 10;
            } else if (Build.MODEL.contains("N900")) { //samsun Galaxy note 3
                SCLibrary.PacketSendTimeIntervalMs = 5;
            } else if (Build.MODEL.contains("SM-N910U")) { //samsun Galaxy note 4
                SCLibrary.PacketSendTimeIntervalMs = 5;
            }

        } else if (Build.MANUFACTURER.equalsIgnoreCase("Xiaomi")) {//for MI
            if (Build.MODEL.equalsIgnoreCase("MI 4W")) {
                SCLibrary.PacketSendTimeIntervalMs = 5;    //MI 4
            }
        } else if (Build.MANUFACTURER.equalsIgnoreCase("Sony")) {//for Sony
            if (Build.MODEL.indexOf("Xperia") > 0) {
                SCLibrary.PacketSendTimeIntervalMs = 5;    //Z3
            }
        } else if (Build.MANUFACTURER.equalsIgnoreCase("HUAWEI")) {//HUAWEI
            if (Build.MODEL.indexOf("GEM-702L") > 0) {
                SCLibrary.PacketSendTimeIntervalMs = 10;    //GEM-702L
            } else {
                SCLibrary.PacketSendTimeIntervalMs = 5;
            }
        }
        //check link rate
        WifiManager wifi_service = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiinfo = wifi_service.getConnectionInfo();
        if (wifiinfo.getLinkSpeed() > 78) {//MCS8 , 20MHZ , NOSGI
            SCLibrary.ProfileSendTimeIntervalMs = 100; //50ms
            SCLibrary.PacketSendTimeIntervalMs = 15;
        }
    }
    boolean configFlag;
    public boolean getFlag(){
        return this.configFlag ;
    }

    /**
     * Handler class to receive send/receive message
     */
    private class MsgHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case ~CfgSuccessACK://Config Timeout
                    Log.d("MsgHandler", "Config Timeout");
                    SCLib.rtk_sc_stop();
                    break;
                case CfgSuccessACK: //Not Showable
                    Log.d("MsgHandler", "Config SuccessACK");
                    SCLib.rtk_sc_stop();
                    configFlag=true;
                    List<HashMap<String, Object>> InfoList = new ArrayList<>();
                    SCLib.rtk_sc_get_connected_sta_info(InfoList);
                    break;
                default:
                    Log.d("MsgHandler", "default");
                    break;
            }
        }
    }
    public void aVoid() {
        Subscriber subscriber= new RXImpl();

        Flowable.create(new FlowableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(FlowableEmitter<Boolean> e) throws Exception {
                configFlag=true;
            }
        }, BackpressureStrategy.BUFFER)

                .subscribe(subscriber);

    }



}
