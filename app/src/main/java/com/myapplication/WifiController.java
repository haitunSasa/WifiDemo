package com.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.realtek.simpleconfiglib.SCLibrary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pc on 2016/3/7.
 */
public class WifiController {
    public WifiManager localWifiManager;//提供Wifi管理的各种主要API，主要包含wifi的扫描、建立连接、配置信息等
    //private List<ScanResult> wifiScanList;//ScanResult用来描述已经检测出的接入点，包括接入的地址、名称、身份认证、频率、信号强度等
    public List<WifiConfiguration> wifiConfigList = new ArrayList<>();//WIFIConfiguration描述WIFI的链接信息，包括SSID、SSID隐藏、password等的设置
    private WifiInfo wifiConnectedInfo;//已经建立好网络链接的信息
    private WifiManager.WifiLock wifiLock;//手机锁屏后，阻止WIFI也进入睡眠状态及WIFI的关闭
    private Context context;


    public WifiController(Context context) {
        this.context = context;
        localWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }


    //检查WIFI状态
    public int WifiCheckState() {
        return localWifiManager.getWifiState();
    }

    //开启WIFI
    public void WifiOpen() {
        if (!localWifiManager.isWifiEnabled()) {
            localWifiManager.setWifiEnabled(true);
        }
    }

    //关闭WIFI
    public void WifiClose() {
        if (!localWifiManager.isWifiEnabled()) {
            localWifiManager.setWifiEnabled(false);
        }
    }

    //扫描wifi
    public void WifiStartScan() {
        localWifiManager.startScan();
    }

    //得到Scan结果
    public List<ScanResult> getScanResults() {
        return localWifiManager.getScanResults();//得到扫描结果
    }

    //得到Wifi配置好的信息
    public void getConfiguration() {
        wifiConfigList = localWifiManager.getConfiguredNetworks();//得到配置好的网络信息
    }

    //判定指定WIFI是否已经配置好,依据WIFI的地址BSSID,返回NetId
    public boolean IsConfiguration(String SSID) {
        String ssid = "\"" + SSID + "\"";
        for (int i = 0; i < wifiConfigList.size(); i++) {
            if (ssid.equals(wifiConfigList.get(i).SSID)) {//地址相同
                return true;
            }
        }
        return false;
    }

    //添加指定WIFI的配置信息,原列表不存在此SSID
    public int AddWifiConfig(String ssid, String pwd) {
        localWifiManager.disconnect();
        int wifiId;
        WifiConfiguration wifiCong = new WifiConfiguration();
        wifiCong.SSID = "\"" + ssid + "\"";//\"转义字符，代表"
        wifiCong.preSharedKey = "\"" + pwd + "\"";//WPA-PSK密码
        wifiCong.hiddenSSID = false;
        wifiCong.status = WifiConfiguration.Status.ENABLED;
        wifiId = localWifiManager.addNetwork(wifiCong);//将配置好的特定WIFI密码信息添加,添加完成后默认是不激活状态，成功返回ID，否则为-1
        return wifiId;
    }

    /***
     * 配置要连接的WIFI热点信息
     *
     * @param SSID
     * @param password
     * @param type     加密类型
     * @return
     */
    public int AddWifiInfo(String SSID, String password, int type) {

        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        //增加热点时候 如果已经存在SSID 则将SSID先删除以防止重复SSID出现
        if (IsConfiguration(SSID)) {
            localWifiManager.removeNetwork(getConfigurationNetWorkId(SSID));
        }

        // 分为三种情况：没有密码   用wep加密  用wpa加密
        if (type == 1) {   // WIFICIPHER_N//OPASS
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        } else if (type == 2) {  //  WIFICIPHER_WEP
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == 3) {   // WIFICIPHER_WPA
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return localWifiManager.addNetwork(config);
    }


    //判定指定WIFI是否已经配置好,依据WIFI的地址BSSID,返回NetId
    public int getConfigurationNetWorkId(String SSID) {
        String ssid = "\"" + SSID + "\"";
        for (int i = 0; i < wifiConfigList.size(); i++) {
            if (wifiConfigList.get(i).SSID.equals(ssid)) {//地址相同
                return wifiConfigList.get(i).networkId;
            }
        }
        return -1;
    }

    public boolean removeNetWifi(int netId) {
        return localWifiManager.removeNetwork(netId);
    }

    public int getConnectStatus(String SSID) {
        String ssid = "\"" + SSID + "\"";
        for (int i = 0; i < wifiConfigList.size(); i++) {
            if (ssid.equals(wifiConfigList.get(i).SSID)) {//地址相同
                return wifiConfigList.get(i).status;
            }
        }
        return 100;
    }

    //连接指定Id的WIFI,返回是否连接成功
    public boolean ConnectWifi(int wifiId) {
        return localWifiManager.enableNetwork(wifiId, true);
    }

    public int WifiGetIpInt() {
        WifiInfo wifiInfo = this.localWifiManager.getConnectionInfo();
        return wifiInfo == null ? 0 : wifiInfo.getIpAddress();
    }

    //保存密码
    public boolean savePassword(String SSID, String password) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Password", Context.MODE_PRIVATE); //私有数据
        SharedPreferences.Editor editor = sharedPreferences.edit();//获取编辑器
        editor.putString("password", password);
        editor.putString("SSID", SSID);
        return editor.commit();
    }

