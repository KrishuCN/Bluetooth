package com.example.TestTaxi.Hy_Bluetooth;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import com.example.TestTaxi.Hy_Bluetooth.helper.BluzScanHelper;
import com.example.TestTaxi.Hy_Bluetooth.helper.IBluzScanHelper;
import com.example.TestTaxi.Hy_Bluetooth.manager.BluzManager;
import com.example.TestTaxi.R;

public class BluetoothService extends Service {

    private BluetoothDevice mDevice = null;
    private IBluzScanHelper mIBluzHelper;
    private BluzManager mBluzManager = null;

    private final int MSG_START_DISCOVERY = 100;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_START_DISCOVERY) {
                mIBluzHelper.startDiscovery();
            }
            return false;
        }
    });


    @Override
    public void onCreate() {
        super.onCreate();
        // 构建前台 Notification
        Notification notification = new Notification.Builder(this.getApplicationContext())
                .setContentTitle("报警服务运行中")
                .setSmallIcon(R.drawable.ic_launcher)
                .build();
        startForeground(9988, notification);
        new Thread(new Runnable() {
            @Override
            public void run() {
                initBluzManager();
                initBluzHelper();
            }
        }).start();
    }

    private void initBluzHelper() {
                mIBluzHelper = BluzScanHelper.getInstance(BluetoothService.this);
                mIBluzHelper.registBroadcast(BluetoothService.this);
                mIBluzHelper.addOnDiscoveryListener(mDiscoveryListener);
                mIBluzHelper.addOnConnectionListener(mConnectionListener);
//                if (!mIBluzHelper.isEnabled() && mIBluzHelper.enable()) mIBluzHelper.openBluetooth();
                mIBluzHelper.startCheckEnable();
                if (mDevice == null) mIBluzHelper.startDiscovery();
    }

    private void initBluzManager() {
        mBluzManager = BluzManager.getInstance(BluetoothService.this);
    }

    private void release() {
        mIBluzHelper.unregistBroadcast(this);
        mIBluzHelper.release();
        mBluzManager.stopWrite();
    }

    @Override
    public void onDestroy() {
//        release();
        startService(new Intent(this, BluetoothService.class));
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    /**
     * 蓝牙查找回调
     */
    private IBluzScanHelper.OnDiscoveryListener mDiscoveryListener = new IBluzScanHelper.OnDiscoveryListener() {

        @Override
        public void onFound(BluetoothDevice device) {
            BluetoothLeService.log("找到设备,device name = " + device.getName() + ",address = " + device.getAddress());
            //可以根据蓝牙名称或者mac地址区分判断
            if ("T91proAC68".equals(device.getName())) {
                boolean isSuccess = mIBluzHelper.connect(device);
                if (!isSuccess) return;
                BluetoothService.this.mDevice = device;
            }
        }

        @Override
        public void onDiscoveryFinished() {
            BluetoothLeService.log("搜索结束 mDevice = " + mDevice);
            if (mDevice == null) mHandler.sendEmptyMessageDelayed(MSG_START_DISCOVERY, 500);
        }
    };


    /**
     * 蓝牙连接回调
     */
    private IBluzScanHelper.OnConnectionListener mConnectionListener = new IBluzScanHelper.OnConnectionListener() {
        @Override
        public void onConnected(BluetoothDevice device) {
            BluetoothLeService.log("连接设备成功");
            BluetoothService.this.mDevice = device;
            mBluzManager.timeSynch();
        }

        @Override
        public void onDisconnected(BluetoothDevice device) {
            BluetoothLeService.log("设备断开");
            BluetoothService.this.mDevice = null;
            mBluzManager.clearOrder();//蓝牙断开清除之前预留的指令
            if (!mIBluzHelper.isEnabled()) return;
            mIBluzHelper.disconnect();
            mHandler.sendEmptyMessageDelayed(MSG_START_DISCOVERY, 1000);
        }
    };
}
