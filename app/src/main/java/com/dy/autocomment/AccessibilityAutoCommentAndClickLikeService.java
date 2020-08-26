package com.dy.autocomment;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.vise.utils.assist.RandomUtil;

import java.io.OutputStream;
import java.util.List;

import yin.deng.normalutils.utils.DownTimer;
import yin.deng.normalutils.utils.LogUtils;


/**
 * 自动点赞评论养号辅助
 * 适配版本 12.4.0 适配机型 小米9
 */
public class AccessibilityAutoCommentAndClickLikeService extends AccessibilityService {
    public static int yhWaitTimeEveryVideo=15;//养号页面停留时间
    public static int lickCount=0;//当前总共点赞数量
    public static int lickPercent=40;//点赞概率
    public static int commentPercent=30;//评论概率
    public static final int WATING=-1;//准备开始
    public static final int NORMAL=0;//准备开始
    public static final int COUNTING=1;//开始计时
    public static final int COUNT_OVER=2;//结束计时
    public static final int LIKEING=3;//点赞开始
    public static final int LIKE_OVER=4;//点赞结束
    public static final int COMMENTING=5;//评论开始
    public static final int COMMENT_OVER=6;//评论结束
    public int nowState=NORMAL;
    public boolean isPauseTimer=true;
    AccessibilityEvent mAccessibilityEvent;
    public static boolean isSwitchOpen=false;//是否开启辅助
    private DownTimer timer;
    public String enterMainAcTag="com.ss.android.ugc.aweme:id/i1s";//视频发布的左下角昵称id
    public String clickEnterInLivingRoom="com.ss.android.ugc.aweme:id/d9k";//直播视频的底部LinearLayoutId
    public String adName="广告";
    private String commentIsOpenStr="com.ss.android.ugc.aweme:id/agl";//评论弹框列表条目id
    private String commentEditTextStr="com.ss.android.ugc.aweme:id/af8";//软键盘弹起后的评论输入框id
    private String sendBtStr="com.ss.android.ugc.aweme:id/afr";//发送按钮的id
    private boolean isSwiping=false;
    public static int commentCount=0;
    private boolean isClickingLike=false;

