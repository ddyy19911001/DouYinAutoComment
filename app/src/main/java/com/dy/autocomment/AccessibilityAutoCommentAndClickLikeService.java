package com.dy.autocomment;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.vise.utils.assist.RandomUtil;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import yin.deng.normalutils.utils.DownTimer;
import yin.deng.normalutils.utils.LogUtils;


/**
 * 自动点赞评论养号辅助
 * 适配版本 12.4.0 适配机型 小米9
 */
public class AccessibilityAutoCommentAndClickLikeService extends AccessibilityService {
    public static int yhWaitTimeEveryVideo=3;//养号页面停留时间
    public static int lickCount=0;//当前总共点赞数量
    public static int lickPercent=70;//点赞概率
    public static int commentPercent=40;//评论概率
    public static final int WATING=-1;//准备开始
    public static final int NORMAL=0;//准备开始
    public static final int COUNTING=1;//开始计时
    public static final int COUNT_OVER=2;//结束计时
    public static final int LIKEING=3;//点赞开始
    public static final int LIKE_OVER=4;//点赞结束
    public static final int COMMENTING=5;//评论开始
    public static final int COMMENT_OVER=6;//评论结束
    private static final int LIVING_RED_BAG = 7;//直播间发现并点击红包
    private static final int LIVING_CLICK_QIANG = 8;//正在抢红包
    private static final int CHECH_RED_BAG = 9;//正在检测红包是否存在
    public int nowState=NORMAL;
    public boolean isPauseTimer=true;
    AccessibilityEvent mAccessibilityEvent;
    public static boolean isSwitchOpen=false;//是否开启辅助
    public static boolean isAutoClickLike=true;//是否开启直播间自动点赞模式
    public static boolean isSingleLivingRoomComment=true;//是否只在同一直播间进行评论
    private DownTimer timer;
    public String enterMainAcTag="com.ss.android.ugc.aweme:id/dwq";//视频发布的左下角昵称id
    public String clickEnterInLivingRoom="com.ss.android.ugc.aweme:id/dbc";//直播视频的底部LinearLayoutId
    public String adName="广告";
    private String commentIsOpenStr="com.ss.android.ugc.aweme:id/dfz";//评论弹框列表条目id
    private String commentEditTextStr="com.ss.android.ugc.aweme:id/ady";//软键盘弹起后的评论输入框id
    private String sendBtStr="com.ss.android.ugc.aweme:id/aeg";//发送按钮的id
    private String livingRoomTopRightPeopleLinearId="com.ss.android.ugc.aweme:id/e6q";//直播间右上角的观众人数框
    private String livingTopicId="com.ss.android.ugc.aweme:id/bsw";//直播间话题id
    private String editTextParenetId="com.ss.android.ugc.aweme:id/b4f";//输入框父容器id
    private String redBagRootId="com.ss.android.ugc.aweme:id/fw0";//装福袋和红包的父容器
    private boolean isSwiping=false;
    public static int commentCount=0;
    public static boolean isOpenYh=false;//是否开启养号功能
    private boolean isClickingLike=false;
    public static int viewedVideoCount=0;//已观看的视频总数
    private String headOfRedBagSenderId="com.ss.android.ugc.aweme:id/blr";//中间大圆形倒计时的id
    private String qiangId="com.ss.android.ugc.aweme:id/fgu";//抢字id
    private boolean isClickingLeftTop=false;
    private long lastClickLivingLikeTime=0;//上次直播点赞时间
    private long LivingLikeClickBetweenTime=60*1000;//一分钟给主播点赞一次
    private long lastclickBottomLinearTime =0;//上次点击左上角红包按钮的时间
    private long lastClickBack=0;
    private boolean isOpeningCommentBottomLinear=false;
    public static int maxClickLikeSize=50;//最多可以点赞50个视频
    private boolean isKillSelfDoing=false;
    public static long sendLivingCommentDelay=6000;//每隔5秒发一条评论
    private int sendLivingCommentCount=0;//单个直播间发送的总评论数
    public static  boolean needRandWords=true;//是否需要随机字符串
    public static  boolean needAutoClose=true;//是否需要自动关闭所有程序
    private double livingPeopleCount=500;//单直播间发言时，人数最低要求
    private double NotSingleModelivingPeopleCount=200;//轮流直播间发言时直播间最低人数要求

    /**
     * 重置当前所有标记类数据
     */
    public static void refreshNowData() {

    }


