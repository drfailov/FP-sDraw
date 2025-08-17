package com.fsoft.FP_sDraw;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.instruments.Instrument;
import com.fsoft.FP_sDraw.menu.MainMenu;
import com.fsoft.FP_sDraw.menu.MenuPopup;
import com.fsoft.FP_sDraw.menu.MenuStrip;
import com.fsoft.FP_sDraw.menu.MyCheckBox;
import com.fsoft.FP_sDraw.menu.Palette;

import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * main activity of the program. activity for canvas *
 * Created by Dr. Failov on 08.06.2014.
 */

public class MainActivity extends Activity {
    public static final int REQUEST_INSERT_FILE_PROGRAM = 43;
    public static final int REQUEST_ENTER_TEXT_PROGRAM = 8378;

    DrawCanvas drawView;
    DrawCore draw;
    public MainMenu mainMenu;
    public Palette bottomPalette = null;
    public MenuStrip bottomToolbar = null;
    public HoverProvider hoverProvider = new HoverProvider();
    //Обработка клавиш
    boolean volumeUpPressed = false;
    boolean volumeDownPressed = false;

    int btndown_times=0;
    long btndown_last_time=0;
    long btnup_last_time=1;
    int press_speed=400;
    int longpress_speed = ViewConfiguration.getLongPressTimeout();//700;
    boolean backKeyUndo = true;
    Timer backKeyUndoTimer = null;
    Handler backKeyUndoHandler = null;
    Toast backKeyUndoToast = null;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Data.init(this);
            TimeProvider.start("start");

            //workaround for cutout areas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                getWindow().setAttributes(layoutParams);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }

            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            if (!(Boolean) Data.get(Data.statusBarBoolean()))
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if ((Boolean) Data.get(Data.keepScreenOnBoolean())) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   //подсветка всегда включена
            }
            if ((Boolean) Data.get(Data.maximumBrightnessBoolean())) {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = 1.0f;
                getWindow().setAttributes(lp);
            }

            //спрятать наэкранные кнопки.
