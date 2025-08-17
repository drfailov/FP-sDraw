package com.fsoft.FP_sDraw;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.fsoft.FP_sDraw.common.EasterEggView;

/**
 *
 * Created by Dr. Failov on 03.05.14.
 */
public class EasterEggScreen extends Activity {
    @Override    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);  //убрать панель
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   //подсветка всегда включена
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new EasterEggView(this, R.drawable.bird, false));
    }
}
