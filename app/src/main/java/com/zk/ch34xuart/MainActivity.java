package com.zk.ch34xuart;

import androidx.appcompat.app.AppCompatActivity;
import cn.wch.ch34xuartdriver.CH34xUARTDriver;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
* CH34x串口：
 * 调用流程 1.openPort() 2.initConfig() 3.sendCommand()
*
* @auther zhaokai
* @date 2021/12/31 10:57
**/
public class MainActivity extends AppCompatActivity {

    ImageView mCloseIV;
    TextView mMessageTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//保持常亮的屏幕的状态
        initView();
    }

    private void initView() {

        mCloseIV=findViewById(R.id.closeIV);
        mMessageTV=findViewById(R.id.messageTV);
        mCloseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //判断系统是否支持USB HOST
        if (!CH34xURATUtil.getInstance().supportUSBHost()){
            showError("初始化失败：不支持USB HOST");
            return;
        }

        //打开串口
        String errorMsg=CH34xURATUtil.getInstance().openPort();
        if(!TextUtils.isEmpty(errorMsg)){
            showError("打开串口失败："+errorMsg);
            return;
        }

        //配置串口波特率等
        boolean setConfigSuccess=CH34xURATUtil.getInstance().initConfig();
        if(!setConfigSuccess){
            showError("初始化串口配置失败！");
        }else{
            showSuccess("初始化串口成功！");
        }

    }

    @Override
    public void onDestroy() {
        CH34xURATUtil.getInstance().closePort();
        super.onDestroy();
    }

    public void btn1(View view) {
        String result=CH34xURATUtil.getInstance().sendCommand(CH34xURATUtil.startDevice);
        showCommandResult("开机",result);
    }
    public void btn2(View view) {
        String result=CH34xURATUtil.getInstance().sendCommand(CH34xURATUtil.stopDevice);
        showCommandResult("关机",result);
    }
    public void btn3(View view) {
        String result=CH34xURATUtil.getInstance().sendCommand(CH34xURATUtil.upBrightness);
        showCommandResult("增加亮度",result);
    }
    public void btn4(View view) {
        String result=CH34xURATUtil.getInstance().sendCommand(CH34xURATUtil.downBrightness);
        showCommandResult("减小亮度",result);
    }
    public void btn5(View view) {
        String result=CH34xURATUtil.getInstance().sendCommand(CH34xURATUtil.playNext);
        showCommandResult("下一个",result);
    }
    public void btn6(View view) {
        String result=CH34xURATUtil.getInstance().sendCommand(CH34xURATUtil.playPrevious);
        showCommandResult("上一个",result);
    }
    public void btn7(View view) {
        String result=CH34xURATUtil.getInstance().sendCommand(CH34xURATUtil.pause);
        showCommandResult("暂停",result);
    }
    public void btn8(View view) {
        String result=CH34xURATUtil.getInstance().sendCommand(CH34xURATUtil.play);
        showCommandResult("继续",result);
    }

    private void showError(String text){
        mMessageTV.setTextColor(Color.parseColor("#ff0000"));
        mMessageTV.setText(text);
    }

    private void showSuccess(String text){
        mMessageTV.setTextColor(Color.parseColor("#00ff00"));
        mMessageTV.setText(text);
    }

    private void showCommandResult(String commandText,String resultText){
        boolean isSuccess=TextUtils.isEmpty(resultText);
        String text="";
        if(isSuccess){
            text=commandText+"指令发送成功";
        }else{
            text=commandText+"指令发送失败："+resultText;
        }
        showMessage(text);
    }

    private void showMessage(String text){
        Toast.makeText(MainActivity.this, text,Toast.LENGTH_LONG).show();
    }


}