//            if(Build.VERSION.SDK_INT>= 14) {
//                View decorView = getWindow().getDecorView();
//                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
//                decorView.setSystemUiVisibility(uiOptions);
//            }

            //ЗДЕСЬ ОХУЕННО БОЛЬШОЕ КОЛИЧЕСТВО КОСТЫЛЕЙ НА СТРОКУ!!!
            //В Samsung сидят конченные пидорасы.
            //На самсунгах каким-то волшебным образом ЛАГАЕТ SurfaceView!!!!!!!! ЛАГАЕТ, КАРЛ!!!!!!!!!
            //Поэтому реализовано ДВА(!) РАЗНЫХ(!) СПОСОБА ОТРИСОВКИ!
            //Канву я оставил специально ради этого говна по имени Samsung.
            //Чтобы иметь возможность хоть как-то хранить эти разыне обьекты, я объединил их под классом View,
            //А чтобы достать ключевой обьект использовал getTag, чисто потому что он возвращает Object.
            drawView = new DrawCanvas(this);
            draw = drawView.getDrawCore();
            //задать ориентацию пока не поздно
            int current_orientation_canvas = draw.orientationProvider.getScreenOrientation();
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
            }
        }
        catch (Exception|OutOfMemoryError e){
            if(Data.tools != null)
                Logger.log("Error: " + (Tools.getStackTrace(e)));
            else
                Logger.log("Error: " + e);
        }
    }
    @Override protected void onResume() {
        super.onResume();
        try {


            //make layout
            if(drawView.getParent() != null)
                ((ViewGroup)drawView.getParent()).removeView(drawView);
            if(bottomPalette != null && bottomPalette.getParent() != null)
                ((ViewGroup)bottomPalette.getParent()).removeView(bottomPalette);
            if(bottomToolbar != null && bottomToolbar.getParent() != null)
                ((ViewGroup)bottomToolbar.getParent()).removeView(bottomToolbar);

            View mainView = drawView;
            if ((Boolean) Data.get(Data.paletteStripBoolean()) && Data.tools.isFullVersion())
                mainView = addBottomPanel(mainView, getBottomPalette());
            if ((Boolean) Data.get(Data.menuStripBoolean()) && Data.tools.isFullVersion()) {
                mainView = addBottomPanel(mainView, getBottomToolbar());
                refreshBottomToolbar();
            }

            setContentView(mainView);

            if (draw != null)
                draw.refresh();
            menuKeyDownTime = -1;
            backKeyUndo = (Boolean) Data.get(Data.backKeyUndoBoolean());

            if (bottomPalette != null)
                bottomPalette.setBackgroundColor((Integer) Data.get(Data.backgroundColorInt()));
            if (bottomToolbar != null)
                bottomToolbar.setBackgroundColor((Integer) Data.get(Data.backgroundColorInt()));

            if ((Boolean) Data.get(Data.fingerHoverBoolean()))
                hoverProvider.enable();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if((Boolean) Data.get(Data.enableHardwareAcceleration())) {
                    drawView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                }
                else    {
                    drawView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
            }

            draw.redraw();

            processIntent();
        }
        catch (Exception e){
            if(Data.tools != null)
                Logger.log("Error on MainActivity.onResume: " + (Tools.getStackTrace(e)));
            else
                Logger.log("Error on MainActivity.onResume: " + e);
        }
    }
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        if(draw != null && draw.orientationProvider != null)
            draw.orientationProvider.beforeRotate();
        super.onSaveInstanceState(outState);
    }
    @Override protected void onPause() {
        hoverProvider.disable();
        if(mainMenu != null) {
            mainMenu.dismiss();
            mainMenu = null;
        }
        if(draw != null)
            draw.pause();
        super.onPause();
    }
    @Override protected void onDestroy() {
        if(mainMenu != null) {
            mainMenu.dismiss();
            mainMenu = null;
        }
        draw.fuckIt();
        draw = null;
        bottomToolbar = null;
        bottomPalette = null;
        mainMenu = null;
        Logger.fuckIt();
        Data.fuckIt();
        super.onDestroy();
        //System.gc();
        if(isFinishing()) {
            System.runFinalization();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
    long menuKeyDownTime = -1;
    @Override  public boolean dispatchKeyEvent(KeyEvent event) {
        //Logger.log(event.toString());
        if(event == null)
            return false;
        try {
            boolean onlySPen = (Boolean)Data.get(Data.sPenOnlyUiBoolean());
            String devName = "";
            String sPenName = "sec_e-pen";
            try {
                if (Build.VERSION.SDK_INT >= 9) {
                    InputDevice inputDevice = event.getDevice();
                    if (inputDevice != null) {
                        String name = inputDevice.getName();
                        if (name != null)
                            devName = name;
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
                Logger.log("MainActivity.dispatchKeyEvent", "Can't get device name: " + e.getMessage(), false);
            }


            boolean drawResult = false;
            if (draw != null)
                drawResult = draw.processKeyEvent(event);
            boolean itsResult = false;


            //Обработка клавиши МЕНЮ
            if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && (!onlySPen || devName.equals(sPenName))) {
                if(event.getAction() == KeyEvent.ACTION_DOWN){
                    if(menuKeyDownTime == -1)
                        menuKeyDownTime = System.currentTimeMillis();
                    else{
                        long now = System.currentTimeMillis();
                        long dif = now - menuKeyDownTime;
                        if(dif > longpress_speed) {
                            Tools.vibrate(drawView);
                            startActivity(new Intent(this, SettingsScreen.class));
                        }
                    }
                }
                else {
                    long now = System.currentTimeMillis();
                    long dif = now - menuKeyDownTime;
                    if(dif < longpress_speed) {
                        openMainMenu();
                    }
                    menuKeyDownTime = -1;
                }
                itsResult = true;
            }

            //Обработка НАЖАТИЯ клавиш громкости
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)
                    volumeDownPressed = true;
                if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)
                    volumeUpPressed = true;
            }

            //Обработка ОТПУСКАНИЯ клавиш громкости
            else if (event.getAction() == KeyEvent.ACTION_UP) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                    //Если обе были нажаты - значит открыть меню
                    if (volumeDownPressed && volumeUpPressed) {
                        openMainMenu();
                    }
                    if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)
                        volumeDownPressed = false;
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)
                        volumeUpPressed = false;
                }
            }

            //Обработка клавиши НАЗАД
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if(onlySPen && !devName.equals(sPenName))
                    return true;
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (btnup_last_time > btndown_last_time)  //подняли палец позже чем поставили последний раз
                    {
                        btndown_times = 1;
                        btndown_last_time = System.currentTimeMillis();
                    }
                    long now = System.currentTimeMillis();
                    long difference = now - btndown_last_time;
                    if (difference > longpress_speed) {
                        if (btndown_times == 1) {
                            Tools.vibrate(drawView);
                            draw.clear();
                            btndown_times = -1;//для того чтобы после очистки не срабатывала отмена
                        }
                    }
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    long now = System.currentTimeMillis();
                    long difference = now - btnup_last_time;
                    if (difference < press_speed)
                        btndown_times++;
                    else {
                        if (backKeyUndo && btndown_times == 1) {//для того чтобы отмена не срабатывала после очистки
                            backKeyUndoTimer = new Timer();
                            backKeyUndoHandler = new Handler();
                            backKeyUndoToast = Toast.makeText(this, Data.tools.getResource(R.string.menuUndo) + "...", Toast.LENGTH_SHORT);
                            backKeyUndoToast.setGravity(Gravity.BOTTOM, 0, 0);
                            backKeyUndoToast.show();
                            backKeyUndoTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if(backKeyUndoHandler == null)
                                        return;
                                    backKeyUndoHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                if (backKeyUndoToast != null)
                                                    backKeyUndoToast.cancel();
                                                if (draw.undoProvider != null)
                                                    draw.undoProvider.undo();
                                            }
                                            catch (Throwable e) {
                                                Logger.log("Error in Back Key Undo(...): " + Tools.getStackTrace(e));
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            }, press_speed);
                        }
                        btndown_times = 1;
                    }
                    btnup_last_time = System.currentTimeMillis();
                    if (btndown_times >= 2) {
                        if (backKeyUndoTimer != null)
                            backKeyUndoTimer.cancel();
                        if (backKeyUndoToast != null)
                            backKeyUndoToast.cancel();
                        this.finish();
                    } else if (Data.isTutor("exit", 5))
                        Logger.show(Data.tools.getResource(R.string.tutorExit));
                    return true;
                }
                return true;
            }
            return drawResult || itsResult || super.dispatchKeyEvent(event);
        }catch (Exception|OutOfMemoryError e){
            Logger.log("Error: " + (Tools.getStackTrace(e)));
        }
        return super.dispatchKeyEvent(event);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        try{
            if(requestCode== REQUEST_ENTER_TEXT_PROGRAM && resultCode == RESULT_OK){
                if(draw != null && draw.text != null && imageReturnedIntent != null && imageReturnedIntent.hasExtra("text"))
                    draw.text.initText(imageReturnedIntent.getStringExtra("text"));
            }
            if(requestCode== REQUEST_INSERT_FILE_PROGRAM && resultCode == RESULT_OK) {
                Uri selectedImage = imageReturnedIntent.getData();
                Logger.log("URI = " + selectedImage);

                if (selectedImage.toString().startsWith("content://com.google.android.apps.photos.content")
                        || selectedImage.toString().startsWith("content://com.google.android.gallery3d.provider")){
                    Logger.log("CLOUD, MOTHERFUCKER!");
                    openFileInserter(selectedImage);
                }
                else {
                    try {
                        openFileInserter(selectedImage);
                    }
                    catch (Exception e){
                        Logger.log("Unsupported!");
                        Logger.show(Data.tools.getResource(R.string.insertUnsupported));
                    }
                }
            }
        }catch (Exception e){
            Logger.log("Где-то в sDraw.onActivityResult произошла ошибка ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Где-то в sDraw.onActivityResult Недостаточно памяти: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Logger.log("ON LOW MEMORY CALLED");
    }

    void processIntent(){
        try {
            Intent intent = getIntent();
            Logger.log("NEW INTENT = " + intent);
            Bundle bundle = intent.getExtras();
            Logger.log("Bundle = " + bundle);
            String streamKey = "android.intent.extra.STREAM";
            if (bundle != null && bundle.containsKey(streamKey)) {
                Uri selectedImage = (Uri)bundle.get(streamKey);
                Logger.log("Uri = " + selectedImage);
                if(selectedImage == null)
                    return;
                getIntent().replaceExtras(new Bundle()); //remove extras to prevent re-running


                insertFile(selectedImage);
            }
            else
                Logger.log("Bundle don't have an stream");
        }
        catch (Exception e){
            Logger.log("Error: " + Tools.getStackTrace(e));
            Logger.log("No image received");
        }
    }
    void insertFile(final Uri selectedImage){
                draw.schedule(() -> {
                    if(Build.VERSION.SDK_INT < 33) {
                        requestFileReadWritePermission(
                                () -> {
                                    try {
                                        openFileInserter(selectedImage);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Logger.log("MainActivity.processIntent", "Ошибка открытия экрана вставки: " + e.getMessage(), false);
                                    }
                                },
                                () -> Toast.makeText(MainActivity.this, R.string.permissionNeededFileAccess, Toast.LENGTH_SHORT).show()
                        );
                    }
                    else{
                        try {
                            openFileInserter(selectedImage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Logger.log("MainActivity.processIntent", "Ошибка открытия экрана вставки: " + e.getMessage(), false);
                        }
                    }
                });

    }
    View addBottomPanel(View original, View bottom){
        LinearLayout linearLayout = new LinearLayout(original.getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        original.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        linearLayout.addView(original);
        bottom.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.addView(bottom);
        return linearLayout;
    }
    View getBottomPalette(){
        Palette.ColorChecker checker = color -> (Integer)Data.get(Data.brushColorInt()) == color;
        Palette.ColorApplier applier = (color, colorView, colorPositionIndex) -> {
            Data.save(color, Data.brushColorInt());
            draw.refresh();
        };
        Palette.ColorApplier longApplier = (color, colorView, colorPositionIndex) -> {
            Data.save(color, Data.backgroundColorInt());
            draw.refresh();
        };
        bottomPalette = new Palette(this, Data.getPaletteBrush(), 1, applier, checker, longApplier);
        bottomPalette.setBackgroundColor((Integer)Data.get(Data.backgroundColorInt()));
        return bottomPalette;
    }
    public void refreshBottomToolbar(){
        if (bottomToolbar != null)
        {
            try {
                //Logger.log("refreshBottomToolbar()");
                int bottonsSize = (int) Data.store().DPI / 4;
                bottomToolbar.clear();
                ArrayList<String> menuStripList = Data.getMenuStripList();
                for(String string:menuStripList) {
                    switch (string) {
                        case "menu":  //
                            MenuStrip.MenuStripElement mse = bottomToolbar.addButton(R.drawable.menu_menu, view -> openMainMenu(), () -> false, bottonsSize, "Show Menu");
                            mse.setOnLongClickListener(getOpenSettingsLongClickListener());
                            break;
                        case "settings":  //SETTINGS
                            bottomToolbar.addButton(R.drawable.menu_settings, view -> {
                                startActivity(new Intent(this, SettingsScreen.class));
                                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
                            }, () -> false, bottonsSize, Data.tools.getResource(R.string.menuSettings));
                            break;
                        case "undo":  //UNDO
                            bottomToolbar.addButton(R.drawable.menu_undo, view -> draw.undoProvider.undo(), () -> false, bottonsSize, Data.tools.getResource(R.string.menuUndo));
                            break;
                        case "redo": //REDO
                            bottomToolbar.addButton(R.drawable.menu_redo, view -> draw.undoProvider.redo(), () -> false, bottonsSize, Data.tools.getResource(R.string.menuRedo));
                            break;
                        case "clear": //CLEAR
                            bottomToolbar.addButton(R.drawable.menu_clear, view -> draw.clear(), () -> false, bottonsSize, Data.tools.getResource(R.string.menuClear));
                            break;
                        default:
                            for (Instrument instrument : draw.instruments) {
                                if (instrument.isVisibleToUser() && instrument.getName().equals(string)) {
                                    bottomToolbar.addButton(
                                            instrument.getImageResourceID(),
                                            instrument.getOnClickListener(),
                                            instrument::isActive,
                                            bottonsSize,
                                            instrument.getVisibleName()
                                    );
                                }
                            }
                            break;
                    }
                }

                bottomToolbar.addButton(R.drawable.ic_list, view -> openBottomBarSelector(), () -> false, bottonsSize, Data.tools.getResource(R.string.edit));
                bottomToolbar.refresh();
            }
            catch (Exception e){
                Logger.log(e);
            }
        }
    }
    View getBottomToolbar(){
        bottomToolbar = new MenuStrip(this, (Integer)Data.get(Data.backgroundColorInt()));
        return bottomToolbar;
    }
    private void openBottomBarSelector(){
        try{
            final MenuPopup menuPopup = new MenuPopup(draw.context);
            menuPopup.setHeader(Data.tools.getResource(R.string.selectMenuStripContent));

            //form list
            final ArrayList<MyCheckBox> checkBoxes = new ArrayList<>();
            ArrayList<String> content = Data.getMenuStripList();
            {//menu
                String tagString = "menu";
                MyCheckBox checkBox = menuPopup.addCheckbox(getString(R.string.menu), R.drawable.menu_menu, content.contains(tagString));
                checkBox.setTag(tagString);
                checkBoxes.add(checkBox);
            }
            {//settings
                String tagString = "settings";
                MyCheckBox checkBox = menuPopup.addCheckbox(getString(R.string.menuSettings), R.drawable.menu_settings, content.contains(tagString));
                checkBox.setTag(tagString);
                checkBoxes.add(checkBox);
            }
            {//undo
                String tagString = "undo";
                MyCheckBox checkBox = menuPopup.addCheckbox(getString(R.string.menuUndo), R.drawable.menu_undo, content.contains(tagString));
                checkBox.setTag(tagString);
                checkBoxes.add(checkBox);
            }
            {//Redo
                String tagString = "redo";
                MyCheckBox checkBox = menuPopup.addCheckbox(getString(R.string.menuRedo), R.drawable.menu_redo, content.contains(tagString));
                checkBox.setTag(tagString);
                checkBoxes.add(checkBox);
            }
            {//Clear
                String tagString = "clear";
                MyCheckBox checkBox = menuPopup.addCheckbox(getString(R.string.menuClear), R.drawable.menu_clear, content.contains(tagString));
                checkBox.setTag(tagString);
                checkBoxes.add(checkBox);
            }

            for(Instrument instrument: draw.instruments){
                if(instrument.isVisibleToUser()) {
                    MyCheckBox checkBox = menuPopup.addCheckbox(instrument.getVisibleName(), instrument.getImageResourceID(), content.contains(instrument.getName()));
                    checkBox.setTag(instrument.getName());
                    checkBoxes.add(checkBox);
                }
            }


            menuPopup.addLittleText(draw.context.getString(R.string.selectMenuStripContentHint));
            menuPopup.addClassicButton(Data.tools.getResource(R.string.apply), v -> {
                ArrayList<String> result = new ArrayList<>();
                for (MyCheckBox checkBox:checkBoxes) {
                    if(checkBox.isChecked()) {
                        if (checkBox.getTag() != null && checkBox.getTag() instanceof String) {
                            result.add((String) checkBox.getTag());
                        }
                    }
                }
                Data.saveMenuStripList(result);
                menuPopup.cancel();
                refreshBottomToolbar();
            }, false);

            menuPopup.addSpacer();
            menuPopup.show();
        }catch (Exception | OutOfMemoryError e){
            Logger.log(e);
        }
    }
    public void openFileInserter(Uri selectedImage){
        Logger.log("opening ImageInserter: " + selectedImage);
        ImageInserter.initCanvasBitmap = draw.bitmap;
        ImageInserter.initListener = rectOfUndo -> {
            if(draw.undoAreaCalculator != null){
                draw.undoAreaCalculator.reset();
                draw.undoAreaCalculator.add(rectOfUndo.left, rectOfUndo.top, 0);
                draw.undoAreaCalculator.add(rectOfUndo.right, rectOfUndo.bottom, 0);
            }
            draw.undoProvider.apply(rectOfUndo);
            draw.undoProvider.prepare();
            draw.lastChangeToBitmap = System.currentTimeMillis();
            draw.redraw();
        };
        Intent data = new Intent();
        //data.putE
        data.putExtra("fileUri", selectedImage.toString());
        //data.putExtra("file", filePath);
        data.setClass(this, ImageInserter.class);
        startActivity(data);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
    }
    public void openMainMenu(){
        mainMenu = new MainMenu(this, draw);
        mainMenu.showMenu();
    }
    private View.OnLongClickListener getOpenSettingsLongClickListener(){
        return view -> {
            try {
                Tools.vibrate(draw.view);
                draw.context.startActivity(new Intent(draw.context, SettingsScreen.class));
            } catch (Throwable e) {
                Logger.log(e);
            }
            return false;
        };
    }
    // Get a MemoryInfo object for the device's current memory status.
    public ActivityManager.MemoryInfo getAvailableMemory() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if(activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo;
        }
        return null;
    }

    private Runnable pendingPermissionGrantedRunnable = null;
    private Runnable pendingPermissionDeniedRunnable = null;
    public static final int REQUEST_PERMISSION_FILE_WRITE = 45;
    private boolean selfPermissionGranted(String permission) {
        // For Android < Android M, self permissions are always granted.
        boolean result = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // targetSdkVersion >= Android M, we can
                // use Context#checkSelfPermission
                result = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
        return result;
    }
    public void requestFileReadWritePermission(Runnable granted, Runnable denied){
        if(selfPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            if(granted != null)
                granted.run();
        }
        else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingPermissionDeniedRunnable = denied;
                pendingPermissionGrantedRunnable = granted;
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_FILE_WRITE);
            }
            else{
                if(denied != null)
                    denied.run();
            }
        }
        //requestPermissions();
        //int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_FILE_WRITE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPermissionGrantedRunnable.run();
            } else {
                pendingPermissionDeniedRunnable.run();
            }
        }
    }

