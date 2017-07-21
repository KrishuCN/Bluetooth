package com.example.TestTaxi.Hy_Bluetooth.helper;


import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import com.example.TestTaxi.Hy_Bluetooth.BluetoothLeService;
import com.example.TestTaxi.Hy_Bluetooth.utils.SharePreferenceUtils;

import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;
import static com.example.TestTaxi.Hy_Bluetooth.BluetoothAttributes.REQUEST_CODE_BLUETOOTH_ON;

/**
 * 蓝牙设备搜索连接管理类
 */
@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class BluzScanHelper implements IBluzScanHelper, BluetoothLeService.OnConnectionStateChangeListener {

    private BluetoothLeService mBluetoothLeService;//蓝牙服务类
    private BluetoothAdapter bluetoothAdapter;//蓝牙适配器
    private BluetoothLeScanner mLeScanner = null;//api 5.0版本以上新的低功耗蓝牙搜索类
    private BleScanCallback mBleScanCallback = null;//api 5.0版本以上搜索回调接口

    private static IBluzScanHelper INSTANCE = null;
    private Context mContext = null;


    private Activity mActivity;
    public Context getContext() {
        return mContext;
    }

    private boolean isDiscovery = false;

    private boolean isDiscovery() {
        return isDiscovery;
    }

    private void setDiscovery(boolean discovery) {
        isDiscovery = discovery;
    }

    private static final int MSG_CONNECTED = 1;//连接
    private static final int MSG_DISCONNECTED = 2;//未连接
    /**
     * 10秒后停止查找搜索.
     */
    private static final int SCAN_PERIOD = 10 * 1000;

    private SharePreferenceUtils preferenceUtils = null;

    /**
     * 使用handler返回主线程,避免UI层直接操作而导致的崩溃
     */
    private Handler mHandler = new Handler(Looper.getMainLooper(),new Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECTED://连接
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    if (connectionListenerList != null && connectionListenerList.size() > 0 && device != null) {
                        for (int i = 0; i < connectionListenerList.size(); i++) {
                            connectionListenerList.get(i).onConnected(device);
                        }
                    }
                    break;
                case MSG_DISCONNECTED://未连接
                    for (int i = 0; i < connectionListenerList.size(); i++) {
                        connectionListenerList.get(i).onDisconnected(null);
                    }
                    break;
            }
            return false;
        }
    });

    public static IBluzScanHelper getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BluetoothLeService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BluzScanHelper(context);
                }
            }
        }
        return INSTANCE;
    }

    private BluzScanHelper(Context context,Activity... activities) {
        this.mContext = context;
        if (activities != null && activities.length > 0 && mActivity == null)
        {
            this.mActivity = activities[0];
        }
        //蓝牙服务
        mBluetoothLeService = BluetoothLeService.getInstance(context);
        mBluetoothLeService.setOnConnectionStateChangeListener(this);
        //蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            mBleScanCallback = new BleScanCallback();
        }else{
            BluetoothLeService.log("设备不支持蓝牙或低于5.0");
        }
        //SharePreference保存蓝牙断开行为是主动断开还是自动断开
        preferenceUtils = SharePreferenceUtils.getInstance(context);

    }

    @Override
    public void registBroadcast(Context context) {
        BluetoothLeService.log("注册监听系统蓝牙状态变化广播 ");
        IntentFilter filter = new IntentFilter();
        //蓝牙状态改变action
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        //蓝牙断开action
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        context.registerReceiver(receiver, filter);
    }

    @Override
    public void unregistBroadcast(Context context) {
        try {
            if (receiver != null) context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    @Override
    public boolean disable() {
        return bluetoothAdapter != null && bluetoothAdapter.disable();
    }

    @Override
    public boolean enable() {
        return bluetoothAdapter != null && bluetoothAdapter.enable();
    }

    @Override
    public void openBluetooth() {
        //打开蓝牙提示框
        Intent enableBtIntent = new Intent(ACTION_REQUEST_ENABLE);
        //TODO 需要传入Activity做处理
        BluetoothLeService.log("Helper获取的activity地址： "+mActivity);
        if (mActivity != null)
            mActivity.startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH_ON);
    }

    @Override
    public void startDiscovery() {
        if (bluetoothAdapter == null) {
            BluetoothLeService.log("开始搜索蓝牙失败，蓝牙适配器为null");
            return;
        }
        BluetoothLeService.log("开始搜索蓝牙 isDiscovering=" + bluetoothAdapter.isDiscovering());
        //正在查找中，不做处理
        if (isDiscovery()) return;
        setDiscovery(true);
        //判断版本号,如果api版本号大于5.0则使用最新的方法搜素
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            scanLeDevice(true); //TODO 因为ROM不同有可能会出现异常
        } else {
            if (!bluetoothAdapter.isEnabled()) return;
            mLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            mLeScanner.startScan(mBleScanCallback);
        }
    }

    @Override
    public void cancelDiscovery() {
        if (bluetoothAdapter == null) {
            BluetoothLeService.log("取消搜索蓝牙失败，蓝牙适配器为null");
            return;
        }
//        if (!isDiscovery()) {
//            Logger.e("取消搜索蓝牙失败，当前状态为取消状态！");
//            return;
//        }
        setDiscovery(false);//复位正在查找标志位
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            scanLeDevice(false);
        } else {
            stopLeScan();
        }
        BluetoothLeService.log("取消搜索蓝牙 isDiscovering=" + bluetoothAdapter.isDiscovering());
    }

    //轮询检查蓝牙是否开启
    @Override
    public void startCheckEnable(){
        new Thread(new Runnable() {
            public void run() {
                for (;;){
                    if (!isEnabled()&&!enable()){
                        openBluetooth();
                    }else {
                        return;
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).run();
    }

    /**
     * 低功耗蓝牙开始查找蓝牙
     *
     * @param enable 是否开始查找
     */
    private void scanLeDevice(boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopLeDevice();
                }
            }, SCAN_PERIOD);
            bluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            stopLeDevice();
        }
    }

    /**
     * 低功耗蓝牙停止查找
     */
    private void stopLeDevice() {
        bluetoothAdapter.stopLeScan(mLeScanCallback);
        for (int i = 0; i < discoveryListenerList.size(); i++) {
            discoveryListenerList.get(i).onDiscoveryFinished();
        }
    }

    //    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void stopLeScan() {
        if (!bluetoothAdapter.isEnabled()) return;
        mLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        mLeScanner.stopScan(mBleScanCallback);
        for (int i = 0; i < discoveryListenerList.size(); i++) {
            discoveryListenerList.get(i).onDiscoveryFinished();
        }
    }

    @Override
    public boolean connect(BluetoothDevice device) {
        if (device == null || mBluetoothLeService == null) return false;
        cancelDiscovery();//取消搜索
        return mBluetoothLeService.connect(device.getAddress());
    }

    @Override
    public void disconnect() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
            preferenceUtils.save(SharePreferenceUtils.SHARE_REFRESH_IS_MANUAL, true);
        }
    }

    @Override
    public void connected(BluetoothDevice device) {
        Message message = new Message();
        message.obj = device;
        message.what = MSG_CONNECTED;
        mHandler.sendMessage(message);
        preferenceUtils.save(SharePreferenceUtils.SHARE_REFRESH_IS_MANUAL, false);
        cancelDiscovery();//连接成功后取消搜索
    }

    @Override
    public void disconnected() {
        mHandler.sendEmptyMessage(MSG_DISCONNECTED);
    }

    @Override
    public void release() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        }
    }

    @Override
    public BluetoothDevice getConnectedDevice() {
        return mBluetoothLeService.getDevice();
    }

    private List<OnDiscoveryListener> discoveryListenerList = new ArrayList<>();//查找蓝牙回调接口集合
    private List<OnConnectionListener> connectionListenerList = new ArrayList<>();//连接蓝牙回调接口集合

    @Override
    public void addOnDiscoveryListener(OnDiscoveryListener listener) {
        if (!discoveryListenerList.contains(listener))
            discoveryListenerList.add(listener);
    }

    @Override
    public void removeOnDiscoveryListener(OnDiscoveryListener listener) {
        discoveryListenerList.remove(listener);
    }

    @Override
    public void addOnConnectionListener(OnConnectionListener listener) {
        connectionListenerList.add(listener);
    }

    @Override
    public void removeOnConnectionListener(OnConnectionListener listener) {
        connectionListenerList.remove(listener);
    }

    /**
     * 查找低功耗蓝牙接口回调
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            //注意在此方法中不要过多的操作
            if (device != null && discoveryListenerList != null) {
                for (int i = 0; i < discoveryListenerList.size(); i++) {
                    discoveryListenerList.get(i).onFound(device);
                }
            }
        }
    };

    /**
     * api21+低功耗蓝牙接口回调
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class BleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null) return;
            BluetoothDevice device = result.getDevice();
            if (device != null && discoveryListenerList != null) {
                for (int i = 0; i < discoveryListenerList.size(); i++) {
                    discoveryListenerList.get(i).onFound(device);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    }

    /**
     * 查找蓝牙广播回调
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothLeService.log("蓝牙广播回调 action=" + action);
            if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {//关闭系统蓝牙
                setDiscovery(false);
                BluetoothLeService.log("系统蓝牙断开！！");
                boolean isEnable = enable();
                if (!isEnable) openBluetooth();
                startCheckEnable();
            } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {//系统蓝牙打开
                setDiscovery(false);
                BluetoothLeService.log("系统蓝牙打开！！");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startDiscovery();
                    }
                },500);
            }

        }
    };
    //显示性开启蓝牙ActivityForResult需要activity实例
    public void setmApp(Activity mApp) {
        this.mActivity = mApp;
    }
}
