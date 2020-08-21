package com.dy.autocomment;

import androidx.appcompat.app.AppCompatActivity;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.dy.fastframework.activity.BaseActivity;
import com.dy.fastframework.view.CommonMsgDialog;

import java.util.List;

import yin.deng.normalutils.utils.MyUtils;
import yin.deng.normalutils.utils.NoDoubleClickListener;
import yin.deng.normalutils.view.MsgDialog;

public class MainActivity extends BaseActivity {

    private Button btStart;
    private EditText etBeTweenTime;
    private Switch switchIsOpen;
    private TextView tvResults;
    private int lastCount;
    private boolean isFirstOpen=true;
    private Switch tvIsYh;
    private EditText etYhBeTime;
    private Switch switchIsOpenComment;
    private EditText etLikePoint;

    @Override
    public int setLayout() {
        return R.layout.activity_main;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isAccessibilitySettingsOn(this,
                AccessibilityTestService.class.getName())) {// 判断服务是否开启
            final CommonMsgDialog msgDialog=new CommonMsgDialog(this);
            msgDialog.setCancelable(false);
            msgDialog.getHolder().tvTitle.setText("系统提示");
            msgDialog.getHolder().tvSure.setText("去开启");
            msgDialog.getHolder().tvCancle.setVisibility(View.GONE);
            msgDialog.getHolder().tvMiddle.setVisibility(View.GONE);
            msgDialog.getHolder().tvContent.setText("使用此功能需要开启无障碍服务，请点击去开启手动打开！");
            msgDialog.getHolder().llProgress.setVisibility(View.GONE);
            msgDialog.getHolder().tvSure.setOnClickListener(new NoDoubleClickListener() {
                @Override
                protected void onNoDoubleClick(View v) {
                    msgDialog.dismiss();
                    jumpToSettingPage(MainActivity.this);// 跳转到开启页面
                }
            });
            msgDialog.show();
        } else {
            showTs( "服务已开启");
            //do other things...
        }
        int nowCount=BaseApp.getSharedPreferenceUtil().getInt("count");
        if(isFirstOpen) {
            lastCount = BaseApp.getSharedPreferenceUtil().getInt("count");
            isFirstOpen=false;
            nowCount=0;
        }
        AccessibilityTestService.isYhOpen=tvIsYh.isChecked();
        AccessibilityTestService.isOpenComment=switchIsOpenComment.isChecked();
        tvResults.setText("上次发送："+lastCount+"条  本次发送："+nowCount+"条");
    }


    //判断自定义辅助功能服务是否开启
    public static boolean isAccessibilitySettingsOn(Context context, String className) {
        if (context == null) {
            return false;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            List<ActivityManager.RunningServiceInfo> runningServices =
                    activityManager.getRunningServices(100);// 获取正在运行的服务列表
            if (runningServices.size() < 0) {
                return false;
            }
            for (int i = 0; i < runningServices.size(); i++) {
                ComponentName service = runningServices.get(i).service;
                if (service.getClassName().equals(className)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }


    //跳转到设置页面无障碍服务开启自定义辅助功能服务
    public static void jumpToSettingPage(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void bindViewWithId() {
        btStart=findViewById(R.id.bt_start);
        etBeTweenTime=findViewById(R.id.et_between_time);
        switchIsOpen=findViewById(R.id.s_is_open);
        switchIsOpenComment=findViewById(R.id.s_is_comment);
        tvResults =findViewById(R.id.tv_results);
        tvIsYh =findViewById(R.id.s_is_yh);
        etYhBeTime =findViewById(R.id.et_yh_between_time);
        etLikePoint =findViewById(R.id.et_like_point);
    }

    @Override
    public void initFirst() {
        AccessibilityTestService.isSwitchOpen=switchIsOpen.isChecked();
        AccessibilityTestService.isOpenComment=switchIsOpenComment.isChecked();
        switchIsOpen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AccessibilityTestService.isSwitchOpen=switchIsOpen.isChecked();
            }
        });
        switchIsOpenComment.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AccessibilityTestService.isOpenComment=switchIsOpenComment.isChecked();
            }
        });
        tvIsYh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AccessibilityTestService.isYhOpen=tvIsYh.isChecked();
            }
        });
        btStart.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                boolean isEmpty=MyUtils.isEmpty(etBeTweenTime);
                boolean isYhBeTimeEmpty=MyUtils.isEmpty(etYhBeTime);
                if(isEmpty){
                    AccessibilityTestService.clickNeedWaitTime=10*1000;
                }else{
                    AccessibilityTestService.clickNeedWaitTime=Integer.parseInt(etBeTweenTime.getText().toString())*1000;
                }
                if(!isYhBeTimeEmpty){
                    AccessibilityTestService.yhWaitTimeEveryVideo=Integer.parseInt(etYhBeTime.getText().toString().trim());
                }
                AccessibilityTestService.refreshNowData();
                if(!MyUtils.isEmpty(etLikePoint)) {
                    AccessibilityTestService.likePoint = Integer.parseInt(etLikePoint.getText().toString().trim());
                }
                launchDouYin();
            }
        });
    }


    /**
     * 启动第三方apk
     * 直接打开  每次都会启动到启动界面，每次都会干掉之前的，从新启动
     * XXXXX ： 包名
     */
    public  void launchDouYin() {
        PackageManager packageManager = getPackageManager();
        Intent it = packageManager.getLaunchIntentForPackage("com.ss.android.ugc.aweme");
        try {
            startActivity(it);
        }catch (Exception e){
            e.printStackTrace();
            showTs("启动失败，请确认您安装了抖音");
        }
    }
}
