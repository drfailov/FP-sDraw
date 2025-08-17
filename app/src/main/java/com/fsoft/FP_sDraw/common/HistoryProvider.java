package com.fsoft.FP_sDraw.common;

import android.view.MotionEvent;

/**
 * Класс для хранения истории перемещения
 * Created by Dr. Failov on 02.01.2015.
 */
public class HistoryProvider{
    public int historySize=3;
    int multitouchMax;
    Point[][] database;
    public HistoryProvider(int _historySize, int _multitouchMax) {
        try{
            historySize = _historySize;
            multitouchMax = _multitouchMax;
            database=new Point[historySize][multitouchMax];
            for (int i=0;i<historySize;i++)
                for(int j=0;j< multitouchMax;j++)
                    database[i][j]=new Point(-1,-1);
        }catch (Exception|OutOfMemoryError e){
            Logger.log(e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
        }
    }
    public void write(MotionEvent event){
        try{
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if(event.getAction() == MotionEvent.ACTION_MOVE   //при перемещении сдвигаем весь массив
                    || action == 213){  //stylus MOVE with pressed button on Galaxy Tab S4
                //shift
                Point[] tmp =database[historySize-1];
                for(int i=historySize-1; i>0; i--) {
                    database[i]=database[i-1];
                }
                database[0]=tmp;
                //save
                for(int i=0; i<event.getPointerCount(); i++)
                    database[0][event.getPointerId(i)].set(event.getX(i), event.getY(i));
            }
            else if(action == MotionEvent.ACTION_DOWN   //при опускании сдвигаем только ту часть массива что отвучает за совершивший действие палец
                    || action == MotionEvent.ACTION_POINTER_DOWN
                    || action == 211)  { //stylus DOWN with pressed button on Galaxy Tab S4
                int reasonIndex=(event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
                int reasonID = event.getPointerId(reasonIndex);
                //shift
                Point tmp=database[historySize-1][reasonID];
                for(int i=historySize-1; i>0; i--) {
                    database[i][reasonID]=database[i-1][reasonID];
                }
                database[0][reasonID]=tmp;
                //save
                database[0][reasonID].set(event.getX(reasonIndex), event.getY(reasonIndex));

            }
            //out
            //print();
        }catch (Exception|OutOfMemoryError e){
            Logger.log(e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
        }
    }
    public float getX(int pointerID, int historyKey){
        return database[historyKey][pointerID].x;
    }
    public float getY(int pointerID, int historyKey){
        return database[historyKey][pointerID].y;
    }
}