static class HoverProvider{
        private final File samsungFile = new File("/sys/class/sec/tsp/cmd");
        private final String[] samsungEnableCommands = {"echo \"hover_enable,1\">/sys/class/sec/tsp/cmd"};
        private final String[] samsungDisableCommands = {"su echo \"hover_enable,0\">/sys/class/sec/tsp/cmd"};
        private boolean samsung_enabled = false;

        public void enable(){
            if(samsungFile.isFile()) {
                Logger.log("TSP Hovering is available. Trying to enable...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                            Logger.log("TSP Hovering enabling...");
                            if (samsung_enabled = execute(samsungEnableCommands)){
                                Logger.log("TSP Hovering enabled successfully.");
                            }
                            else {
                                Logger.log("TSP Hovering is NOT enabled due to ERROR.");
                            }
                    }
                }).start();
            }
        }

        public void disable(){
            if(isEnabled()) {
                Logger.log("TSP Hovering enabled. Disabling...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Logger.log("TSP Hovering. Trying to disable...");
                        if (execute(samsungDisableCommands)) {
                            Logger.log("TSP Hovering disabled successfully.");
                        } else {
                            Logger.log("TSP Hovering is NOT disabled due to ERROR.");
                        }
                    }
                }).start();
            }
        }

        public boolean isEnabled(){
            return samsung_enabled;
        }

        private boolean execute(String[] cmds){
            try {
                StringBuilder s = new StringBuilder();
                for(String st:cmds) s.append(st).append(";");
                Logger.log("MainActivity.HoverProvider.execute","Executing commands "+s+"...", false);
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());
                for (String tmpCmd : cmds) {
                    os.writeBytes(tmpCmd+"\n");
                }
                os.writeBytes("exit\n");
                os.flush();
                return true;
            } catch (Exception e) {
                Logger.log("Error executing command: " + e + ".");
                e.printStackTrace();
                return false;
            }
        }
    }
}

