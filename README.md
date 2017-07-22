# Bluetooth
BLE设备蓝牙连接模块化代码示例

启动方法：BluetoothLeService.getInstance(Context).startAlarmService();



1、AndroidMainifest.xml权限：

<uses-permission android:name="android.permission.INTERNET"/>
   <uses-permission android:name="android.permission.BLUETOOTH"/>
   <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
   <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
   
          <!--app只支持拥有BLE的设备上运行-->
    <uses-feature
            android:name="android.hardware.bluetooth_le"
            android:required="true" />
   
2、在代码中动态注册：

注意：6.0以上需要添加运行时权限

        /**
     * 蓝牙申请权限
     */
    private void checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            //校验是否已具有模糊定位权限
            if (ContextCompat.checkSelfPermission(TYMposActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(TYMposActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            } else {
                //具有权限
                connectBluetooth();
            }
        } else {
            //系统不高于6.0直接执行
            connectBluetooth();
        }
    }

    /**
     * 申请权限结果
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        doNext(requestCode, grantResults);
    }

    private void doNext(int requestCode, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //同意权限
                connectBluetooth();
            } else {
                // 权限拒绝
                // 下面的方法最好写一个跳转，可以直接跳转到权限设置页面，方便用户
                denyPermission();
            }
        }
    }

