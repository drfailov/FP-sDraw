package com.fsoft.FP_sDraw;

import android.app.ActionBar;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.MenuPopup;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Dr. Failov
 * Date: 08.03.13
 * Time: 22:26
 */
public class FileSelector extends FragmentActivity {
    public static ImageGridFragment.OnSelect initAction = null;
    public static File[] initPaths = null;
    public static File[] initAuxPaths = null;
    public static String[] initNames = null;

    ViewPager pager;
    File[] paths = null;
    File[] auxPaths = null;
    String[] names = null;
    ImageGridFragment.OnSelect onSelect = null;
    int backgroundColor = MenuPopup.menuBackgroundColor;

    @Override public void onCreate(Bundle bundle)  {
        try{
            super.onCreate(bundle);

            //задать ориентацию пока не поздно
            int current_orientation_canvas = Tools.getScreenOrientation(this);
            int needed_orientation_canvas = (Integer) Data.get(Data.orientationCanvasInt());
            if (needed_orientation_canvas == DrawCore.OrientationProvider.ORIENTATION_AUTO)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            else if (needed_orientation_canvas == DrawCore.OrientationProvider.ORIENTATION_VERTICAL)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else if (needed_orientation_canvas == DrawCore.OrientationProvider.ORIENTATION_HORIZONTAL)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            //если текущая ориентация не соответствует заданой в настройках - программе гроизт перезагрузка.
            // А значит, грузить текущую сессию дальше нет смысла
            if (needed_orientation_canvas != DrawCore.OrientationProvider.ORIENTATION_AUTO && current_orientation_canvas != needed_orientation_canvas) {
                Logger.log("Текущая ориентация холста не совпадает с требуемой - я ухожу.");
                return;
            }


            if(initAction == null)
                Logger.log("There is no action received! Did you forgot to fill static fields before calling???");
            onSelect = initAction;
            initAction = null;
            paths = initPaths;
            initPaths = null;
            auxPaths = initAuxPaths;
            initAuxPaths = null;
            names = initNames;
            initNames = null;

            //back button
            if(Build.VERSION.SDK_INT > 11) {
                ActionBar actionBar = getActionBar();
                if(actionBar != null)
                    actionBar.setDisplayHomeAsUpEnabled(true);
            }

            Logger.log("sDraw.onCreate", "Инициализация: FileSelector...", false);
            //requestWindowFeature(Window.FEATURE_NO_TITLE);  //убрать панель уведомлений
            setContentView(R.layout.view_pager_top_titlestrip);
            pager = findViewById(R.id.pager);
            PagerTitleStrip titleStrip = findViewById(R.id.pagerTitleStrip);
            titleStrip.setTextColor(Color.WHITE);
            pager.setBackgroundColor(backgroundColor);

            PagerAdapter pagerAdapter = new FileSelectorPagerAdapter(getSupportFragmentManager());
            pager.setAdapter(pagerAdapter);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(Color.parseColor("#273238"));
                getWindow().setNavigationBarColor(Color.parseColor("#273238"));
            }
        }catch (Exception | OutOfMemoryError e) {
            Logger.log("FileSelector.onCreate", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        if(paths != null && auxPaths != null && names != null && onSelect != null) {
            initAuxPaths = auxPaths;
            initPaths = paths;
            initAction = onSelect;
            initNames = names;
        }
        super.onSaveInstanceState(outState);
    }
    @Override protected void onResume(){
        super.onResume();
    }
    @Override protected void onDestroy(){
        super.onDestroy();
        pager = null;
        paths = null;
        names = null;
        onSelect = null;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId()==android.R.id.home) {
            finish();
        }
        return false;
    }

    private class FileSelectorPagerAdapter extends FragmentPagerAdapter {
        ImageGridFragment[] fragments = null;

        public FileSelectorPagerAdapter(FragmentManager fm) {
            super(fm);
            fragments = new ImageGridFragment[paths.length];
        }
        @Override public CharSequence getPageTitle(int position) {
            if(names == null)
                return " error ";
            if(position < 0 || position >= names.length)
                return " error ";
            return names[position];
        }
        @Override public Fragment getItem(int position) {
            if(paths == null)
                return new Fragment();
            if(position < 0 || position >= paths.length)
                return new Fragment();
            if(fragments[position] == null) {
                fragments[position] = new ImageGridFragment(paths[position], auxPaths[position], onSelect);
            }
            return fragments[position];
        }
        @Override public int getCount() {
            return paths.length;
        }
    }
}
