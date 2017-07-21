package com.example.TestTaxi.Hy_Bluetooth.decoder;

import android.content.Context;

/**
 * 解码父类
 */
public class Decoder implements IDecoder {

    public Decoder(Context context) {
    }

    @Override
    public boolean decode(String code) {
        return true;
    }
}
