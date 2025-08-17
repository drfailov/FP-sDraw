package com.fsoft.FP_sDraw.common;

import android.support.annotation.NonNull;

/**
 * Штука для вычисления области в которой выполнялись действия между вызовами reset.
 * Usage:
     * `add()
     * check()
     * !ifEmpty()
     *      //make undo
     *      reset()`
 * Created by Dr. Failov on 02.01.2015.
 */
public class AreaCalculator {
    private final int emptyValue = -9876;
    public int top=emptyValue;
    public int left=0;
    public int right=0;
    public int bottom=0;
    final int fund=15;
    public void reset(){
        top=emptyValue;
    }
    public boolean isEmpty(){
        return top == emptyValue;
    }
    public void add(float x, float y, float brush){
        add((int)x, (int)y, (int)brush);
    }
    public void add(int x, int y, int brush) {
        try{
            if(top==-9876) {     //start
                top=y-(fund+brush);
                bottom=y+(fund+brush);
                left=x-(fund+brush);
                right=x+(fund+brush);
            }
            else {     //add
                int ttop = y - (fund + brush);
                if(ttop <top) top= ttop;
                int tbottom = y + (fund + brush);
                if(tbottom >bottom) bottom= tbottom;
                int tleft = x - (fund + brush);
                if(tleft <left) left= tleft;
                //temp
                int tright = x + (fund + brush);
                if(tright >right) right= tright;
            }
        }
        catch (Exception|OutOfMemoryError e){
            Logger.log(e.toString() + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
        }
    }
    public void check(int width, int height) {
        try{
            if(top<0)top=0;
            if(left<0)left=0;
            if(right>width)right=width;
            if(bottom>height)bottom=height;
        }
        catch (Exception|OutOfMemoryError e){
            Logger.log(e.toString() + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
        }
    }
    @NonNull
    @Override public String toString() {
        String result="";
        result+="top="+top;
        result+="; bottom="+bottom;
        result+="; left="+left;
        result+="; right="+right+";";
        return result;
    }
}
