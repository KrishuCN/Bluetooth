package com.example.TestTaxi.Hy_Bluetooth.manager;

import android.bluetooth.*;
import android.content.Context;
import com.example.TestTaxi.Hy_Bluetooth.BluetoothAttributes;
import com.example.TestTaxi.Hy_Bluetooth.BluetoothLeService;
import com.example.TestTaxi.Hy_Bluetooth.decoder.IDecoder;
import com.example.TestTaxi.Hy_Bluetooth.factory.DecoderFactory;
import com.example.TestTaxi.Hy_Bluetooth.thread.WriteThread;
import com.example.TestTaxi.Hy_Bluetooth.utils.CRCUtils;
import com.example.TestTaxi.Hy_Bluetooth.utils.HexStringUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;

/**
 * 设备管理类
 */
public class BluzManager implements BluetoothLeService.OnCharacteristicListener {

    private static BluzManager INSTANCE = null;
    private Context mContext;
    private BluetoothLeService mBluetoothLeService;

    /**
     * 缓存连接设备的蓝牙特征值和uuid
     */
    private HashMap<UUID, BluetoothGattCharacteristic> mWriteHashMap = new HashMap<>();

    private WriteThread mWriteThread;
    private DecoderFactory mDecoderFactory;

    private Context getContext() {
        return mContext;
    }

    /**
     * 蓝牙信号强度回调接口
     */
    private OnReadRemoteRssiListener listener;

