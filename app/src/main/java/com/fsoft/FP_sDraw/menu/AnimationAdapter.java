package com.fsoft.FP_sDraw.menu;

import android.graphics.Canvas;
import android.os.Handler;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;

import java.util.ArrayList;

/**
 * класс создан для эффективного управления анимационными эффектами.
 * Использование:
 * - наследоваться
 * - в конструкторе вызвать функции setFrameRate() и setAnimTime() если нужно
 * - функция setQuickOn позволяет сделать быстрое повыщение процента и медленное понижеине
 * - реализовать рисование в функции draw() с использованием параметра getAnimPercent() 0...100
 * - переопределить функцию redraw()  в котором вызвать функцию Invaligate целевого обьекта
 * - вызывать функцию draw() при отрисовке анимируемого обьекта
 * - вызвать setPressed(bool) для запуска анимации
 * Created by Dr. Failov on 18.11.2015.
 */
abstract public class AnimationAdapter {
    public interface OnAnimationCorrectlyFinished{
        void onFinished();
    }

    private final ArrayList<Thread> animationThreads = new ArrayList();
    private final Handler handler = new Handler();
    private final float minPercent = 0;
    private final float maxPercent = 100;
    private float animPercent = minPercent;
    private float animTime = 300;
    private float frameRate = 60;
    private boolean quickOn = false;
    private OnAnimationCorrectlyFinished onAnimationCorrectlyFinished = null;

    abstract public void draw(Canvas canvas);
    abstract protected void redraw();

    public void startAnimation(){
        animPercent = minPercent;
        startPressing(false);
    }
    public void setPressed(boolean p){
        startPressing(!p);
    }
    public void setFrameRate(int fps){
        frameRate = fps;
    }
    public void setAnimTime(int ms){
        animTime = ms;
    }
    public float getAnimPercent(){
        return animPercent;
    }
    public void setQuickOn(boolean qo){
        this.quickOn = qo;
    }
    public void setOnAnimationCorrectlyFinished(OnAnimationCorrectlyFinished onAnimationCorrectlyFinished){
        this.onAnimationCorrectlyFinished = onAnimationCorrectlyFinished;
    }

    private void startPressing( final  boolean unpress){
        //прервать все потоки. эта штука нужна чтобы избежать исключения конкурентного обрыва
        boolean ok = false;
        while(!ok) {
            try {
                for (Thread t : animationThreads)
                    t.interrupt();
                ok = true;
            } catch (Exception e) {
            }
        }

        Thread animationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                float frameTime = 1000 / frameRate;
                float frames = animTime / frameTime;
                float percentStep = maxPercent/frames;
                if(unpress)
                    percentStep = -percentStep;
                try {
                    for (; (unpress ? animPercent > minPercent : animPercent < maxPercent) && !Thread.currentThread().isInterrupted(); animPercent += percentStep) {
                        if(quickOn && !unpress)
                            Thread.sleep((int) frameTime/3);
                        else
                            Thread.sleep((int) frameTime);
                        postRedraw();
                    }

                    if (!Thread.interrupted()) {
                        //annimation correctly ended
                        animationThreads.remove(Thread.currentThread());
                        if(onAnimationCorrectlyFinished != null)
                            onAnimationCorrectlyFinished.onFinished();
                    }
                }
                catch (Exception e){
                    //Logger.log("thread stopped");
                }
                finally {
                    if(animPercent < minPercent) animPercent = minPercent;
                    if(animPercent > maxPercent) animPercent = maxPercent;
                }
            }
        });
        animationThreads.add(animationThread);
        animationThread.start();
    }
    private void postRedraw(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    redraw();
                }
                catch (Exception e){
                    Logger.log("AnimationAdapter.postRedraw", "Error while redrawing: " + e + "\n StackTrace: \n" + Tools.getStackTrace(e), false);
                }
            }
        });
    }
}
