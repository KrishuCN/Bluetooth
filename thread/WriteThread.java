package com.example.TestTaxi.Hy_Bluetooth.thread;

import android.bluetooth.BluetoothGattCharacteristic;
import com.example.TestTaxi.Hy_Bluetooth.BluetoothAttributes;
import com.example.TestTaxi.Hy_Bluetooth.BluetoothLeService;
import com.example.TestTaxi.Hy_Bluetooth.model.Cell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * 蓝牙数据写入线程
 */
public class WriteThread extends Thread {

    private BluetoothLeService mBluetoothLeService;
    private HashMap<UUID, BluetoothGattCharacteristic> mGattHashMap;

    public WriteThread(BluetoothLeService mBluetoothLeService) {
        this.mBluetoothLeService = mBluetoothLeService;
        this.start();
    }

    public void initData(HashMap<UUID, BluetoothGattCharacteristic> mGattHashMap) {
        this.mGattHashMap = mGattHashMap;
    }

    /**
     * 堆栈管理写数据
     */
    private List<Cell> mWriteList = new ArrayList<>();
    /**
     * 允许失败最大次数
     */
    private static int FAIL_COUNT = 3;
    /**
     * 报警设备超时响应次数 3
     */
    private static int TIME_OUT = 3;
    /**
     * 是否发送下一条命令
     */
    private boolean isSendNextOrder = false;
    /**
     * 蓝牙服务是否回调
     */
    private boolean isServiceCallback = false;

    public boolean isServiceCallback() {
        return isServiceCallback;
    }

    public void setServiceCallback(boolean serviceCallback) {
        isServiceCallback = serviceCallback;
    }

    /**
     * 是否暂停
     */
    private boolean stoped = false;

    private void setStoped(boolean stoped) {
        this.stoped = stoped;
    }

    private boolean isStoped() {
        return stoped;
    }

    /**
     * 获取当前堆栈数量
     *
     * @return count
     */
    public int getWriteListSize() {
        return mWriteList.size();
    }

    @Override
    public void run() {
        while (!isStoped()) {//循环写入数据
            Cell cell = popCell();//从堆栈中拿取数据
            if (isServiceCallback() && cell != null) {//判断当前蓝牙服务是否回调了，堆栈中的数据是否为null
                boolean res = writeCharcteristic(cell.getBuffer());//开始写入
                BluetoothLeService.log("蓝牙数据写入状态 res=" + res);

                if (!res) {//写入失败
                    if (FAIL_COUNT > 0) {//循环多次尝试发送
                        FAIL_COUNT--;//次数自减
                        threadSleep(500);//休眠500毫秒，蓝牙数据操作不易发送过于频繁，容易导致断开
                    } else {//多次发送失败，堆栈中移除当前指令，发送下一条指令
                        mWriteList.remove(0);
                        //如果当前堆栈没有数据，且多次发送失败，断开连接。
                        if (getWriteListSize() == 0) {
                            mBluetoothLeService.disconnect();
                            BluetoothLeService.log("多次发送失败，断开连接!!!");
                        }
                        FAIL_COUNT = 3;//次数重置
                    }
                } else {//写入成功
                    FAIL_COUNT = 3;//次数重置
                    threadWait(4 * 1000);//线程等待4秒钟，此处时间可以根据硬件数据回调快慢等实际情况去修改。
                    if (!isSendNextOrder && TIME_OUT > 0) {//继续发送当前命令，直到循环结束
                        TIME_OUT--;
                        threadSleep(500);
                    } else {//发送下一条命令,从堆栈中移除当前的命令
                        mWriteList.remove(0);
                        //如果当前堆栈没有数据，且已经走过超时流程，直接回调调用下一条指令
                        if (TIME_OUT == 0 && getWriteListSize() == 0) {
                            BluetoothLeService.log("手环回调超时!!!");
                        }
                        TIME_OUT = 3;
                        threadSleep(500);
                    }
                }
                isSendNextOrder = false;
            }
        }
    }

    /**
     * 是否被主动唤醒
     *
     * @param isSendNextOrder 是否发送下一条命令
     */
    public void threadNotify(boolean isSendNextOrder) {
        BluetoothLeService.log("写入线程被唤醒，是否发送下一条命令 isSendNextOrder=" + isSendNextOrder);
        this.isSendNextOrder = isSendNextOrder;
        try {
            synchronized (this) {
                this.notify();
            }
        } catch (IllegalMonitorStateException e) {
            e.printStackTrace();
        }
    }

    /**
     * 线程休眠
     */
    private void threadWait(long millis) {
        try {
            synchronized (this) {
                this.wait(millis);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 线程休眠
     *
     * @param millis 休眠时间 毫秒
     */
    private void threadSleep(long millis) {
        try {
            sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将数据放入堆栈
     *
     * @param buffer 传输数据
     */
    private void pushCell(byte[] buffer) {
        Cell cell = new Cell(buffer);
        mWriteList.add(cell);
    }

    /**
     * 将数据从堆栈中拿取出来
     *
     * @return cell
     */
    private Cell popCell() {
        if (mWriteList == null
                || mWriteList.isEmpty()
                || mWriteList.size() <= 0) return null;
        return mWriteList.get(0);
    }

    /**
     * 开始写入堆栈
     *
     * @param value 传输数据
     */
    public void startWrite(byte[] value) {
        pushCell(value);
    }

    /**
     * 停止传输,中断线程
     */
    public void stopWrite() {
        setStoped(true);
        this.interrupt();
    }

    /**
     * 断开连接，清楚所有数据
     */
    public void clearAll() {
        mWriteList.clear();
        TIME_OUT = 3;
        FAIL_COUNT = 3;
        isSendNextOrder = false;
    }

    /**
     * 写数据
     *
     * @param buffer 指令
     * @return boolean
     */
    private boolean writeCharcteristic(byte[] buffer) {
        //控制uuid
        UUID uuid = UUID.fromString(BluetoothAttributes.UUID_CONTROL);
        if (mGattHashMap == null || !mGattHashMap.containsKey(uuid)) return false;
        //根据uuid从缓存中拿取蓝牙服务特征
        BluetoothGattCharacteristic mCharacteristic = mGattHashMap.get(uuid);
        if (mCharacteristic == null) return false;
        int charaProp = mCharacteristic.getProperties();
        return writeCharacteristic(buffer, charaProp, mCharacteristic);
    }

    /**
     * 向设备写数据,根据Properties性质使用不同的写入方式
     *
     * @param buffer         写入的数据
     * @param charaProp      BluetoothGattCharacteristic属性
     * @param characteristic BluetoothGattCharacteristic对象
     * @return boolean
     */
    private boolean writeCharacteristic(byte[] buffer, int charaProp, BluetoothGattCharacteristic characteristic) {
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {// PROPERTY_WRITE 默认类型，需要外围设备的确认,才能继续发送写
            // 可写，二进制1000
            characteristic.setValue(buffer);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            return mBluetoothLeService.writeCharecteristic(characteristic);
        } else if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {// PROPERTY_WRITE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率
            // 只可写，二进制0100
            characteristic.setValue(buffer);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            return mBluetoothLeService.writeCharecteristic(characteristic);
        }
        return false;
    }
}