    public interface OnReadRemoteRssiListener {
        void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);
    }

    public void setOnReadRemoteRssiListener(OnReadRemoteRssiListener listener) {
        this.listener = listener;
    }

    private BluzManager(Context context) {
        mContext = context;
        initialize();
    }

    /**
     * 单列模式，确保唯一性
     *
     * @param context 上下文
     * @return BluzManager蓝牙管理类
     */
    public static BluzManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BluzManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BluzManager(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 初始化相关数据
     */
    private void initialize() {
        //初始化蓝牙服务类
        mBluetoothLeService = BluetoothLeService.getInstance(getContext());
        mBluetoothLeService.setOnCharacteristicListener(this);
        //初始化解析工厂
        if (mDecoderFactory == null) mDecoderFactory = new DecoderFactory(getContext());
        //初始化写线程
        if (mWriteThread == null) mWriteThread = new WriteThread(mBluetoothLeService);
    }

    /**
     * 获取蓝牙信号强度,只能单次获取
     *
     * @return boolean
     */
    public boolean getRssiValue() {
        return mBluetoothLeService.getRssiValue();
    }

    /**
     * 清楚所有缓存数据
     */
    private void clearHashMap() {
        mWriteHashMap.clear();
    }

    /**
     * 蓝牙服务发现回调
     */
    @Override
    public void onServicesDiscovered() {
        clearHashMap();
        List<BluetoothGattService> list = mBluetoothLeService.getSupportedGattServices();
        for (BluetoothGattService bluetoothGattService : list) {
            UUID uuid = bluetoothGattService.getUuid();
            BluetoothLeService.log("蓝牙服务回调 uuid=" + uuid.toString());
            //TODO 根据报警设备提供的可用的特征服务UUID过滤出可用的服务以及特征值
            if (BluetoothAttributes.UUID_CHARACTERISTICS_SERVICE.equalsIgnoreCase(uuid.toString())) {
                List<BluetoothGattCharacteristic> characteristicList = bluetoothGattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristicList) {
                    //根据服务特征中的属性区分是可读、可写、通知。
                    int properties = characteristic.getProperties();
                    //拥有写权限的uuid放入集合中缓存起来，在需要使用的时候拿取出来。
                    if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                        mWriteHashMap.put(characteristic.getUuid(), characteristic);
                    }
                    //TODO 打开通知权限，具体根据报警设备给过来的UUID去修改
                    if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        if (mBluetoothLeService.setCharacteristicNotification(characteristic, true)) {
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                    UUID.fromString(BluetoothAttributes.UUID_RESPONSE_2902));
                            descriptor.setValue(ENABLE_NOTIFICATION_VALUE);
                            mBluetoothLeService.writeDescriptor(descriptor);
                        }
                    }
                }
            }
        }
        //如果缓存特征服务为空，表示服务回调失败了，可以尝试断开连接或者关闭系统蓝牙重新去连接。
        if (mWriteHashMap.size() == 0) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) bluetoothAdapter.disable();
            return;
        }
        //初始化写数据线程特征服务
        mWriteThread.initData(mWriteHashMap);
        //获取服务成功则允许数据写入,,断开连接则重置
        mWriteThread.setServiceCallback(true);
        BluetoothLeService.log("蓝牙服务回调：mWriteHashMap=" + mWriteHashMap);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        BluetoothLeService.log("蓝牙数据读取回调 status=" +status+ "收到的消息为: " + HexStringUtils.bytesToHexString(characteristic.getValue()));
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        BluetoothDevice device = mBluetoothLeService.getDevice();//当前连接的蓝牙对象
        if (characteristic == null || characteristic.getValue() == null || device == null) return;
        String data = HexStringUtils.bytesToHexString(characteristic.getValue());//将byte类型数据转换为16进制
        BluetoothLeService.log("蓝牙数据通知回调 uuid==" + characteristic.getUuid().toString() + ",data=" + data);
        // 工厂模式分别去解析数据
        IDecoder decoder = mDecoderFactory.getDecoder(data);
        if (decoder == null) return;
        decoder.decode(data);

    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        BluetoothLeService.log("蓝牙数据写入回调 status=" + status);
        if (status == BluetoothGatt.GATT_SUCCESS) {//数据写入成功，写入下一条指令
            mWriteThread.threadNotify(true);
        } else {
            mWriteThread.threadNotify(false);
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        if (listener != null) listener.onReadRemoteRssi(gatt, rssi, status);
    }

    /**
     * 时间同步
     */
    public void timeSynch() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int dayMon = calendar.get(Calendar.DAY_OF_MONTH);
        int week = calendar.get(Calendar.DAY_OF_WEEK);
        int dayWeek = week == 1 ? 6 : week - 2;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        byte[] content = new byte[10];
        content[0] = (byte) HexStringUtils.hexStringToAlgorism("c0");//头字段
        content[1] = (byte) 0x0a;//数据长度
        content[2] = HexStringUtils.str2Bcd((year % 100) + "")[0];//年 L
        content[3] = HexStringUtils.str2Bcd((year / 100) + "")[0];//年 H
        content[4] = HexStringUtils.str2Bcd(month + "")[0];//月
        content[5] = HexStringUtils.str2Bcd(dayMon + "")[0];//月--日
        content[6] = HexStringUtils.str2Bcd(dayWeek + "")[0];//星期---日
        content[7] = HexStringUtils.str2Bcd(hour + "")[0];//小时
        content[8] = HexStringUtils.str2Bcd(minute + "")[0]; //分钟
        content[9] = HexStringUtils.str2Bcd(second + "")[0];//秒
        mWriteThread.startWrite(getCrc16(content));
    }

    /**
     * 添加校验码
     *
     * @param content byte[]
     * @return 具备校验码的byte[]
     */
    private byte[] getCrc16(byte[] content) {
        int crc = CRCUtils.crc16_ccitt(content, content.length);//crc校验码
        byte crcL = (byte) (crc & 0xff);
        byte crcH = (byte) ((crc >> 8) & 0xff);
        byte[] bytes = new byte[content.length + 2];
        System.arraycopy(content, 0, bytes, 0, content.length);
        bytes[content.length] = crcL;
        bytes[content.length + 1] = crcH;
        return bytes;
    }

    /**
     * 停止写入数据
     */
    public void stopWrite() {
        if (mWriteThread != null) {
            mWriteThread.stopWrite();
            mWriteThread = null;
        }
    }

    /**
     * 蓝牙断开，清除剩余指令
     */
    public void clearOrder() {
        if (mWriteThread != null) {
            mWriteThread.setServiceCallback(false);
            mWriteThread.clearAll();
        }
    }

}