    SimpleDateFormat format=new SimpleDateFormat("HH");

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        mAccessibilityEvent = event;
        if (!isSwitchOpen) {
            LogUtils.v("已关闭辅助");
            return;
        }
        if (!isOpenYh) {
            if(isClickZan){
                return;
            }
            if (isSwiping) {
                return;
            }
            List<AccessibilityNodeInfo> node = findNodesById(livingRoomTopRightPeopleLinearId);
            if(isOk(node)&&node.get(0)!=null&&nowState==NORMAL){
                LogUtils.i("进入了直播间");
                if(needAutoClose){
                    String nowHour=format.format(new Date());
                    int nowHourNum=Integer.parseInt(nowHour);
                    if(nowHourNum>=3&&nowHourNum<6) {
                        LogUtils.i("当前时间凌晨3点-6点之间，自动重启手机杀进程");
                        isKillSelfDoing = true;
                        killAll(true);
                        isKillSelfDoing = false;
                        return;
                    }
                }
                CharSequence personCount = node.get(0).getText();
                if(TextUtils.isEmpty(personCount)){
                    return;
                }
                String personCountStr= (String) personCount;
                LogUtils.i("在线用户数："+personCountStr);
                boolean isUsefulLivingRoom=false;//是否是有用的直播间
                boolean isCountOk=false;//人数是否达标
                if(personCountStr.endsWith("万")){
                    isCountOk=true;
                }else{
                    double personCountNum=Double.parseDouble(personCountStr);
                    if(personCountNum>=livingPeopleCount){
                        isCountOk=true;
                    }
                    if(!isSingleLivingRoomComment){
                        if(personCountNum>=NotSingleModelivingPeopleCount){
                            isCountOk=true;
                        }
                    }
                }
                boolean isTopicOk=false;//话题是否达标
                List<AccessibilityNodeInfo> topicNode = findNodesById(livingTopicId);
                if(isOk(topicNode)&&topicNode.get(0)!=null){
                    String topicStr= (String) topicNode.get(0).getText();
                    if(topicStr!=null&&(topicStr.contains("涨")||
                            topicStr.contains("人气")||
                            topicStr.contains("粉"))||
                            topicStr.contains("抱团")||
                            topicStr.contains("互")||
                            topicStr.contains("交友")||
                            topicStr.contains("友")||
                            topicStr.contains("朋友")||
                            topicStr.contains("热门")){
                        isTopicOk=true;
                    }
                }
                isUsefulLivingRoom=isTopicOk&&isCountOk;
                //如果是直播间，执行直播的对应任务
                if(isAutoClickLike&&isUsefulLivingRoom&&System.currentTimeMillis()-lastClickLivingLikeTime>=LivingLikeClickBetweenTime){
                    doClickLikeInLivingRoom();
                }else {
                    if(isUsefulLivingRoom) {
                        long betweenTime = System.currentTimeMillis() - lastclickBottomLinearTime;
                        if (betweenTime <= sendLivingCommentDelay) {
                            return;
                        }
                        LogUtils.i("时间差：" + betweenTime + ",预设时间差：" + sendLivingCommentDelay);
                        doLivingTask();
                    }else{
                        showTs("在线人数或话题未满足，换房间");
                        isSwiping = true;
                        openNextOne();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isSwiping = false;
                                nowState = NORMAL;
                            }
                        }, 3000);
                    }
                }
            }
            return;
        }
        //开始干活
        switch (nowState) {
            case NORMAL:
                if (isSwiping) {
                    return;
                }
                LogUtils.d("开始干活了，当前类型：" + nowState);
                if(isKillSelfDoing){
                    return;
                }
                if(lickCount>=maxClickLikeSize){
                    isKillSelfDoing=true;
                    killAll(false);
                    isKillSelfDoing=false;
                    return;
                }
                isClickLikeThis=false;
                List<AccessibilityNodeInfo> nodes = findNodesById(enterMainAcTag);
                if (isOk(nodes)) {
                    LogUtils.d("找到音乐播放图标");
                    List<AccessibilityNodeInfo> nodeIsLiving = findNodesById(clickEnterInLivingRoom);
                    List<AccessibilityNodeInfo> nodeIsIsAd = findNodesByText(adName);
                    if (isOk(nodeIsLiving)) {
                        showTs("直播间，直接划过");
                        LogUtils.e("直播间，直接划过");
                        isSwiping = true;
                        openNextOne();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isSwiping = false;
                                nowState = NORMAL;
                            }
                        }, 1000);
                    } else {
                        if (isOk(nodeIsIsAd)) {
                            showTs("广告内容，直接划过");
                            LogUtils.e("广告内容，直接划过");
                            isSwiping = true;
                            openNextOne();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    isSwiping = false;
                                    nowState = NORMAL;
                                }
                            }, 1000);
                        } else {
                            startTimer();
                        }
                    }
                } else {
                    LogUtils.e("暂未找到音乐播放图标");
                }
                break;
            case COUNTING:
                List<AccessibilityNodeInfo> nodesWhenCounting = findNodesById(enterMainAcTag);
                if (!isOk(nodesWhenCounting)) {
                    if (!isPauseTimer && timer != null) {
                        timer.pause();
                        LogUtils.e("计时中，找不到音乐播放图标，暂停计时");
                        isPauseTimer = true;
                        showTs("页面已切换，暂停任务计时");
                    }
                } else {
                    if (timer != null && isPauseTimer) {
                        timer.resume();
                        isPauseTimer = false;
                        showTs("页面已恢复，继续上次任务");
                        LogUtils.v("计时中，找到音乐播放图标，继续计时");
                    }
                }
                break;
            case COUNT_OVER:
                LogUtils.d("计时结束了，当前类型：" + nowState);
                List<AccessibilityNodeInfo> nodesWhenCountOver = findNodesById(enterMainAcTag);
                if (isOk(nodesWhenCountOver)) {
                    setIsClickLike();
                } else {
                    nowState = NORMAL;
                }
                break;
            case LIKEING:
                if (isClickingLike) {
                    return;
                }
                LogUtils.d("开始点赞了，当前类型：" + nowState);
                List<AccessibilityNodeInfo> nodesWhenLiking = findNodesById(enterMainAcTag);
                if (isOk(nodesWhenLiking)) {
                    isClickingLike = true;
                    clickLike();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            nowState = LIKE_OVER;
                            isClickingLike = false;
                        }
                    }, 1000);
                } else {
                    nowState = NORMAL;
                }
                break;
            case LIKE_OVER:
                LogUtils.d("点赞结束了，当前类型：" + nowState);
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
                isClickLikeThis=false;
                if (isSwiping) {
                    return;
                }
                isSwiping = true;
                LogUtils.d("评论结束了，当前类型：" + nowState);
                openNextOne();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nowState = NORMAL;
                        isSwiping = false;
                        viewedVideoCount++;
                        BaseApp.getSharedPreferenceUtil().saveInt("viewedVideoCount",viewedVideoCount);
                    }
                }, 1000);
                break;
            case LIVING_RED_BAG:
                //开始查找红包的弹框是否出现了，或者已抢完的弹框是否出现了

                break;
        }
    }

    private void doClickLikeInLivingRoom() {
        isClickZan=true;
        new Thread(){
            @Override
            public void run() {
                for(int i=0;i<30;i++){
                    try {
                        forceClick(500, 500);
                        Thread.sleep(60);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                lastClickLivingLikeTime=System.currentTimeMillis();
                isClickZan=false;
            }
        }.start();

    }


    /**
     * 执行直播间对应任务
     */
    boolean isLivingTaskDoing=false;
    private void doLivingTask() {
        if(isLivingTaskDoing){
            return;
        }
        isLivingTaskDoing=true;
        //点击底部框
        try {
            forceClick(211,2264);
            lastclickBottomLinearTime=System.currentTimeMillis();
            Thread.sleep(1000);
            findEditextAndSendMsg();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void findEditextAndSendMsg() throws InterruptedException {
        List<AccessibilityNodeInfo> nodeEditTextParent = findNodesById(editTextParenetId);
        if(isOk(nodeEditTextParent)){
            //输入框已经出现
            setNodeText(nodeEditTextParent.get(0).getChild(0),getLiveCommentText());
            //点击发送按钮
            Thread.sleep(1000);
            forceClick(1000,1440);
            Thread.sleep(1000);
            forceClick(500, 500);
            Thread.sleep(1000);
            if(isSingleLivingRoomComment) {
                isLivingTaskDoing = false;
                sendLivingCommentCount++;
                showTs("已发送评论" + sendLivingCommentCount + "条");
            }else{
                isSwiping = true;
                openNextOne();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isSwiping = false;
                        isLivingTaskDoing = false;
                        nowState = NORMAL;
                    }
                }, 2000);
                sendLivingCommentCount++;
                showTs("已发送评论" + sendLivingCommentCount + "条");
            }
        }else{
            //输入框未出现,重新点击底部
            isLivingTaskDoing=false;
            findEditextAndSendMsg();
        }
    }

    /**
     * 遍历元素，查找是否存在红包图标
     * @param accessibilityNodeInfo
     * @return
     */
    private boolean checkHasRedBag(AccessibilityNodeInfo accessibilityNodeInfo) {
        if(accessibilityNodeInfo==null){
            LogUtils.e("未找到任何按钮，不点击:"+nowState);
            nowState = NORMAL;
            return false;
        }
        if(accessibilityNodeInfo.getChildCount()>0){
            checkHasRedBag(accessibilityNodeInfo.getChild(0));
        }else{
            if(accessibilityNodeInfo.getClassName().equals("android.widget.ImageView")){
                nowState = NORMAL;
                LogUtils.e("找到福袋，不点击,状态"+nowState);
                return false;
            }else{
                LogUtils.e("找到红包按钮，点击状态："+nowState);
                if(isClickingLeftTop){
                    return false;
                }
                isClickingLeftTop=true;
                LogUtils.i("开始点击左上角按钮");
                forceClick(80,350);
                lastClickBack=System.currentTimeMillis();
                lastclickBottomLinearTime =System.currentTimeMillis();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nowState = LIVING_RED_BAG;
                        isClickingLeftTop=false;
                        LogUtils.w("当前状态："+nowState);
                    }
                }, 1000);
                return true;
            }
        }
        return false;
    }


    /**
     * 判断是否是直播间
     * @return
     */
    private boolean findIsLivingRoom() {
        List<AccessibilityNodeInfo> node = findNodesById(livingRoomTopRightPeopleLinearId);
        if(isOk(node)){
            return true;
        }
        return false;
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
            if(isOpeningCommentBottomLinear){
                return;
            }
            isOpeningCommentBottomLinear=true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //评论面板已经打开
                    forceClick(436, 2250);
                    isOpeningCommentBottomLinear=false;
                    //弹起输入框
                }
            }, 500);

        }

    }


    /*
     * 杀死后台进程
     */
    public void killAll(boolean needReOpenPhone){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                forceBack();
            }
        }, 1000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                forceBack();
            }
        }, 1100);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                forceBack();
            }
        }, 1300);
        if(needReOpenPhone) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    reOpenPhone();
                }
            }, 5000);
        }else{
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                stopSelf();
                android.os.Process.killProcess(android.os.Process.myPid());    //获取PID
                System.exit(0);
                }
            }, 5000);
        }
    }




    private void reOpenPhone() {
        LogUtils.v("关机重启");
        String cmd="reboot";
        doCmd(cmd);
    }

    private void forceBack() {
        LogUtils.v("点击返回键");
        String cmd="input keyevent 4";
        doCmd(cmd);
    }

    private void setIsNeedComment() {
        int isNeedComment=RandomUtil.getRandom(100);
        LogUtils.e("预设评论概率："+commentPercent+",当前随机数值为："+isNeedComment);
        if(!isClickLikeThis){
            nowState=COMMENT_OVER;
            return;
        }
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
        if(nowState==COUNTING){
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
     * 获取评论词
     * @return
     */
    private String getLiveCommentText() {
        String [] datas=getResources().getStringArray(R.array.comments);
        String nowComment=datas[RandomUtil.getRandom(datas.length)];
        if(needRandWords){
            String x=RandomUtil.getRandomNumbersAndLetters(6);
            nowComment+=x;
        }
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
    boolean isClickLikeThis=false;//本个视频是否点赞
    public void clickLike(){
        forceClick(1020, 1410);
        lickCount++;
        isClickLikeThis=true;
        BaseApp.getSharedPreferenceUtil().saveInt("count", lickCount);
    }


    /**
     * 取消赞任务
     */
    public void cancleTask(){
        new Thread(){
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(4000);
                        LogUtils.i("取消点赞");
                        clickLike();
                        Thread.sleep(400);
                        LogUtils.i("下一个");
                        openNextOne();
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }





    long lastClickTime=0;
    boolean isClickZan=false;
    private void forceClick(int x,int y){
        if(!isClickZan&&System.currentTimeMillis()-lastClickTime<=200){
            LogUtils.w("点击冷却");
            return;
        }
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
        sendLivingCommentCount=0;
        lastclickBottomLinearTime=System.currentTimeMillis();
//        cancleTask();
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