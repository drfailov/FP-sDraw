package com.fsoft.FP_sDraw.common;

import android.app.Activity;
import android.util.DisplayMetrics;

/**
 * Хранит общеполезные данные для программы
 * Created by Dr. Failov on 02.01.2015.
 */
public class Store{
    //Этот класс создан для хранение общеполезной информации
    public Activity activity=null;
    public int displayHeight = 1080;//default values to not break anything in case of device failure
    public int displayWidth = 1920;
    public float DPI = 320;

    Store (Activity _act){
        try {
            activity = _act;
            //достать размер дисплея
            DisplayMetrics dm = new DisplayMetrics();                                //получить размер дисплея
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            displayWidth = dm.widthPixels;
            displayHeight = dm.heightPixels;
            DPI = dm.densityDpi;
        }
        catch (Exception e){
            e.printStackTrace();
            Logger.log("Store", "Ошибка конструктора: " + e.getMessage(), false);
        }
    }
}
