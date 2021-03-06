package com.ispring.rocketgame.game;

import android.content.Context;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.ispring.rocketgame.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class GameView extends View {

    private Paint paint;
    private Paint textPaint;
    private Rocket rocket = null;
    private List<Sprite> sprites = new ArrayList<Sprite>();
   private List<Sprite> spritesNeedAdded = new ArrayList<Sprite>();

    private List<Bitmap> bitmaps = new ArrayList<Bitmap>();
    private float density = getResources().getDisplayMetrics().density;//屏幕密度
    public static final int STATUS_GAME_STARTED = 1;//游戏开始
    public static final int STATUS_GAME_PAUSED = 2;//游戏暂停
    public static final int STATUS_GAME_OVER = 3;//游戏结束
    public static final int STATUS_GAME_DESTROYED = 4;//游戏销毁
    public static final int STATUS_GAME_BUYUPGRADE = 5;//购买
    public static final int STATUS_GAME_BUYLV_1= 6;//购买LV1
    public static final int STATUS_GAME_BUYLV_2 = 7;//购买LV2
    private int status = STATUS_GAME_DESTROYED;//初始为销毁状态
    private  int rocket_Status = 0;
    private long frame = 0;//总共绘制的帧数
    private long score = 0;//总得分
    private int money = 0;//金币数量
    private float fontSize = 12;//默认的字体大小，用于绘制左上角的文本
    private float fontSize2 = 20;//用于在Game Over的时候绘制Dialog中的文本
    private float borderSize = 2;//Game Over的Dialog的边框
    private Rect BuyOneRect = new Rect();//"等级1"按钮的Rect
    private Rect BuyTwoRect = new Rect();//"等级2"按钮的Rect
    private Rect continueRect = new Rect();//"继续"、"重新开始"按钮的Rect
    //屏幕宽高
    WindowManager wm = (WindowManager) getContext()
            .getSystemService(Context.WINDOW_SERVICE);
    private float width = wm.getDefaultDisplay().getWidth();
    private float height = wm.getDefaultDisplay().getHeight();
    //火箭位置
    private float rocketWidth = width/2;
    private float rocketHeight = height - 20;
    //火箭属性设置
    private  float verticalSpeed = 5;
    private float horizontalSpeed = 0;
    //触摸事件相关的变量
    private static final int TOUCH_MOVE = 1;//移动
    private static final int TOUCH_SINGLE_CLICK = 2;//单击
    private static final int TOUCH_DOUBLE_CLICK = 3;//双击
    //一次单击事件由DOWN和UP两个事件合成，假设从down到up间隔小于200毫秒，我们就认为发生了一次单击事件
    private static final int singleClickDurationTime = 200;
    //一次双击事件由两个点击事件合成，两个单击事件之间小于300毫秒，我们就认为发生了一次双击事件
    private static final int doubleClickDurationTime = 300;
    private long lastSingleClickTime = -1;//上次发生单击的时刻
    private long touchDownTime = -1;//触点按下的时刻
    private long touchUpTime = -1;//触点弹起的时刻
    private float touchX = -1;//触点的x坐标
    private float touchY = -1;//触点的y坐标

    public GameView(Context context) {
        super(context);
        init(null, 0);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public GameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.GameView, defStyle, 0);
        a.recycle();
        //初始化paint
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        //设置textPaint，设置为抗锯齿，且是粗体
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.FAKE_BOLD_TEXT_FLAG);
        textPaint.setColor(0xff000000);
        fontSize = textPaint.getTextSize();
        fontSize *= density;
        fontSize2 *= density;
        textPaint.setTextSize(fontSize);
        borderSize *= density;
    }

    public void start(int[] bitmapIds){
        destroy();
        for(int bitmapId : bitmapIds){
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), bitmapId);
            bitmaps.add(bitmap);
        }
        startWhenBitmapsReady();
    }
    
    private void startWhenBitmapsReady(){
        if(rocket_Status == 0){
            rocket = new Rocket(bitmaps.get(0));
            verticalSpeed = 5;
        }else if(rocket_Status == 1){
            rocket = new Rocket(bitmaps.get(13));
            verticalSpeed = 30;
        }else {
            rocket = new Rocket(bitmaps.get(14));
            verticalSpeed = 40;
        }
        //将游戏设置为开始状态
        status = STATUS_GAME_STARTED;
        postInvalidate();
    }
    
    private void restart(){
        destroyNotRecyleBitmaps();
        startWhenBitmapsReady();
    }

    public void pause(){
        //将游戏设置为暂停状态
        status = STATUS_GAME_PAUSED;
    }
    public void buying(){
        //将游戏设置为购买暂停状态
        status = STATUS_GAME_BUYUPGRADE;
    }
    public void buyLv1(){
        //将游戏设置为购买1级状态
        status = STATUS_GAME_BUYLV_1;
    }
    public void buyLv2(){
        //将游戏设置为购买2级状态
        status = STATUS_GAME_BUYLV_2;
    }
    private void resume(){
        //将游戏设置为运行状态
        status = STATUS_GAME_STARTED;
        postInvalidate();
    }

    private long getScore(){
        //获取游戏得分
        return score;
    }

    /*-------------------------------draw-------------------------------------*/

    @Override
    protected void onDraw(Canvas canvas) {
        //我们在每一帧都检测是否满足延迟触发单击事件的条件
        if(isSingleClick()){
            onSingleClick(touchX, touchY);
        }
        super.onDraw(canvas);

        if(status == STATUS_GAME_STARTED){
            drawGameStarted(canvas);
        }else if(status == STATUS_GAME_PAUSED){
            drawGamePaused(canvas);
        }else if(status == STATUS_GAME_BUYUPGRADE){
            drawGameBuy(canvas);
        }else if(status == STATUS_GAME_BUYLV_1){
            if(money >= 200){
                money -= 200;
                BuyLvOne(canvas);
            }else{
                BuyError(canvas);
            }
        }else if(status == STATUS_GAME_BUYLV_2){
            if(money >= 400){
                money -= 400;
                BuyLvTwo(canvas);
            }else{
                BuyError(canvas);
            }
        }else if(status == STATUS_GAME_OVER){
            drawGameOver(canvas);
        }
    }
    //购买失败
    private void BuyError(Canvas canvas){
        status = STATUS_GAME_STARTED;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("没钱！" ) ;
        builder.setMessage("来啦！老弟，可惜钱不够呀！继续努力吧！" ) ;
        builder.setPositiveButton("知道了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.show();
        postInvalidate();
    }
    //购买一级火箭
    private void  BuyLvOne(Canvas canvas){
        rocket_Status = 1;
        verticalSpeed = 30;
        frame = 0;
        startWhenBitmapsReady();
    }
    //购买二级火箭
    private void  BuyLvTwo(Canvas canvas){
        rocket_Status = 2;
        verticalSpeed = 50;
        frame = 0;
        startWhenBitmapsReady();
    }
    //绘制运行状态的游戏
    private void drawGameStarted(Canvas canvas){

        drawScoreAndBombs(canvas);

        //第一次绘制时，将火箭移到Canvas最下方，在水平方向的中心
        if(frame == 0){
            rocket.centerTo(rocketWidth, rocketHeight);
        }

        //将spritesNeedAdded添加到sprites中
        if(spritesNeedAdded.size() > 0){
            sprites.addAll(spritesNeedAdded);
            spritesNeedAdded.clear();
        }

        //检查火箭跑到子弹前面的情况
        destroyBulletsFrontOfCombatAircraft();

        //在绘制之前先移除掉已经被destroyed的Sprite
        removeDestroyedSprites();

        //每隔30帧随机添加Sprite
        if(frame % 30 == 0){
            createRandomSprites(canvas.getWidth());
        }
        frame++;

        //遍历sprites，绘制敌舰、子弹、金币、奖励、特效
        Iterator<Sprite> iterator = sprites.iterator();
        while (iterator.hasNext()){
            Sprite s = iterator.next();

            if(!s.isDestroyed()){
                //在Sprite的draw方法内有可能会调用destroy方法
                s.draw(canvas, paint, this);
            }

            //我们此处要判断Sprite在执行了draw方法后是否被destroy掉了
            if(s.isDestroyed()){
                //如果Sprite被销毁了，那么从Sprites中将其移除
                iterator.remove();
            }
        }

        if(rocket != null){
            //最后绘制火箭
            rocket.centerTo(rocketWidth, rocketHeight);
            rocket.draw(canvas, paint, this);
            if(rocket.isDestroyed()){
                //如果火箭被击中销毁了，那么游戏结束
                status = STATUS_GAME_OVER;
            }
            //通过调用postInvalidate()方法使得View持续渲染，实现动态效果
            postInvalidate();
        }
    }

    //绘制暂停状态的游戏
    private void drawGamePaused(Canvas canvas){
        drawScoreAndBombs(canvas);

        //调用Sprite的onDraw方法，而非draw方法，这样就能渲染静态的Sprite，而不让Sprite改变位置
        for(Sprite s : sprites){
            s.onDraw(canvas, paint, this);
        }
        if(rocket != null){
            rocket.onDraw(canvas, paint, this);
        }

        //绘制Dialog，显示得分
        drawScoreDialog(canvas, "继续");

        if(lastSingleClickTime > 0){
            postInvalidate();
        }
    }
    //绘制商店状态的游戏
    private void drawGameBuy(Canvas canvas){
        drawScoreAndBombs(canvas);

        //调用Sprite的onDraw方法，而非draw方法，这样就能渲染静态的Sprite，而不让Sprite改变位置
        for(Sprite s : sprites){
            s.onDraw(canvas, paint, this);
        }
        if(rocket != null){
            rocket.onDraw(canvas, paint, this);
        }

        //绘制Dialog，显示商店
        drawBuyDialog(canvas, "继续");

        if(lastSingleClickTime > 0){
            postInvalidate();
        }
    }

    //绘制结束状态的游戏
    private void drawGameOver(Canvas canvas){
        //Game Over之后只绘制弹出窗显示最终得分
        drawScoreDialog(canvas, "重新开始");

        if(lastSingleClickTime > 0){
            postInvalidate();
        }
    }
    private void drawBuyDialog(Canvas canvas, String operation){
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        //存储原始值
        float originalFontSize = textPaint.getTextSize();
        Paint.Align originalFontAlign = textPaint.getTextAlign();
        int originalColor = paint.getColor();
        Paint.Style originalStyle = paint.getStyle();
        /*
        W = 360
        w1 = 20
        w2 = 320
        buttonWidth = 140
        buttonHeight = 42
        H = 558
        h1 = 150
        h2 = 60
        h3 = 124
        h4 = 76
        */
        int w1 = (int)(20.0 / 360.0 * canvasWidth);
        int w2 = canvasWidth - 2 * w1;
        int buybuttonWidth = (int)(100.0 / 360.0 * canvasHeight);
        int buttonWidth = (int)(140.0 / 360.0 * canvasWidth);

        int h1 = (int)(100.0 / 558.0 * canvasHeight);
        int h2 = (int)(60.0 / 558.0 * canvasHeight);
        int h3 = (int)(200.0 / 558.0 * canvasHeight);
        int h4 = (int)(76.0 / 558.0 * canvasHeight);
        int buybuttonHeight = (int)(180.0 / 558.0 * canvasHeight);
        int buttonHeight = (int)(42.0 / 558.0 * canvasHeight);

        canvas.translate(w1, h1);
        //绘制背景色
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFD7DDDE);
        Rect rect1 = new Rect(0, 0, w2, canvasHeight - 2 * h1);
        canvas.drawRect(rect1, paint);
        //绘制边框
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF515151);
        paint.setStrokeWidth(borderSize);
        //paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawRect(rect1, paint);
        //绘制文本"商店"
        textPaint.setTextSize(fontSize2);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("商店", w2 / 2, (h2 - fontSize2) / 2 + fontSize2, textPaint);
        //绘制"商店"下面的横线
        canvas.translate(0, h2);
        canvas.drawLine(0, 0, w2, 0, paint);
       if(rocket_Status == 0){
           //绘制LV1物品按钮边框
           Rect rectLvOne = new Rect();
           rectLvOne.left = (w2 - buybuttonWidth)/2;
           rectLvOne.right = w2 - rectLvOne.left;
           rectLvOne.top = ( h3- buybuttonHeight) / 2;
           rectLvOne.bottom = h3 - rectLvOne.top;
           canvas.drawRect(rectLvOne, paint);
           //绘制商店的LV1物品
           Bitmap OneBitmap = bitmaps.get(13);  //LV1火箭
           Bitmap MoneyBitmap = bitmaps.get(18);    //金币
           canvas.drawBitmap(OneBitmap, (3 * rectLvOne.left+rectLvOne.right)/4, (3 * rectLvOne.top+rectLvOne.bottom)/4, paint);
           canvas.drawText("等级 1", w2 / 3, (h3 - fontSize2) / 2 + fontSize2 - 10, textPaint);
           canvas.drawBitmap(MoneyBitmap,2*w2 / 3, (rectLvOne.top+rectLvOne.bottom)/2, paint);
           canvas.drawText("200个", 2*w2 / 3, (rectLvOne.top+rectLvOne.bottom)/2, textPaint);
           BuyOneRect = new Rect(rectLvOne);
           BuyOneRect.left = w1 + rectLvOne.left;
           BuyOneRect.right = BuyOneRect.left + buybuttonWidth;
           BuyOneRect.top = h1 + h2 + rectLvOne.top;
           BuyOneRect.bottom = BuyOneRect.top + buybuttonHeight;
       }else if(rocket_Status == 1){
           //绘制LV2物品按钮边框
           Rect rectLvTwo = new Rect();
           rectLvTwo.left = (w2 - buybuttonWidth)/2;
           rectLvTwo.right = w2 - rectLvTwo.left;
           rectLvTwo.top = ( h3- buybuttonHeight) / 2;
           rectLvTwo.bottom = h3 - rectLvTwo.top;
           canvas.drawRect(rectLvTwo, paint);
           //绘制商店的LV2物品
           Bitmap TwoBitmap = bitmaps.get(14);  //LV2火箭
           Bitmap MoneyBitmap = bitmaps.get(18);    //金币
           canvas.drawBitmap(TwoBitmap, (3 * rectLvTwo.left+rectLvTwo.right)/4, (3 * rectLvTwo.top+rectLvTwo.bottom)/4, paint);
           canvas.drawText("等级 2", w2 / 3, (h3 - fontSize2) / 2 + fontSize2, textPaint);
           canvas.drawBitmap(MoneyBitmap,2*w2 / 3, (rectLvTwo.top+rectLvTwo.bottom)/2, paint);
           canvas.drawText("400个", 2*w2 / 3, (rectLvTwo.top+rectLvTwo.bottom)/2, textPaint);
           BuyTwoRect = new Rect(rectLvTwo);
           BuyTwoRect.left = w1 + rectLvTwo.left;
           BuyTwoRect.right = BuyTwoRect.left + buybuttonWidth;
           BuyTwoRect.top = h1 + h2 + rectLvTwo.top;
           BuyTwoRect.bottom = BuyTwoRect.top + buybuttonHeight;
       }else{
           //绘制顶级购买商店状态
           canvas.drawText("已经购买所有升级！！", w2 / 2, (h3 - fontSize2) / 2 + fontSize2, textPaint);
       }
        //绘制"购买等级"下面的线
        canvas.translate(0, h3);
        canvas.drawLine(0, 0, w2, 0, paint);
        //绘制按钮边框
        Rect rect2 = new Rect();
        rect2.left = (w2 - buttonWidth) / 2;
        rect2.right = w2 - rect2.left;
        rect2.top = (h4 - buttonHeight) / 2;
        rect2.bottom = h4 - rect2.top;
        canvas.drawRect(rect2, paint);
        //绘制文本"继续"或"重新开始"
        canvas.translate(0, rect2.top);
        canvas.drawText(operation, w2 / 2, (buttonHeight - fontSize2) / 2 + fontSize2, textPaint);
        continueRect = new Rect(rect2);
        continueRect.left = w1 + rect2.left;
        continueRect.right = continueRect.left + buttonWidth;
        continueRect.top = h1 + h2 + h3 + rect2.top;
        continueRect.bottom = continueRect.top + buttonHeight;
        //重置
        textPaint.setTextSize(originalFontSize);
        textPaint.setTextAlign(originalFontAlign);
        paint.setColor(originalColor);
        paint.setStyle(originalStyle);
    }
    private void drawScoreDialog(Canvas canvas, String operation){
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        //存储原始值
        float originalFontSize = textPaint.getTextSize();
        Paint.Align originalFontAlign = textPaint.getTextAlign();
        int originalColor = paint.getColor();
        Paint.Style originalStyle = paint.getStyle();
        /*
        W = 360
        w1 = 20
        w2 = 320
        buttonWidth = 140
        buttonHeight = 42
        H = 558
        h1 = 150
        h2 = 60
        h3 = 124
        h4 = 76
        */
        int w1 = (int)(20.0 / 360.0 * canvasWidth);
        int w2 = canvasWidth - 2 * w1;
        int buttonWidth = (int)(140.0 / 360.0 * canvasWidth);

        int h1 = (int)(150.0 / 558.0 * canvasHeight);
        int h2 = (int)(60.0 / 558.0 * canvasHeight);
        int h3 = (int)(124.0 / 558.0 * canvasHeight);
        int h4 = (int)(76.0 / 558.0 * canvasHeight);
        int buttonHeight = (int)(42.0 / 558.0 * canvasHeight);

        canvas.translate(w1, h1);
        //绘制背景色
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFD7DDDE);
        Rect rect1 = new Rect(0, 0, w2, canvasHeight - 2 * h1);
        canvas.drawRect(rect1, paint);
        //绘制边框
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF515151);
        paint.setStrokeWidth(borderSize);
        //paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawRect(rect1, paint);
        //绘制文本"火箭飞天分数"
        textPaint.setTextSize(fontSize2);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("火箭飞天分数", w2 / 2, (h2 - fontSize2) / 2 + fontSize2, textPaint);
        //绘制"火箭飞天分数"下面的横线
        canvas.translate(0, h2);
        canvas.drawLine(0, 0, w2, 0, paint);
        //绘制实际的分数
        String allScore = String.valueOf(getScore());
        canvas.drawText(allScore, w2 / 2, (h3 - fontSize2) / 2 + fontSize2, textPaint);
        //绘制分数下面的横线
        canvas.translate(0, h3);
        canvas.drawLine(0, 0, w2, 0, paint);
        //绘制按钮边框
        Rect rect2 = new Rect();
        rect2.left = (w2 - buttonWidth) / 2;
        rect2.right = w2 - rect2.left;
        rect2.top = (h4 - buttonHeight) / 2;
        rect2.bottom = h4 - rect2.top;
        canvas.drawRect(rect2, paint);
        //绘制文本"继续"或"重新开始"
        canvas.translate(0, rect2.top);
        canvas.drawText(operation, w2 / 2, (buttonHeight - fontSize2) / 2 + fontSize2, textPaint);
        continueRect = new Rect(rect2);
        continueRect.left = w1 + rect2.left;
        continueRect.right = continueRect.left + buttonWidth;
        continueRect.top = h1 + h2 + h3 + rect2.top;
        continueRect.bottom = continueRect.top + buttonHeight;

        //重置
        textPaint.setTextSize(originalFontSize);
        textPaint.setTextAlign(originalFontAlign);
        paint.setColor(originalColor);
        paint.setStyle(originalStyle);
    }

    //绘制左上角的得分和左下角炸弹的数量
    private void drawScoreAndBombs(Canvas canvas){
        //绘制左上角的暂停按钮
        Bitmap pauseBitmap = status == STATUS_GAME_STARTED ? bitmaps.get(9) : bitmaps.get(10);
        RectF pauseBitmapDstRecF = getPauseBitmapDstRecF();
        float pauseLeft = pauseBitmapDstRecF.left;
        float pauseTop = pauseBitmapDstRecF.top;
        canvas.drawBitmap(pauseBitmap, pauseLeft, pauseTop, paint);
        //绘制右上角的商店按钮
        Bitmap buyBitmap = status == STATUS_GAME_STARTED ? bitmaps.get(15):bitmaps.get(16);
        RectF getBuyBitmapDstRecF = getBuyBitmapDstRecF();
        float buyLeft = getBuyBitmapDstRecF.left;
        float buyTop = getBuyBitmapDstRecF.top;
        canvas.drawBitmap(buyBitmap, buyLeft, buyTop, paint);
        //绘制左上角的总得分数
        float scoreLeft = pauseLeft + pauseBitmap.getWidth() + 20 * density;
        float scoreTop = fontSize + pauseTop + pauseBitmap.getHeight() / 2 - fontSize / 2;
        canvas.drawText(score + "", scoreLeft, scoreTop, textPaint);

        //绘制左下角
        if(rocket != null && !rocket.isDestroyed() || money > 0){
            int bombCount = rocket.getBombCount();
            int bigMoneyCount = money;
            if(bombCount > 0) {
                //绘制左下角的炸弹
                Bitmap bombBitmap = bitmaps.get(11);
                float bombTop = canvas.getHeight() - bombBitmap.getHeight();
                canvas.drawBitmap(bombBitmap, 0, bombTop, paint);
                //绘制左下角的炸弹数量
                float bombCountLeft = bombBitmap.getWidth() + 10 * density;
                float bombCountTop = fontSize + bombTop + bombBitmap.getHeight() / 2 - fontSize / 2;
                canvas.drawText("X " + bombCount, bombCountLeft, bombCountTop, textPaint);
            }
            if(bigMoneyCount > 0){
                //绘制左下角的金币
                Bitmap bigMoneyBitmap = bitmaps.get(18);
                float bigMoneyTop = canvas.getHeight() - bigMoneyBitmap.getHeight() - 60 * density;
                canvas.drawBitmap(bigMoneyBitmap, 0, bigMoneyTop, paint);
                //绘制左下角的金币数量
                float bigMoneyCountLeft = bigMoneyBitmap.getWidth() + 20 * density;
                float bigMoneyCountTop = fontSize + bigMoneyTop + bigMoneyBitmap.getHeight() / 2 - fontSize / 2;
                canvas.drawText("X " + bigMoneyCount, bigMoneyCountLeft, bigMoneyCountTop, textPaint);
            }
        }
    }

    //检查火箭跑到子弹前面的情况
    private void destroyBulletsFrontOfCombatAircraft(){
        if(rocket != null){
            float aircraftY = rocket.getY();
            List<Bullet> aliveBullets = getAliveBullets();
            for(Bullet bullet : aliveBullets){
                //如果火箭跑到了子弹前面，那么就销毁子弹
                if(aircraftY <= bullet.getY()){
                    bullet.destroy();
                }
            }
        }
    }

    //移除掉已经destroyed的Sprite
    private void removeDestroyedSprites(){
        Iterator<Sprite> iterator = sprites.iterator();
        while (iterator.hasNext()){
            Sprite s = iterator.next();
            if(s.isDestroyed()){
                iterator.remove();
            }
        }
    }

    //生成随机的Sprite
    private void createRandomSprites(int canvasWidth){
        Sprite sprite = null;
        int speed = 2;
        //callTime表示createRandomSprites方法被调用的次数
        int callTime = Math.round(frame / 30);
        if((callTime + 1) % 25 == 0){
            //发送道具奖品
            if((callTime + 1) % 50 == 0){
                //发送炸弹
                sprite = new BombAward(bitmaps.get(12));
            }
            else{
                //发送双子弹
                sprite = new BulletAward(bitmaps.get(8));
            }
        }
        else{
            //发送舰
            int[] nums = {0,0,0,2,5,5,0,0,1,0,0,1,0,5,0,0,3,2,0,1,1,1,1,1,1,2,3,5,5,4};
            int index = (int)Math.floor(nums.length*Math.random());
            int type = nums[index];
            if(type == 0){
                //小战舰
                sprite = new SmallWarship(bitmaps.get(4));
            }
            else if(type == 1){
                //中战舰
                sprite = new MiddleWarship(bitmaps.get(5));
            }
            else if(type == 2) {
                //大战舰
                sprite = new BigWarship(bitmaps.get(6));
            }
            else if(type == 3){
                //超级战舰
                sprite = new SuperWarship(bitmaps.get(19));
            }else if(type == 4 && score > 1000000){
                //X战舰
                sprite = new XWarship(bitmaps.get(20));
            }else if(type == 5){
                //大金币
                sprite = new Gold(bitmaps.get(17));
            }
            if(type != 0 || type != 1 || type != 2 || type != 3){
                if(Math.random() < 0.33){
                    speed = 4;
                }
            }
        }

        if(sprite != null){
            float spriteWidth = sprite.getWidth();
            float spriteHeight = sprite.getHeight();
            float x = (float)((canvasWidth - spriteWidth)*Math.random());
            float y = -spriteHeight;
            sprite.setX(x);
            sprite.setY(y);
            if(sprite instanceof VerticalSprite){
                VerticalSprite verticalSprite = (VerticalSprite)sprite;
                verticalSprite.setSpeed(speed);
            }
            addSprite(sprite);
        }
    }

    /*-------------------------------touch------------------------------------*/

    @Override
    public boolean onTouchEvent(MotionEvent event){
        //通过调用resolveTouchType方法，得到我们想要的事件类型
        //需要注意的是resolveTouchType方法不会返回TOUCH_SINGLE_CLICK类型
        //我们会在onDraw方法每次执行的时候，都会调用isSingleClick方法检测是否触发了单击事件
        int touchType = resolveTouchType(event);
        if(status == STATUS_GAME_STARTED){
            if(touchType == TOUCH_MOVE){
                if(rocket != null){
                    //如果x为左边区域，向左边移动verticalSpeed，或者右边
                    if(touchX <= width/2){
                        rocketWidth -= verticalSpeed;
                        rocket.centerTo(rocketWidth,rocketHeight);
                    }else{
                        rocketWidth += verticalSpeed;
                        rocket.centerTo(rocketWidth,rocketHeight);
                    }
                }
            }else if(touchType == TOUCH_DOUBLE_CLICK){
                if(status == STATUS_GAME_STARTED){
                    if(rocket != null){
                        //双击会使得火箭使用炸弹
                        rocket.bomb(this);
                    }
                }
            }
        }else if(status == STATUS_GAME_PAUSED){
            if(lastSingleClickTime > 0){
                postInvalidate();
            }
        }else if(status == STATUS_GAME_BUYUPGRADE){
            if(lastSingleClickTime > 0){
                postInvalidate();
            }
        }else if(status == STATUS_GAME_BUYLV_1) {
            if(lastSingleClickTime > 0){
                postInvalidate();
            }
        }
        else if(status == STATUS_GAME_BUYLV_2) {
            if(lastSingleClickTime > 0){
                postInvalidate();
            }
        }
        else if(status == STATUS_GAME_OVER){
            if(lastSingleClickTime > 0){
                postInvalidate();
            }
        }
        return true;
    }

    //合成我们想要的事件类型
    private int resolveTouchType(MotionEvent event){
        int touchType = -1;
        int action = event.getAction();
        float lastX = touchX;
        float lastY = touchY;
        touchX = event.getX();
        touchY = event.getY();
        if(action == MotionEvent.ACTION_MOVE){
            long deltaTime = System.currentTimeMillis() - touchDownTime;
            if(deltaTime > singleClickDurationTime){
                //触点移动
                touchType = TOUCH_MOVE;
            }
        }else if(action == MotionEvent.ACTION_DOWN){
            //触点按下
            touchType = TOUCH_MOVE;
            touchDownTime = System.currentTimeMillis();
        }else if(action == MotionEvent.ACTION_UP){
            //触点弹起
            touchUpTime = System.currentTimeMillis();
            //计算触点按下到触点弹起之间的时间差
            long downUpDurationTime = touchUpTime - touchDownTime;
            //如果此次触点按下和抬起之间的时间差小于一次单击事件指定的时间差，
            //那么我们就认为发生了一次单击
            if(downUpDurationTime <= singleClickDurationTime){
                //计算这次单击距离上次单击的时间差
                long twoClickDurationTime = touchUpTime - lastSingleClickTime;

                if(twoClickDurationTime <=  doubleClickDurationTime){
                    //如果两次单击的时间差小于一次双击事件执行的时间差，
                    //那么我们就认为发生了一次双击事件
                    touchType = TOUCH_DOUBLE_CLICK;
                    //重置变量
                    lastSingleClickTime = -1;
                    touchDownTime = -1;
                    touchUpTime = -1;
                }else{
                    //如果这次形成了单击事件，但是没有形成双击事件，那么我们暂不触发此次形成的单击事件
                    //我们应该在doubleClickDurationTime毫秒后看一下有没有再次形成第二个单击事件
                    //如果那时形成了第二个单击事件，那么我们就与此次的单击事件合成一次双击事件
                    //否则在doubleClickDurationTime毫秒后触发此次的单击事件
                    lastSingleClickTime = touchUpTime;
                }
            }
        }
        return touchType;
    }

    //在onDraw方法中调用该方法，在每一帧都检查是不是发生了单击事件
    private boolean isSingleClick(){
        boolean singleClick = false;
        //我们检查一下是不是上次的单击事件在经过了doubleClickDurationTime毫秒后满足触发单击事件的条件
        if(lastSingleClickTime > 0){
            //计算当前时刻距离上次发生单击事件的时间差
            long deltaTime = System.currentTimeMillis() - lastSingleClickTime;
            if(deltaTime >= doubleClickDurationTime){
                //如果时间差超过了一次双击事件所需要的时间差，
                //那么就在此刻延迟触发之前本该发生的单击事件
                singleClick = true;
                //重置变量
                lastSingleClickTime = -1;
                touchDownTime = -1;
                touchUpTime = -1;
            }
        }
        return singleClick;
    }

    private void onSingleClick(float x, float y) {
        if (status == STATUS_GAME_STARTED) {
            if (isClickPause(x, y)) {
                //单击了暂停按钮
                pause();
            } else if (isClickBuy(x, y)) {
                //单击了商店按钮
                buying();
            }
        } else if (status == STATUS_GAME_PAUSED) {
            if (isClickContinueButton(x, y)) {
                //单击了“继续”按钮
                resume();
            }
        } else if (status == STATUS_GAME_BUYUPGRADE) {
            if (isClickContinueButton(x, y)) {
                //单击了“继续”按钮
                resume();
            }
            if (isClickLvOneButton(x, y)) {
                //单击了LV1按钮
                buyLv1();
            }
            if (isClickLvTwoButton(x, y)) {
                //单击了LV2按钮
                buyLv2();
            }
        }else if(status == STATUS_GAME_OVER){
            if(isClickRestartButton(x, y)){
                //单击了“重新开始”按钮
                restart();
            }
        }
    }

    //是否单击了左上角的暂停按钮
    private boolean isClickPause(float x, float y){
        RectF pauseRecF = getPauseBitmapDstRecF();
        return pauseRecF.contains(x, y);
    }
    //是否单击了右上角的商店按钮
    private boolean isClickBuy(float x, float y){
        RectF buyRecF = getBuyBitmapDstRecF();
        return buyRecF.contains(x, y);
    }

    //是否单击了暂停状态下的“继续”按妞
    private boolean isClickContinueButton(float x, float y){
        return continueRect.contains((int)x, (int)y);
    }
    //是否单击了购买状态下的“LV1”按钮
    private boolean isClickLvOneButton(float x, float y){
        return BuyOneRect.contains((int)x, (int)y);
    }
    //是否单击了购买状态下的“LV2”按钮
    private boolean isClickLvTwoButton(float x, float y){
        return BuyTwoRect.contains((int)x, (int)y);
    }
    //是否单击了GAME OVER状态下的“重新开始”按钮
    private boolean isClickRestartButton(float x, float y){
        return continueRect.contains((int)x, (int)y);
    }

    private RectF getPauseBitmapDstRecF(){
        Bitmap pauseBitmap = status == STATUS_GAME_STARTED ? bitmaps.get(9) : bitmaps.get(10);
        RectF recF = new RectF();
        recF.left = 15 * density;
        recF.top = 15 * density;
        recF.right = recF.left + pauseBitmap.getWidth();
        recF.bottom = recF.top + pauseBitmap.getHeight();
        return recF;
    }
    private RectF getBuyBitmapDstRecF(){
        Bitmap buyBitmap = status == STATUS_GAME_STARTED ? bitmaps.get(15) : bitmaps.get(16);
        RectF recF = new RectF();
        recF.left = 15 * density;
        recF.top = 50 * density;
        recF.right = recF.left + buyBitmap.getWidth();
        recF.bottom = recF.top + buyBitmap.getHeight();
        return recF;
    }

    /*-------------------------------destroy------------------------------------*/
    
    private void destroyNotRecyleBitmaps(){
        //将游戏设置为销毁状态
        status = STATUS_GAME_DESTROYED;

        //重置frame
        frame = 0;

        //重置得分
        score = 0;

        //销毁火箭
        if(rocket != null){
            rocket.destroy();
        }
        rocket = null;

        //销毁敌舰、子弹、奖励、特效、金币
        for(Sprite s : sprites){
            s.destroy();
        }
        sprites.clear();
    }

    public void destroy(){
        destroyNotRecyleBitmaps();

        //释放Bitmap资源
        for(Bitmap bitmap : bitmaps){
            bitmap.recycle();
        }
        bitmaps.clear();
    }

    /*-------------------------------public methods-----------------------------------*/

    //向Sprites中添加Sprite
    public void addSprite(Sprite sprite){
        spritesNeedAdded.add(sprite);
    }

    //添加得分
    public void addScore(int value){
        score += value;
    }
    //添加已获得金币数
    public void addMoney(int value){
        money += value;
    }

    public int getStatus(){
        return status;
    }

    public float getDensity(){
        return density;
    }

    public Bitmap getYellowBulletBitmap(){
        return bitmaps.get(2);
    }

    public Bitmap getBlueBulletBitmap(){
        return bitmaps.get(3);
    }

    public Bitmap getExplosionBitmap(){
        return bitmaps.get(1);
    }

    //获取处于活动状态的敌人
    public List<Warship> getAliveEnemyPlanes(){
        List<Warship> warships = new ArrayList<Warship>();
        for(Sprite s : sprites){
            if(!s.isDestroyed() && s instanceof Warship){
                Warship sprite = (Warship)s;
                warships.add(sprite);
            }
        }
        return warships;
    }

    //获取处于活动状态的金币
    public List<Money> getAliveMoneyPlanes(){
        List<Money> money = new ArrayList<Money>();
        for(Sprite s : sprites){
            if(!s.isDestroyed() && s instanceof Money){
                Money sprite = (Money)s;
                money.add(sprite);
            }
        }
        return money;
    }

    //获得处于活动状态的炸弹奖励
    public List<BombAward> getAliveBombAwards(){
        List<BombAward> bombAwards = new ArrayList<BombAward>();
        for(Sprite s : sprites){
            if(!s.isDestroyed() && s instanceof BombAward){
                BombAward bombAward = (BombAward)s;
                bombAwards.add(bombAward);
            }
        }
        return bombAwards;
    }

    //获取处于活动状态的子弹奖励
    public List<BulletAward> getAliveBulletAwards(){
        List<BulletAward> bulletAwards = new ArrayList<BulletAward>();
        for(Sprite s : sprites){
            if(!s.isDestroyed() && s instanceof BulletAward){
                BulletAward bulletAward = (BulletAward)s;
                bulletAwards.add(bulletAward);
            }
        }
        return bulletAwards;
    }

    //获取处于活动状态的子弹
    public List<Bullet> getAliveBullets(){
        List<Bullet> bullets = new ArrayList<Bullet>();
        for(Sprite s : sprites){
            if(!s.isDestroyed() && s instanceof Bullet){
                Bullet bullet = (Bullet)s;
                bullets.add(bullet);
            }
        }
        return bullets;
    }
}