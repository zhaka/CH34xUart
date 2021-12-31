package com.zk.ch34xuart;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {

    public static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext=this;
        CH34xURATUtil.getInstance().initCH34x();
    }



}