    //获取密码
    public String getPassword(String SSID) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Network", Context.MODE_PRIVATE); //私有数据
        if (SSID.equals(sharedPreferences.getString("SSID", ""))) {
            String password = sharedPreferences.getString("password", "");
            return password;
        } else {
            return "";
        }
    }

    //获取当前WIFI信息
    public WifiInfo getCurrentWifiInfo() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        info.getIpAddress();
        if (info.getSupplicantState() == SupplicantState.COMPLETED) {
            return info;
        } else {
            return null;
        }
    }

    public static SCLibrary SCLib = new SCLibrary();
    ConfigurationDevice.DeviceInfo[] configuredDevices;

    static {
        System.loadLibrary("simpleconfiglib");
    }
    public void stopConfigure(){
        SCLib.rtk_sc_stop();
    }
    //配置
    public void Configure_action(Context context) {
        int connect_count = 200;
        WifiController wifiController = new WifiController(context);
        //get wifi ip
        int wifiIP = wifiController.WifiGetIpInt();
        while (connect_count > 0 && wifiIP == 0) {
            wifiIP = wifiController.WifiGetIpInt();
            connect_count--;
        }
        SCLib.rtk_sc_reset();

        SharedPreferences sharedPreferences = context.getSharedPreferences("Network", Context.MODE_PRIVATE);
        String password = sharedPreferences.getString("password", "");
        String SSID = sharedPreferences.getString("SSID", "");
        SCLib.rtk_sc_set_ssid(SSID);
        SCLib.rtk_sc_set_password(password);
        SCLib.rtk_sc_set_ip(wifiIP);
        SCLib.rtk_sc_build_profile();

		/* Profile(SSID+PASSWORD, contain many packets) sending total time(ms). */
        SCLibrary.ProfileSendTimeMillis = 12000;
        /* Time interval(ms) between sending two profiles. */
        SCLibrary.ProfileSendTimeIntervalMs = 50; //50ms
        /* Time interval(ms) between sending two packets. */
        SCLibrary.PacketSendTimeIntervalMs = 5; //0ms
		/* Each packet sending counts. */
        SCLibrary.EachPacketSendCounts = 1;

        SCLib.rtk_sc_start();
    }

    private void showConfiguredList() {
        final List<HashMap<String, Object>> InfoList = new ArrayList<HashMap<String, Object>>();
        String[] deviceList = null;
        SCLib.rtk_sc_stop();
        final int itemNum = SCLib.rtk_sc_get_connected_sta_num();

        SCLib.rtk_sc_get_connected_sta_info(InfoList);

        final boolean[] isSelectedArray = new boolean[itemNum];
        Arrays.fill(isSelectedArray, Boolean.TRUE);

        //input data
        if (itemNum > 0) {
            deviceList = new String[itemNum];
            for (int i = 0; i < itemNum; i++) {

                configuredDevices[i].setaliveFlag(1);

                if (InfoList.get(i).get("Name") == null)
                    configuredDevices[i].setName((String) InfoList.get(i).get("MAC"));
                else
                    configuredDevices[i].setName((String) InfoList.get(i).get("Name"));

                configuredDevices[i].setmacAdrress((String) InfoList.get(i).get("MAC"));
                configuredDevices[i].setIP((String) InfoList.get(i).get("IP"));

                deviceList[i] = configuredDevices[i].getName();
            }
        }
    }

}
