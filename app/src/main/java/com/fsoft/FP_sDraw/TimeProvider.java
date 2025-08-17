package com.fsoft.FP_sDraw;

import android.os.Bundle;

import com.fsoft.FP_sDraw.common.Logger;

/**
 * занимается учетом времени выполнения тех или иных функций
 * Created by Dr. Failov on 02.01.14.
 */
public class TimeProvider {
    static private final Bundle timestamps = new Bundle();


    static public void start(String func){
        timestamps.putLong(func, System.currentTimeMillis());
        Logger.log("TimeProvider", "Операция " + func + "() запущена.", false);
    }

    static public void finish(String func){
        long start = timestamps.getLong(func, 0);
        if(start == 0)
            Logger.log("TimeProvider", "Операция "+func+" завершена, но запущена не была.", false);
        else
            Logger.log("TimeProvider", "Операция "+func+" завершена за "+(System.currentTimeMillis() - start)+" мс.", false);
    }
}