    /**
     * 重置当前所有标记类数据
     */
    public static void refreshNowData() {

    }

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        mAccessibilityEvent = event;
        if (!isSwitchOpen) {
            LogUtils.v("已关闭辅助");
            return;
        }
        //开始干活
        switch (nowState) {
            case NORMAL:
                LogUtils.d("开始干活了，当前类型："+nowState);
                List<AccessibilityNodeInfo> nodes = findNodesById(enterMainAcTag);
                if (isOk(nodes)) {
                    LogUtils.d("找到音乐播放图标");
                    List<AccessibilityNodeInfo> nodeIsLiving = findNodesById(clickEnterInLivingRoom);
                    List<AccessibilityNodeInfo> nodeIsIsAd = findNodesByText(adName);
                    if(isOk(nodeIsLiving)){
                        showTs("直播间，直接划过");
                        LogUtils.e("直播间，直接划过");
                        if(isSwiping){
                            return;
                        }
                        isSwiping=true;
                        openNextOne();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isSwiping=false;
                                nowState = NORMAL;
                            }
                        }, 1000);
                    }else {
                        if(isOk(nodeIsIsAd)){
                            showTs("广告内容，直接划过");
                            LogUtils.e("广告内容，直接划过");
                            openNextOne();
                            nowState = NORMAL;
                        }else {
                            startTimer();
                        }
                    }
                } else {
                    LogUtils.e("暂未找到音乐播放图标");
                    nowState=NORMAL;
                }
                break;
            case COUNTING:
                List<AccessibilityNodeInfo> nodesWhenCounting = findNodesById(enterMainAcTag);
                if (!isOk(nodesWhenCounting)) {
                    if (!isPauseTimer&&timer != null) {
                        timer.pause();
                        LogUtils.e("计时中，找不到音乐播放图标，暂停计时");
                        isPauseTimer=true;
                        showTs("页面已切换，暂停任务计时");
                    }
                } else {
                    if (timer != null&&isPauseTimer) {
                        timer.resume();
                        isPauseTimer=false;
                        showTs("页面已恢复，继续上次任务");
                        LogUtils.v("计时中，找到音乐播放图标，继续计时");
                    }
                }
                break;
            case COUNT_OVER:
                LogUtils.d("计时结束了，当前类型："+nowState);
                List<AccessibilityNodeInfo> nodesWhenCountOver = findNodesById(enterMainAcTag);
                if (isOk(nodesWhenCountOver)) {
                    setIsClickLike();
                } else {
                    nowState = NORMAL;
                }
                break;
            case LIKEING:
                if(isClickingLike){
                    return;
                }
                LogUtils.d("开始点赞了，当前类型："+nowState);
                List<AccessibilityNodeInfo> nodesWhenLiking = findNodesById(enterMainAcTag);
                if (isOk(nodesWhenLiking)) {
                    isClickingLike=true;
                    clickLike();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            nowState = LIKE_OVER;
                            isClickingLike=false;
                        }
                    }, 1000);
                } else {
                    nowState = NORMAL;
                }
                break;
            case LIKE_OVER:
                LogUtils.d("点赞结束了，当前类型："+nowState);
                List<AccessibilityNodeInfo> nodesWhenCountLikeOver = findNodesById(enterMainAcTag);
                if (isOk(nodesWhenCountLikeOver)) {
                    setIsNeedComment();
                } else {
                    nowState = NORMAL;
                }
                break;
            case COMMENTING:
                commentText();
                break;
            case COMMENT_OVER:
                if(isSwiping){
                    return;
                }
                isSwiping=true;
                LogUtils.d("评论结束了，当前类型："+nowState);
                openNextOne();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nowState = NORMAL;
                        isSwiping=false;
                    }
                }, 1000);
                break;
        }

    }

    long lastDoTime=0;
    boolean isInputOver=false;
    private void commentText() {
        if(System.currentTimeMillis()-lastDoTime<500){
            return;
        }
        if(isInputOver){
            return;
        }
        lastDoTime=System.currentTimeMillis();
        List<AccessibilityNodeInfo> node = findNodesById(commentIsOpenStr);
        List<AccessibilityNodeInfo> nodeEditext = findNodesById(commentEditTextStr);
        if(!isOk(node)) {
            if(!isOk(nodeEditext)){
                LogUtils.d("点击坐标打开评论框");
                //什么都没有点的情况
                forceClick(1000,1605);//打开评论面板
            }else{
                //已经弹起输入框的情况
                LogUtils.d("输入评论内容");
                setNodeText(nodeEditext.get(0), getCommentText());
                List<AccessibilityNodeInfo> sendBt = findNodesById(sendBtStr);
                if(isOk(sendBt)){
                    sendBt.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    commentCount++;
                    BaseApp.getSharedPreferenceUtil().saveInt("commentCount", commentCount);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            forceBack();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    LogUtils.d("评论结束了，当前类型："+nowState);
                                    openNextOne();
                                    LogUtils.d("关闭评论对话框");
                                    nowState = NORMAL;
                                    isInputOver=false;
                                }
                            }, 1500);
                        }
                    }, 1500);
                    LogUtils.d("发送评论");
                    isInputOver=true;
                }else{
                    LogUtils.v("没有找到发送按钮");
                    isInputOver=false;
                }

            }
        }else{
            LogUtils.v("评论面板已经打开");
            //评论面板已经打开
            forceClick(436, 2250);
            //弹起输入框
        }

    }

    private void forceBack() {
        LogUtils.v("点击返回键");
        String cmd="input keyevent 4";
        doCmd(cmd);
    }

    private void setIsNeedComment() {
        int isNeedComment=RandomUtil.getRandom(100);
        LogUtils.e("预设评论概率："+commentPercent+",当前随机数值为："+isNeedComment);
        if(isNeedComment<=commentPercent){
            nowState=COMMENTING;
            showTs("进行评论");
        }else{
            nowState=COMMENT_OVER;
        }
    }

    private void setIsClickLike() {
        int isClickLike=RandomUtil.getRandom(100);
        LogUtils.e("预设点赞概率："+lickPercent+",当前随机数值为："+isClickLike);
        if(isClickLike<=lickPercent){
            nowState=LIKEING;
            showTs("点个赞");
        }else{
            nowState=LIKE_OVER;
        }
    }

    private void showTs(final String msg) {
        Handler handlerThree=new Handler(Looper.getMainLooper());
        handlerThree.post(new Runnable(){
            public void run(){
                Toast.makeText(getApplicationContext() ,msg,Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startTimer() {
        if(timer!=null){
            return;
        }
        showTs("开始新的计时任务");
        nowState=COUNTING;
        timer = new DownTimer();
        timer.setTotalTime(yhWaitTimeEveryVideo*1000);
        timer.setIntervalTime(1000);
        timer.setTimerLiener(new DownTimer.TimeListener() {
            @Override
            public void onFinish() {
                timer=null;
                LogUtils.v("计时结束");
                nowState=COUNT_OVER;
                isPauseTimer=false;
            }

            @Override
            public void onInterval(long remainTime) {
                LogUtils.v("剩余时间："+remainTime/1000+"s");
            }
        });
        timer.start();
        isPauseTimer=false;
    }


    /**
     * 返回该节点是否存在或有效
     * @param nodes
     * @return
     */
    private boolean isOk(List<AccessibilityNodeInfo> nodes) {
        if(nodes!=null&&nodes.size()>0&&nodes.get(0).isEnabled()){
            return true;
        }
        return false;
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
     * 看下一个
     */
    private void openNextOne() {
        String move="input swipe 560 1600 560 500";
        LogUtils.v("滑动下一个视频");
        doCmd(move);
//        if (nowState!=WATING&&Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            nowState=WATING;
//            int middleXValue = 360;
//            final int topSideOfScreen = 100;
//            final int bottomSizeOfScreen = 1200;
//            Path path = new Path();
//            path.moveTo(middleXValue,bottomSizeOfScreen);
//            path.lineTo(middleXValue,topSideOfScreen);
//            LogUtils.v("开始滑动到下一个视频");
//            dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
//                    (path, 20, 800)).build(), new GestureResultCallback() {
//                @Override
//                public void onCompleted(GestureDescription gestureDescription) {
//                    super.onCompleted(gestureDescription);
//                    LogUtils.v("滑动到下一个视频----已完成");
//
//                }
//
//                @Override
//                public void onCancelled(GestureDescription gestureDescription) {
//                    super.onCancelled(gestureDescription);
//                    LogUtils.e("滑动到下一个视频----失败了");
//
//                }
//            }, null);
//            //滑动完成
//        }
    }


    /**
     * 点个小红心
     */
    public void clickLike(){
        forceClick(1020, 1410);
        lickCount++;
        BaseApp.getSharedPreferenceUtil().saveInt("count", lickCount);
    }





    private void forceClick(int x,int y){
        String cmd = "input tap " + String.valueOf(x) + " " + String.valueOf(y);
        LogUtils.v("当前点击的坐标："+x+","+y);
        doCmd(cmd);
    }


    public void doCmd(String cmd){
        try {
            OutputStream os;
            os = Runtime.getRuntime().exec("su").getOutputStream();
            os.write(cmd.getBytes());
            os.flush();//清空缓存
            os.close();//停止流
            LogUtils.e("命令执行完成");
        } catch (Exception e) {
            LogUtils.e(e.getMessage());
            LogUtils.v("命令出错了:"+e.getMessage());
        }
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