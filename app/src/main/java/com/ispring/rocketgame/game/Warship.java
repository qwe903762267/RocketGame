package com.ispring.rocketgame.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

import java.util.List;

/**
 * 敌舰类，从上向下沿直线运动
 */
public class Warship extends VerticalSprite {

    private int power = 1;//敌舰的抗打击能力
    private int value = 0;//打一个敌舰的得分

    public Warship(Bitmap bitmap){
        super(bitmap);
    }

    public void setPower(int power){
        this.power = power;
    }

    public int getPower(){
        return power;
    }

    public void setValue(int value){
        this.value = value;
    }

    public int getValue(){
        return value;
    }

    @Override
    protected void afterDraw(Canvas canvas, Paint paint, GameView gameView) {
        super.afterDraw(canvas, paint, gameView);

        //绘制完成后要检查自身是否被子弹打中
        if(!isDestroyed()){
            //敌舰在绘制完成后要判断是否被子弹打中

            List<Bullet> bullets = gameView.getAliveBullets();
            for(Bullet bullet : bullets){
                //判断敌舰是否与子弹相交
                Point p = getCollidePointWithOther(bullet);
                if(p != null){
                    //如果有交点，说明子弹打到了战舰上
                    bullet.destroy();
                    power--;
                    if(power <= 0){
                        //敌舰已经没有能量了，执行爆炸效果
                        explode(gameView);
                        return;
                    }
                }
            }
        }
    }

    //创建爆炸效果后会销毁敌舰
    public void explode(GameView gameView){
        //创建爆炸效果
        float centerX = getX() + getWidth() / 2;
        float centerY = getY() + getHeight() / 2;
        Bitmap bitmap = gameView.getExplosionBitmap();
        Effects effects = new Effects(bitmap);
        effects.centerTo(centerX, centerY);
        gameView.addSprite(effects);

        //创建爆炸效果完成后，向GameView中添加得分并销毁敌舰
        gameView.addScore(value);
        destroy();
    }
}