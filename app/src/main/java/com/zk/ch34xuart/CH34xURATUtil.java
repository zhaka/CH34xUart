package com.zk.ch34xuart;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

/**
* CH34xURAT 工具类
*
* @auther zhaokai
* @date 2021/12/30 16:50
**/
public class CH34xURATUtil {

    private final static String TAG=CH34xURATUtil.class.getSimpleName();

    private static CH34xUARTDriver driver;

    private boolean isOpen=false;

    //配置参数
    private int baudRate=9600;//波特率  115200
    private byte stopBit=1;//停止位
    private byte dataBit=8;//数据位
    private byte parity=0;//0：none，1：add，2：even，3：mark 和 4：space，默认：none
    private byte flowControl=0;//0：none，1：cts/rts，默认：none

    //指令  两个命令之间的间隔必须大于250ms
    public static byte[] startDevice={(byte) 0xB6,(byte) 0x01,(byte) 0x01,(byte) 0x00};//开机
    public static byte[] stopDevice={(byte) 0xB6,(byte) 0x01,(byte) 0x02,(byte) 0x00};//关机
    public static byte[] setBrightness={(byte) 0xB6,(byte) 0x02,(byte) 10,(byte) 0x00};//设置亮度  亮度0-15 第三位
    public static byte[] upBrightness={(byte) 0xB6,(byte) 0x02,(byte) 0x11,(byte) 0x00};//增加亮度
    public static byte[] downBrightness={(byte) 0xB6,(byte) 0x02,(byte) 0x12,(byte) 0x00};//减小亮度
    public static byte[] mainMachine={(byte) 0xB6,(byte) 0x03,(byte) 0x01,(byte) 0x00};//设置设备为主机
    public static byte[] followMachine={(byte) 0xB6,(byte) 0x03,(byte) 0x02,(byte) 0x00};//设置设备为从机
    public static byte[] apMode={(byte) 0xB6,(byte) 0x04,(byte) 0x01,(byte) 0x00};//设为AP模式
    public static byte[] playNext={(byte) 0xB6,(byte) 0x05,(byte) 0x01,(byte) 0x00};//播放下一个
    public static byte[] playPrevious={(byte) 0xB6,(byte) 0x05,(byte) 0x02,(byte) 0x00};//播放上一个
    public static byte[] pause={(byte) 0xB6,(byte) 0x05,(byte) 0x03,(byte) 0x00};//暂停播放
    public static byte[] play={(byte) 0xB6,(byte) 0x05,(byte) 0x04,(byte) 0x00};//继续播放
    public static byte[] rePlay={(byte) 0xB6,(byte) 0x05,(byte) 0x05,(byte) 0x00};//从头播放
    public static byte[] switchAndPause={(byte) 0xB6,(byte) 0x05,(byte) 0x06,(byte) 1};//切换到第N（最后一位）个视频并暂停 ,播放完之后停在最后一帧
    public static byte[] pauseAndPlay={(byte) 0xB6,(byte) 0x05,(byte) 0x07,(byte) 0x00};//暂停/继续播放

//    0x01	0x01       0x00	开机
//    0x01	0x02       0x00	关机
//    0x02	0x00-0x0f   0x00	亮度0-15
//    0x02	0x11       0x00	增加亮度
//    0x02	0x12       0x00	减小亮度
//    0x03	0x01       0x00	设置设备为主机
//    0x03	0x02       0x00	设置设备为主机
//    0x04	0x01       0x00	设为AP模式
//    0x05	0x01       0x00	播放下一个
//    0x05	0x02       0x00	播放上一个
//    0x05	0x03       0x00	暂停播放
//    0x05	0x04       0x00	继续播放
//    0x05	0x05       0x00	从头播放
//    0x05	0x06       N	切换到第N个视频并暂停 ,播放完之后停在最后一帧
//    0x05	0x07       0x00	暂停/继续播放
//    0x20	N[15:8]    N[7:0]	N:要播放的文件名(0.MP4-65535.MP4)

    public static CH34xURATUtil getInstance() {
        return SingletonHolder.getSingleton();
    }

    private static class SingletonHolder {
        private static final CH34xURATUtil singleton = new CH34xURATUtil();

        private static CH34xURATUtil getSingleton() {
            return SingletonHolder.singleton;
        }
    }

    /**
     * 创建Driver
     */
    public void initCH34x() {
        UsbManager usbManager=(UsbManager)MyApplication.getContext().getSystemService(Context.USB_SERVICE);
        String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";
        driver=new CH34xUARTDriver(usbManager, MyApplication.getContext(), ACTION_USB_PERMISSION);
    }

    /**
     * 是否支持USBHost
     * @return
     */
    public boolean supportUSBHost(){
       return driver.UsbFeatureSupported();
    }

    /**
     * 打开串口
     * @return 失败时，返回失败原因
     */
    public String openPort(){
        int retval = driver.ResumeUsbPermission();//判断是否授权
        if (retval == 0) {
            //Resume usb device list
            retval = driver.ResumeUsbList();// 枚举并打开CH34X设备
            if (retval == -1){//打开失败
                closePort();
                return "ResumeUsbList failed!";
            } else if (retval == 0){//打开成功
                if (driver.mDeviceConnection != null) {
                    if (driver.UartInit()) {//初始化 CH34x 芯片
                        isOpen = true;
                        new ReadThread().start();//开启读线程读取串口接收的数据
                        return "";
                    }else{
                        return "UartInit failed!";
                    }
                } else {
                    return "DeviceConnection Open failed!";
                }
            } else {
                return "ResumeUsbList Open failed! 未授权限";
            }
        }else{
            return "USB未授权 retval="+retval;
        }

    }

