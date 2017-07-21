package com.example.TestTaxi.Hy_Bluetooth.model;

/**
 * 写入蓝牙数据类
 */
public class Cell {

    public Cell(byte[] buffer) {
        this.buffer = buffer;
    }

    private byte[] buffer;

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }
}

