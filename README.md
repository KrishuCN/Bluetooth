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

/*
    校验蓝牙权限
   */
   private void checkBluetoothPermission() {
       if (Build.VERSION.SDK_INT >= 23) {
           //校验是否已具有模糊定位权限
           if (ContextCompat.checkSelfPermission(MainActivity.this,
                   Manifest.permission.ACCESS_COARSE_LOCATION)
                   != PackageManager.PERMISSION_GRANTED) {
               ActivityCompat.requestPermissions(MainActivity.this,
                       new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                       REQUEST_ENABLE_BT );
           }else{<br>　　　　　　　　　　//权限已打开
               startScan();
           }
       }else{　　　　　　　//小于23版本直接使用
           startScan();
       }
   }
   
　3、接收请求权限的返回：
@Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
      if (requestCode == REQUEST_ENABLE_BT){
          if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {<br>　　　　　　　　　　//蓝牙权限开启成功
              startScan();
          }else{
              Toast.makeText(MainActivity.this, "蓝牙权限未开启,请设置", Toast.LENGTH_SHORT).show();
          }
      }
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }
 

 

　　检查蓝牙是否开启：
public boolean checkBlueEnable(){
        if (mBluetoothAdapter.isEnabled()){
           return  true;
        }else {
            Toast.makeText(this,"蓝牙未打开",Toast.LENGTH_SHORT).show();
            return  false;
        }
    }
