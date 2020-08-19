package com.dy.autocomment;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

import com.vise.utils.assist.RandomUtil;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import yin.deng.normalutils.utils.LogUtils;

public class AccessibilityTestService extends AccessibilityService {
    AccessibilityEvent mAccessibilityEvent;
    AccessibilityEvent mAccessibilityEventOfEditShow;
    long lastSendTime=0;
    long lastInputTime=0;
    long lastClickEditextTime=0;
    private boolean isLive=true;
    public static long clickNeedWaitTime=10000;//每隔多久点弹起来一次输入框
    private Thread mThread;
    private boolean isDoing=false;



    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        mAccessibilityEvent=event;
        String packageName = event.getPackageName().toString();
        int eventType = event.getEventType();
        Log.w("accessibility", "packageName = " + packageName + " eventType = " + eventType);
        if(isDoing){
            return;
        }
        if("com.ss.android.ugc.aweme".equals(packageName)){
            if(mThread==null){
                mThread=new Thread(new Runnable(){
                    @Override
                    public void run() {
                        while (isLive){
                            try {
                                Thread.sleep(clickNeedWaitTime*3);
                                if(mAccessibilityEventOfEditShow!=null){
                                    closeEdit();
                                }else{
                                    LogUtils.e("qqqqqqmAccessibilityEventOfEditShow为空");
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                LogUtils.e("qqqqqq线程出现异常");
                            }
                        }
                    }
                });
                mThread.start();
            }
            List<AccessibilityNodeInfo> nodeInfos=findNodesById("com.ss.android.ugc.aweme:id/bt0");
            List<AccessibilityNodeInfo> etParent=findNodesById("com.ss.android.ugc.aweme:id/b4f");
            if(nodeInfos!=null&&nodeInfos.size()>0||(etParent!=null&&etParent.size()>0)){
                isDoing=true;
                LogUtils.w("qqqqqq进入直播页面");
                if(etParent!=null&&etParent.size()>0&&etParent.get(0).getChildCount()>0&&etParent.get(0).getChild(0).isEnabled()){
                    LogUtils.w("qqqqqq找到了输入框，输入框已经弹起");
                    if(System.currentTimeMillis()-lastInputTime>clickNeedWaitTime/2) {
                        setNodeText(etParent.get(0).getChild(0), getNowNeedInputText());
                        lastInputTime=System.currentTimeMillis();
                    }
                    mAccessibilityEventOfEditShow=event;
                    final List<AccessibilityNodeInfo> btNode = findNodesById("com.ss.android.ugc.aweme:id/fhj");
                    if(btNode!=null&&btNode.get(0).isEnabled()){
                        LogUtils.w("qqqqqq找到了发送按钮");
                        if(System.currentTimeMillis()-lastSendTime>clickNeedWaitTime/2) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    btNode.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    lastSendTime=System.currentTimeMillis();
                                    mAccessibilityEventOfEditShow=null;
                                    isDoing=false;
                                }
                            },2000);
                        }else{
                            LogUtils.w("qqqqqq时间未冷却无法发送");
                            isDoing=false;
                        }
                    }else{
                        LogUtils.w("qqqqqq没找到发送按钮");
                        isDoing=false;
                    }
                }else{
                    LogUtils.w("qqqqqq没找到输入框");
                    List<AccessibilityNodeInfo> nodeNeedClick = findNodesById("com.ss.android.ugc.aweme:id/b2t");
                    if(nodeNeedClick!=null) {
                        if(System.currentTimeMillis()-lastClickEditextTime>=clickNeedWaitTime) {
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
    }

    private void closeEdit() {
        final List<AccessibilityNodeInfo> btNode = findNodesById("com.ss.android.ugc.aweme:id/hxu");
        if(btNode!=null&&btNode.size()>0){
            btNode.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            LogUtils.e("qqqqqq点击屏幕关闭输入框");
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