package com.example.TestTaxi.Hy_Bluetooth.decoder;

/**
 * 解码接口
 */
public interface IDecoder {
    /**
     * 解码
     *
     * @param code 返回的数据
     */
    boolean decode(String code);
}
