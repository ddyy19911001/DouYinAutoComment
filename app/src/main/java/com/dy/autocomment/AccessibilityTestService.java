package com.dy.autocomment;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Service;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.vise.utils.assist.RandomUtil;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import yin.deng.normalutils.utils.DownTimer;
import yin.deng.normalutils.utils.LogUtils;
import yin.deng.normalutils.utils.SharedPreferenceUtil;
import yin.deng.normalutils.utils.VibratorUtil;

public class AccessibilityTestService extends AccessibilityService {
    AccessibilityEvent mAccessibilityEvent;
    AccessibilityEvent mAccessibilityEventOfEditShow;
    long lastSendTime=0;
    long lastInputTime=0;
    long lastClickEditextTime=0;
    private boolean isLive=true;
    public static boolean isSwitchOpen=true;
    public static boolean isYhOpen=false;//是否同时开启养号模式
    public static long clickNeedWaitTime=10000;//每隔多久点弹起来一次输入框
    public static long openSoftDelayTime=5000;//每隔多久点弹起来一次输入框
    public static int yhWaitTimeEveryVideo=15;//每隔多久点弹起来一次输入框
    public static int likePoint=40;//每隔多久点弹起来一次输入框
    private Thread mThread;
    private boolean isDoing=false;
    private List<AccessibilityNodeInfo> etParent;
    public int nowSendCount=0;
    private static boolean isYhDoing=false;
    private boolean isCommentDoing=false;
    private static boolean isCommentThisOne=true;
    private static boolean isTimeOver=true;
    private static boolean isOpenning=false;
    public static boolean isOpenComment=false;

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        mAccessibilityEvent=event;
        if(!isSwitchOpen){
            return;
        }
        String packageName = event.getPackageName().toString();
        int eventType = event.getEventType();
        Log.w("accessibility", "packageName = " + packageName + " eventType = " + eventType);
        if("com.ss.android.ugc.aweme".equals(packageName)){
            dealWithLivingCommment(event);
            List<AccessibilityNodeInfo> isMainAc = findNodesById("com.ss.android.ugc.aweme:id/fx9");
            if(isMainAc!=null&&isMainAc.size()>0&&isMainAc.get(0).isEnabled()) {
                if (isYhOpen) {
                    if(!isTimeOver||isYhDoing){
                        LogUtils.v("时间未到");
                        return;
                    }
                    isYhDoing = true;
                    LogUtils.v("养号操作");
                    dealWithYh(event);
                    if(isYhDoing&&!isCommentThisOne) {
                        dealWithYhComment(event);
                    }
                }
            }
        }
    }


    /**
     * 处理养号时的评论操作
     * @param event
     */
    private void dealWithYhComment(final AccessibilityEvent event) {
        if(!isCommentDoing) {
            //此视频还没有进行评论
            //看评论框是否开启
            isCommentDoing=true;
            final List<AccessibilityNodeInfo> etInputBt = findNodesById("com.ss.android.ugc.aweme:id/a7r");
            if (etInputBt != null && etInputBt.size() > 0 && etInputBt.get(0).isEnabled()) {
                etInputBt.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                final String getYhCommentText = getCommentText();
                setNodeText(etInputBt.get(0), getYhCommentText);
                final List<AccessibilityNodeInfo> sendCommentBt = findNodesById("com.ss.android.ugc.aweme:id/a87");
                if (sendCommentBt != null && sendCommentBt.size() > 0 &&sendCommentBt.get(0).isEnabled()&&sendCommentBt.get(0).isClickable()) {
                    sendCommentBt.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    LogUtils.v("评论成功了");
                    List<AccessibilityNodeInfo> closeBt = findNodesById("com.ss.android.ugc.aweme:id/a7r");
                    if (closeBt != null && closeBt.size() > 0 && closeBt.get(0).isEnabled()) {
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    }else{
                        LogUtils.v("没有找到关闭按钮");
                    }
                    //评论框是弹起状态
                    isCommentThisOne=true;
                    isCommentDoing=false;
                    if(isTimeOver) {
                        openNextOne();
                    }
                }else{
                    isCommentDoing=false;
                    LogUtils.v("没有找到发送按钮");
                }
            }else{
                isCommentDoing=false;
                if(isTimeOver) {
                    openNextOne();
                }
            }
        }else{
            isCommentDoing=false;
            List<AccessibilityNodeInfo> closeBt = findNodesById("com.ss.android.ugc.aweme:id/a7r");
            List<AccessibilityNodeInfo> sendCommentBt = findNodesById("com.ss.android.ugc.aweme:id/a87");
            if (closeBt != null && closeBt.size() > 0 && closeBt.get(0).isEnabled()&&(sendCommentBt==null||sendCommentBt.size()==0||!sendCommentBt.get(0).isEnabled())) {
                if(isCommentThisOne) {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                }
                if(isTimeOver) {
                    openNextOne();
                }
            }else{
                LogUtils.v("没有找到关闭按钮");
                if(isTimeOver) {
                    openNextOne();
                }
            }
        }
    }

    /**
     * 获取评论词
     * @return
     */
    private String getCommentText() {
        String [] datas=getResources().getStringArray(R.array.comment_normal);
        String nowComment=datas[RandomUtil.getRandom(datas.length)];
        return nowComment;
    }


    /**
     * 处理自动养号相关逻辑
     * @param event
     */
    private void dealWithYh(final AccessibilityEvent event) {
        LogUtils.w("开始计时");
        isTimeOver=false;
        isCommentThisOne=false;
        new Thread(new Runnable(){
            @Override
            public void run() {
               int time=yhWaitTimeEveryVideo;
               while (time>0){
                   if(mAccessibilityEvent!=null&&!"com.ss.android.ugc.aweme".equals(mAccessibilityEvent.getPackageName().toString())){
                       isTimeOver=true;
                       isCommentThisOne=false;
                       isOpenning=false;
                       isYhDoing = false;
                       return;
                   }
                   try {
                       Thread.sleep(1000);
                       time--;
                       LogUtils.w("当前时间："+time);
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   }
               }
                if(mAccessibilityEvent!=null&&!"com.ss.android.ugc.aweme".equals(event.getPackageName().toString())){
                    isTimeOver=true;
                    isCommentThisOne=false;
                    isOpenning=false;
                    isYhDoing = false;
                    return;
                }
                int x= RandomUtil.getRandom(100);
                if(x<likePoint) {
                    clickLike();
                }
                int isNeedcomment=RandomUtil.getRandom(100);
                if(isOpenComment&&isNeedcomment<60){
                    comment(event);
                }else {
                    openNextOne();
                }
                isTimeOver=true;
            }
        }).start();
    }

    public static void refreshNowData(){
        isTimeOver=true;
        isCommentThisOne=true;
        isOpenning=false;
        isYhDoing = false;
    }

    private void comment(AccessibilityEvent event) {
        List<AccessibilityNodeInfo> commentBt = findNodesById("com.ss.android.ugc.aweme:id/a8j");
        if(commentBt!=null&&commentBt.size()>0&&commentBt.get(0).isEnabled()&&commentBt.get(0).isClickable()){
            commentBt.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            LogUtils.v("打开评论框");
        }else{
            openNextOne();
        }
    }


    /**
     * 看下一个
     */
    private void openNextOne() {
        if (!isOpenning&&android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            isOpenning=true;
            int middleXValue = 360;
            final int topSideOfScreen = 100;
            final int bottomSizeOfScreen = 1200;
            Path path = new Path();
            path.moveTo(middleXValue,bottomSizeOfScreen);
            path.lineTo(middleXValue,topSideOfScreen);
            LogUtils.v("开始滑动到下一个视频");
            dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                    (path, 20, 800)).build(), new AccessibilityService.GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    LogUtils.v("滑动到下一个视频----已完成");
                    isYhDoing = false;
                    isOpenning=false;
                    isTimeOver=true;
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    LogUtils.e("滑动到下一个视频----失败了");
                    isOpenning=false;
                }
            }, null);
        }
    }


    /**
     * 点个小红心
     */
    public void clickLike(){
        List<AccessibilityNodeInfo> likeView = findNodesById("com.ss.android.ugc.aweme:id/alx");
        if(likeView!=null&&likeView.size()>0&&likeView.get(0).isEnabled()&&likeView.get(0).isClickable()){
            likeView.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            LogUtils.v("给一个小心心");
        }
    }


    /***
     * 处理直播评论相关逻辑
     * @param event
     */
    private void dealWithLivingCommment(AccessibilityEvent event) {
        if(isDoing){
            return;
        }
        List<AccessibilityNodeInfo> nodeInfos=findNodesById("com.ss.android.ugc.aweme:id/bf_");
        etParent=findNodesById("com.ss.android.ugc.aweme:id/aty");
        if(nodeInfos!=null&&nodeInfos.size()>0||(etParent!=null&&etParent.size()>0)){
            dealWithThread();
            isDoing=true;
            LogUtils.w("qqqqqq进入直播页面");
            if(etParent!=null&&etParent.size()>0&&etParent.get(0).getChildCount()>0&&etParent.get(0).getChild(0).isEnabled()){
                LogUtils.w("qqqqqq找到了输入框，输入框已经弹起");
                mAccessibilityEventOfEditShow=event;
                inputTextAndSend(etParent,false);
            }else{
                LogUtils.w("qqqqqq没找到输入框");
                List<AccessibilityNodeInfo> nodeNeedClick = findNodesById("com.ss.android.ugc.aweme:id/aso");
                if(nodeNeedClick!=null) {
                    if(System.currentTimeMillis()-lastClickEditextTime>=openSoftDelayTime) {
//                            forceClick(nodeNeedClick.get(0));
                        nodeNeedClick.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        LogUtils.w("qqqqqq点击输入框");
                        lastClickEditextTime=System.currentTimeMillis();
                    }
                    isDoing=false;
                }else{
                    LogUtils.w("qqqqqq无法弹起输入框");
                    isDoing=false;
                }
            }
        }else{
            isDoing=false;
        }
    }


    /**
     * 处理输入框弹起之后的文字和发送
     */
    private void dealWithThread() {
        if(mThread==null){
            mThread=new Thread(new Runnable(){
                @Override
                public void run() {
                    while (isLive){
                        try {
                            Thread.sleep(clickNeedWaitTime);
                            if(!isSwitchOpen){
                                continue;
                            }
                            if(mAccessibilityEventOfEditShow!=null){
                                closeEdit();
                            }else{
                                LogUtils.e("qqqqqqmAccessibilityEventOfEditShow为空");
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            LogUtils.e("qqqqqq线程出现异常");
                            Vibrator vib = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
                            vib.vibrate(2000);
                        }
                    }
                    Vibrator vib = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
                    vib.vibrate(2000);
                }
            });
            mThread.start();
        }
    }

    private void inputTextAndSend(List<AccessibilityNodeInfo> etParent,boolean isInThread) {
        final String nowText=getNowNeedInputText();
        if(System.currentTimeMillis()-lastInputTime>clickNeedWaitTime/3) {
            setNodeText(etParent.get(0).getChild(0),nowText);
            lastInputTime=System.currentTimeMillis();
        }
        final List<AccessibilityNodeInfo> btNode = findNodesById("com.ss.android.ugc.aweme:id/eiu");
        if(btNode!=null&&btNode.get(0).isEnabled()){
            LogUtils.w("qqqqqq找到了发送按钮");
            if(System.currentTimeMillis()-lastSendTime>clickNeedWaitTime/2) {
                if(!isInThread) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            btNode.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            LogUtils.v("发送了信息：" + nowText);
                            lastSendTime = System.currentTimeMillis();
                            nowSendCount++;
                            BaseApp.getSharedPreferenceUtil().saveInt("count", nowSendCount);
                            mAccessibilityEventOfEditShow = null;
                            isDoing = false;
                        }
                    }, 1000);
                }else{
                    try {
                        Thread.sleep(1000);
                        btNode.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        LogUtils.v("发送了信息："+nowText);
                        nowSendCount++;
                        BaseApp.getSharedPreferenceUtil().saveInt("count", nowSendCount);
                        lastSendTime=System.currentTimeMillis();
                        mAccessibilityEventOfEditShow=null;
                        isDoing=false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                LogUtils.e("当前已发送："+nowSendCount+"条");
            }else{
                LogUtils.w("qqqqqq时间未冷却无法发送");
                isDoing=false;
            }
        }else{
            LogUtils.w("qqqqqq没找到发送按钮");
            isDoing=false;
        }
    }


    private void closeEdit() {
        if(etParent!=null){
            inputTextAndSend(etParent,true);
            LogUtils.e("qqqqqq继续输入文字并发送");
        }else{
            LogUtils.i("qqqqqq没有找到可点击的控件");
        }
    }


    /**
     * 返回当前输入框需要输入的内容
     * @return
     */
    private String getNowNeedInputText() {
        String []datas=new String[]{"6666","你好","互保胡暖","我是最活跃的","球管住"};
        datas=getResources().getStringArray(R.array.comments);
        int x=RandomUtil.getRandom(datas.length);
        return datas[x];
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        LogUtils.d("qqqqqq无障碍服务已开启");
    }

    public void setNodeText(AccessibilityNodeInfo node, String text)
    {
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }

    @Override
    public void onInterrupt() {

    }



    private AccessibilityNodeInfo getRootNodeInfo() {
        AccessibilityEvent curEvent = mAccessibilityEvent;
        AccessibilityNodeInfo nodeInfo = null;
        if (Build.VERSION.SDK_INT >= 16) {
                nodeInfo = getRootInActiveWindow();
        } else {
            nodeInfo = curEvent.getSource();
        }
        return nodeInfo;
    }

    public List<AccessibilityNodeInfo> findNodesByText(String text) {
        AccessibilityNodeInfo nodeInfo = getRootNodeInfo();
        if (nodeInfo != null) {
            Log.i("accessibility", "getClassName：" + nodeInfo.getClassName());
            Log.i("accessibility", "getText：" + nodeInfo.getText());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                //需要在xml文件中声明权限android:accessibilityFlags="flagReportViewIds"
                // 并且版本大于4.3 才能获取到view 的 ID
                Log.i("accessibility", "getClassName：" + nodeInfo.getViewIdResourceName());
            }
            return nodeInfo.findAccessibilityNodeInfosByText(text);
        }
        return null;
    }


    private AccessibilityNodeInfo getChildNodeInfos(String id, int childIndex) {
        List<AccessibilityNodeInfo> listChatRecord = findNodesById(id);
        if (listChatRecord == null || listChatRecord.size() == 0) {
            return null;
        }
        AccessibilityNodeInfo parentNode = listChatRecord.get(0);//该节点
        int count = parentNode.getChildCount();
        Log.i("accessibility", "子节点个数 " + count);
        return childIndex < count ? parentNode.getChild(childIndex) : null;
    }




    public List<AccessibilityNodeInfo> findNodesById(String viewId) {
        AccessibilityNodeInfo nodeInfo = getRootNodeInfo();
        if (nodeInfo != null) {
            if (Build.VERSION.SDK_INT >= 18) {
                return nodeInfo.findAccessibilityNodeInfosByViewId(viewId);
            }
        }
        return null;
    }
}