    /**
     * 关闭串口
     */
    public void closePort(){
        isOpen = false;
        driver.CloseDevice();
    }

    /**
     * 设置 UART 接口的波特率、数据位、停止位、奇偶校验位以及流控
     */
    public boolean initConfig(){
        return driver.SetConfig(baudRate, dataBit, stopBit, parity,flowControl);
    }

    /**
     * 是否连接
     */
    public boolean isConnected(){
        return driver.isConnected();
    }

    /**
     * 发送命令
     */
    public String sendCommand(byte[] to_send){
        if(!isOpen){
            //打开串口
            String errorMsg=CH34xURATUtil.getInstance().openPort();
            if(!TextUtils.isEmpty(errorMsg)){
                return "打开串口失败："+errorMsg;
            }

            //配置串口波特率等
            boolean setConfigSuccess=CH34xURATUtil.getInstance().initConfig();
            if(!setConfigSuccess){
                return "初始化串口配置失败："+errorMsg;
            }
        }
        //发送
        int retval = driver.WriteData(to_send, to_send.length);//写数据，第一个参数为需要发送的字节数组，第二个参数为需要发送的字节长度，返回实际发送的字节长度
        if(retval>=0){//大于等0，发送成功
            return "";
        }else{
            return "retval="+retval;
        }
    }

    private class ReadThread extends Thread {

        public void run() {
            byte[] buffer = new byte[4096];
            while (true) {
                if (!isOpen) {
                    break;
                }
                int length = driver.ReadData(buffer, 4096);
                if (length > 0) {
                    String recv = toHexString(buffer, length);		//以16进制输出
//					String recv = new String(buffer, 0, length);		//以字符串形式输出
                    Log.d(TAG,"收到数据："+recv);
                }
            }
        }
    }

    /**
     * 将String转化为byte[]数组
     * @param arg 需要转换的String对象
     * @return 转换后的byte[]数组
     */
    private byte[] toByteArray(String arg) {
        if (arg != null) {
            /* 1.先去除String中的' '，然后将String转换为char数组 */
            char[] NewArray = new char[1000];
            char[] array = arg.toCharArray();
            int length = 0;
            for (int i = 0; i < array.length; i++) {
                if (array[i] != ' ') {
                    NewArray[length] = array[i];
                    length++;
                }
            }
            /* 将char数组中的值转成一个实际的十进制数组 */
            int EvenLength = (length % 2 == 0) ? length : length + 1;
            if (EvenLength != 0) {
                int[] data = new int[EvenLength];
                data[EvenLength - 1] = 0;
                for (int i = 0; i < length; i++) {
                    if (NewArray[i] >= '0' && NewArray[i] <= '9') {
                        data[i] = NewArray[i] - '0';
                    } else if (NewArray[i] >= 'a' && NewArray[i] <= 'f') {
                        data[i] = NewArray[i] - 'a' + 10;
                    } else if (NewArray[i] >= 'A' && NewArray[i] <= 'F') {
                        data[i] = NewArray[i] - 'A' + 10;
                    }
                }
                /* 将 每个char的值每两个组成一个16进制数据 */
                byte[] byteArray = new byte[EvenLength / 2];
                for (int i = 0; i < EvenLength / 2; i++) {
                    byteArray[i] = (byte) (data[i * 2] * 16 + data[i * 2 + 1]);
                }
                return byteArray;
            }
        }
        return new byte[] {};
    }

    /**
     * 将String转化为byte[]数组
     * @param arg
     *            需要转换的String对象
     * @return 转换后的byte[]数组
     */
    private byte[] toByteArray2(String arg) {
        if (arg != null) {
            /* 1.先去除String中的' '，然后将String转换为char数组 */
            char[] NewArray = new char[1000];
            char[] array = arg.toCharArray();
            int length = 0;
            for (int i = 0; i < array.length; i++) {
                if (array[i] != ' ') {
                    NewArray[length] = array[i];
                    length++;
                }
            }

            byte[] byteArray = new byte[length];
            for (int i = 0; i < length; i++) {
                byteArray[i] = (byte)NewArray[i];
            }
            return byteArray;

        }
        return new byte[] {};
    }

    /**
     * 将byte[]数组转化为String类型
     * @param arg
     *            需要转换的byte[]数组
     * @param length
     *            需要转换的数组长度
     * @return 转换后的String队形
     */
    private String toHexString(byte[] arg, int length) {
        String result = new String();
        if (arg != null) {
            for (int i = 0; i < length; i++) {
                result = result
                        + (Integer.toHexString(
                        arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
                        + Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])
                        : Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])) + " ";
            }
            return result;
        }
        return "";
    }

    /**
     * 将byte[]数组转化为String类型
     * @param arg
     *            需要转换的byte[]数组
     * @param length
     *            需要转换的数组长度
     * @return 转换后的String队形
     */
    private String toHexString2(byte[] arg, int length) {
        String result = new String();
        if (arg != null) {
            for (int i = 0; i < length; i++) {
                if (i==length-1){
                    result = result
                            + (Integer.toHexString(
                            arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
                            + Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                            : arg[i])
                            : Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                            : arg[i])) + "";
                }else {
                    result = result
                            + (Integer.toHexString(
                            arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
                            + Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                            : arg[i])
                            : Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                            : arg[i])) + " ";
                }
            }
            return result;
        }
        return "";
    }

}
