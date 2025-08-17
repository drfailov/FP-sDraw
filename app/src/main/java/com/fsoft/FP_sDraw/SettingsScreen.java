package com.fsoft.FP_sDraw;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.MyImageView;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.common.ViewAdapter;
import com.fsoft.FP_sDraw.menu.AnimatedButton;
import com.fsoft.FP_sDraw.menu.AnimationAdapter;
import com.fsoft.FP_sDraw.menu.ColorPickerDialog;
import com.fsoft.FP_sDraw.menu.MainMenu;
import com.fsoft.FP_sDraw.menu.MenuPopup;
import com.fsoft.FP_sDraw.menu.MenuStrip;
import com.fsoft.FP_sDraw.menu.MyCheckBox;
import com.fsoft.FP_sDraw.menu.Palette;
import com.fsoft.FP_sDraw.menu.SmoothSeekBar;

import java.util.ArrayList;

/**
 * класс отображения меню
 * Created with IntelliJ IDEA.
 * User: Dr. Failov
 * Date: 23.01.13
 * Time: 23:26
 */
public class SettingsScreen extends Activity {

    private final int CODE_LANGUAGE_CHANGED = 333;
    Activity context = this;
    EasterEggsProvider easterEggsProvider = null;
    Preview preview = null;
    FrameLayout brushPaletteFrame = null;
    FrameLayout backgroundPaletteFrame = null;
    ListView listView = null;
    int settings_header_text_size = 16;
    int backgroundColor = MenuPopup.menuBackgroundColor;
    int DPI = (int)Data.store().DPI;
    ViewGroup.LayoutParams layout_params_header = null;
    //base elements
    @SuppressLint("SourceLockedOrientationActivity")
    @Override public void onCreate(Bundle b){
    /*
    Конструктор. он тут решает.
     */
        try{

            //INIT
            super.onCreate(b);
            Logger.log("SettingsScreen.onCreate", "Инициализация...", false);
            setTitle(R.string.menuSettings);
            layout_params_header = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            easterEggsProvider = new EasterEggsProvider();
            easterEggsProvider.whenStarted();
            ViewAdapter viewAdapter = new ViewAdapter(context);

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

            //back button
            if(Build.VERSION.SDK_INT > 11) { //it's not used but I leave it here for case if style fails for some reason
                ActionBar actionBar = getActionBar();
                if(actionBar != null)
                    actionBar.setDisplayHomeAsUpEnabled(true);
            }

            getBackButtonBlock(viewAdapter);

            //FILL
            getPaletteBrushBlock(viewAdapter);
            getPaletteBackgroundBlock(viewAdapter);

            getPreviewBlock(viewAdapter);
            getBrushSizeBlock(viewAdapter);
            //getEraserSizeBlock(viewAdapter);
            //getMosaicSizeBlock(viewAdapter);
            getGridSizeBlock(viewAdapter);
            getManageMethodBlock(viewAdapter);
            getImageProcessingBlock(viewAdapter);
            getInterfaceBlock(viewAdapter);
            getOrientationBlock(viewAdapter);
            getOtherSettingsBlock(viewAdapter);

            //SHOW
            listView = new ListView(context);
            listView.setCacheColorHint(backgroundColor);
            if(Build.VERSION.SDK_INT >= 16)
                listView.setScrollBarSize(Tools.dp(3));
            listView.setAdapter(viewAdapter);
            listView.setBackgroundColor(backgroundColor);//Color.parseColor("#111119"));  //если оставить, то будет мерцать

            setContentView(listView);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.getWindow().setStatusBarColor(backgroundColor);
                context.getWindow().setNavigationBarColor(backgroundColor);
            }
        }catch (Exception e){
            Logger.log("Где-то в SettingsScreen.onCreate произошла ошибка ", e +"\n-Ну и фиг с ней!" + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Где-то в SettingsScreen.onCreate Недостаточно памяти: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }
    @Override public void onResume() {
    /*
    Срабатывает при отображении окна на экране. Инициирует обновление содержимого
     */
        try{
            super.onResume();
            //Logger.log("SettingsScreen.onCreate", "OnResume SettinsScreen...", false);
            context = this;
            //update();
        }catch (Exception e){
            Logger.log("Где-то в SettingsScreen.onResume произошла ошибка ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Где-то в SettingsScreen.onResume Недостаточно памяти: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }
    @Override public void onPause()  {
    /*
    Срабатывает при закрытии окна. Инициирует сохранение данных
    */
        try{
            super.onPause();
            //Logger.log("SettingsScreen.apply", "Запись данных в память...", false);
            //GlobalData.save_settings();
            //GlobalData.drawOld.paint.setAntiAlias(GlobalData.antialiasing);
            //Logger.log("SettingsScreen.apply", "Переход на холст...", false);
//            if(GlobalData.drawOld.isEmpty())
//                GlobalData.drawOld.clear();
//            GlobalData.drawOld.setMode(GlobalData.drawOld.mode); //обновить холст
        }catch (Exception e){
            Logger.log("SettingsScreen.onPause Exception: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("SettingsScreen.onPause OutOfMemoryError: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }
    @Override public void onDestroy(){
        super.onDestroy();
        try {
            easterEggsProvider.whenFinished();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(!(Boolean)Data.get(Data.volumeButtonsBoolean()))  //если обработка клавиш отключена - не обрабатываем
            return super.dispatchKeyEvent(event);
        /*перехватывает клавиши громкости*/
        return event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN || super.dispatchKeyEvent(event);
    }
    @Override public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId()==android.R.id.home) {
            finish();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.log("onActivityResult");
        if(requestCode == CODE_LANGUAGE_CHANGED) {
            finish();
        }
    }

    //elementary parts
    public TextView getTitle(String text){
        TextView textView=new TextView(context);
        textView.setText(text.toUpperCase());
        textView.setTextColor(MainMenu.menuAccentColor);
        //textView.setBackgroundColor(backgroundColor);
        textView.setGravity(Gravity.START);
        textView.setTextSize(settings_header_text_size);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(lp);
        textView.setPadding(Tools.dp(15), Tools.dp(10), Tools.dp(15), 0);
        return textView;
    }
    public TextView getSubTitle(String text){
        TextView textView=new TextView(context);
        textView.setText(text);
        textView.setTextColor(Color.WHITE);
        textView.setGravity(Gravity.START);
        textView.setTextSize(18);
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(lp);
        textView.setPadding(Tools.dp(15), 0, Tools.dp(15), 0);
        return textView;
    }
    public TextView getHint(String text){
        TextView hintView = new TextView(context);
        hintView.setGravity(Gravity.START);
        hintView.setTextColor(MainMenu.menuHintColor);
        //hintView.getPaint().setAntiAlias(false);
        //hintView.setBackgroundColor(Color.BLACK);
        hintView.setPadding(Tools.dp(15), 0, Tools.dp(15), Tools.dp(5));
        hintView.setTextSize((int) (settings_header_text_size * 0.8));
        //hintView.setLines(10);
        //hintView.setTypeface(Typeface.DEFAULT);
        //hintView.setBackgroundColor(backgroundColor);
        hintView.setText(/*String.valueOf*/(text));
        return hintView;
    }
    public View getSlider(String name, int min, int max, int dflt, SeekBar.OnSeekBarChangeListener listener){
        LinearLayout linearLayout = new LinearLayout(context);
        SeekBar seekBar = new SmoothSeekBar(context);
        TextView textView = new TextView(context);

        textView.setTextColor(Color.WHITE);
        textView.setGravity(Gravity.END);
        //textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setPadding(Tools.dp(15), 0, Tools.dp(15), Tools.dp(0));
        textView.setText(String.valueOf(dflt));

        seekBar.setPadding(Tools.dp(15), 0, Tools.dp(15), 0);
        seekBar.setMax(max - min);
        seekBar.setProgress(dflt - min);
        seekBar.setTag(R.drawable.menu_text, textView);    //да, будем АйДи страниц приветствия использовать как уникальное значение для передачи тэгов. Офигенно же!
        seekBar.setTag(R.drawable.menu_line, listener);
        seekBar.setTag(R.drawable.menu_insert, min);
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Tools.dp(25)));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int i, boolean b) {  //обьект, индекс, пользователь ли выполнил действие
                //init
                SeekBar.OnSeekBarChangeListener toDo = (SeekBar.OnSeekBarChangeListener)seekBar.getTag(R.drawable.menu_line);
                TextView indicator = (TextView)seekBar.getTag(R.drawable.menu_text);
                Integer min = (Integer)seekBar.getTag(R.drawable.menu_insert);
                //check
                if(indicator != null)
                    indicator.setText(String.valueOf(i+min));
                if(toDo != null)
                    toDo.onProgressChanged(seekBar, i + min, b);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                SeekBar.OnSeekBarChangeListener toDo = (SeekBar.OnSeekBarChangeListener)seekBar.getTag(R.drawable.menu_line);
                if(toDo != null)
                    toDo.onStartTrackingTouch(seekBar);
            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                SeekBar.OnSeekBarChangeListener toDo = (SeekBar.OnSeekBarChangeListener)seekBar.getTag(R.drawable.menu_line);
                if(toDo != null)
                    toDo.onStopTrackingTouch(seekBar);
            }
        });

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView nameView = getTitle(name);
        //linearLayout.setBackgroundColor(backgroundColor);
        LinearLayout horizontalLayout = new LinearLayout(this);
        horizontalLayout.setGravity(Gravity.BOTTOM);
        horizontalLayout.addView(nameView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        horizontalLayout.addView(textView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));

        linearLayout.addView(horizontalLayout);
        linearLayout.addView(seekBar);

        return linearLayout;
    }
    public View getCheckbox(String name, boolean checked, CheckBox.OnClickListener listener, int image, boolean locked){
        MyCheckBox checkBox = new MyCheckBox(context, locked);
        checkBox.setText(name);
        checkBox.setChecked(checked);
        checkBox.setOnClickListener(listener);
        checkBox.setPadding(0, DPI / 32, 0, DPI / 16);
        checkBox.setImage(image);
        if(locked)
            checkBox.setEnabled(false);
        return checkBox;
    }
    public View getRadioGroup2(String[] names, String[] hints, int[] images, View.OnClickListener[] listeners, MenuStrip.ActiveChecker[] checked){
        //check
        if(names.length != hints.length || names.length != listeners.length || checked.length != names.length)
            return getTitle("Ошибка в наборе аргументов");
        if(names.length < 1)
            return getTitle("Пусто в аргументах");

        TextView nameView = getSubTitle("");
        TextView hintView = getHint("");

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        MenuStrip menuStrip = new MenuStrip(this, backgroundColor);
        menuStrip.setPadding(Tools.dp(15), 0, Tools.dp(15), 0);
        menuStrip.linearLayout.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        for(int i=0; i<names.length; i++){
            int finalI = i;
            View.OnClickListener onClickListener = view -> {
                nameView.setText(names[finalI]);
                hintView.setText(hints[finalI]);
                listeners[finalI].onClick(view);
            };
            menuStrip.addButton(images[i], onClickListener, checked[i], Tools.dp(50), names[i]);
            if(checked[i].isActive()){
                nameView.setText(names[finalI]);
                hintView.setText(hints[finalI]);
            }
        }
        linearLayout.addView(menuStrip);

        linearLayout.addView(nameView);
        linearLayout.addView(hintView);


        return linearLayout;
    }
    public View getButton (String text, View.OnClickListener listener, View.OnLongClickListener longListener){
        //create button

        TextView button = new AnimatedButton(context);
        button.setOnClickListener(listener);
        button.setOnLongClickListener(longListener);
        button.setText(text);
        //padding must be set by button correctly
        //padding must be set by button correctly
        //button.setPadding(Tools.dp(15), Tools.dp(8), Tools.dp(15), Tools.dp(10));
        button.setGravity(Gravity.CENTER);

        //create layout
        LinearLayout linearLayout = new LinearLayout(context);
        AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT, AbsListView.LayoutParams.WRAP_CONTENT);
        linearLayout.setPadding(Tools.dp(15), Tools.dp(5), Tools.dp(15), Tools.dp(5));
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setBackgroundColor(backgroundColor);
        linearLayout.setGravity(Gravity.END);
        linearLayout.addView(button);

        return linearLayout;
    }

    //blocks
    private void getBackButtonBlock(ViewAdapter viewAdapter){
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        };
        int textId = R.string.back;
        int imageResId = R.drawable.ic_back;
        getButtonBlock(viewAdapter, listener, textId, 0, imageResId);
    }
    private void getButtonBlock(ViewAdapter viewAdapter, View.OnClickListener listener, int textResId, int hintResId, int imageResId){

        MainMenu.Cell cell = new MainMenu.Cell(this);
        //cell.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        //fill image
        MyImageView image = new MyImageView(context);
        image.setDrawable(imageResId);
        LinearLayout.LayoutParams lpi = new LinearLayout.LayoutParams(DPI / 3, DPI / 5, 0);
        lpi.gravity = Gravity.CENTER_VERTICAL;
        image.setLayoutParams(lpi);
        image.setPadding(0, 0, Tools.dp(10), 0);
        cell.addView(image);

        //fill text
        TextView text = new TextView(context);
        //text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        //text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        //lp.setMargins(Tools.dp(15), 0,0,0);
        text.setLayoutParams(lp);
        text.setTextColor(MainMenu.menuTextColor);
        text.setText(textResId);
        text.setTextSize(18);
        //text.setGravity(Gravity.START);
        if(hintResId == 0) {
            cell.addView(text);
        }
        else {
            LinearLayout ll = new LinearLayout(context);
            ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setGravity(Gravity.START);
            ll.addView(text);


            TextView hintView = new TextView(context);
            //hintView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            hintView.setGravity(Gravity.START);
            hintView.setTextColor(MainMenu.menuHintColor);
            //hintView.setBackgroundColor(Color.BLACK);
            //hintView.setPadding(DPI / 8, 0, DPI / 16, DPI / 32);
            hintView.setTextSize((int) (settings_header_text_size * 0.8));
            //hintView.setLines(10);
            //hintView.setTypeface(Typeface.DEFAULT);
            //hintView.setBackgroundColor(backgroundColor);
            hintView.setText(getString(hintResId));
            ll.addView(hintView);
            cell.addView(ll);
        }

        cell.setBackgroundColor(MainMenu.menuBackgroundColor);
        cell.setPadding(Tools.dp(10),Tools.dp(10),Tools.dp(10),Tools.dp(10));
        cell.setOnClickListener(listener);

        viewAdapter.addView(cell);
    }
    private void getPreviewBlock(ViewAdapter viewAdapter) {
        FrameLayout frameLayout = new FrameLayout(this);
        preview = new Preview(context);
        frameLayout.addView(preview, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        frameLayout.setPadding(Tools.dp(15), 0, Tools.dp(15), 0);


        frameLayout.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Tools.dp(120)));
        viewAdapter.addView(frameLayout);
    }
    public void refreshPaletteBrushBlock(){
        int height = 3;

        Palette palette = new Palette(this, Data.getPaletteBrush(), height, getPaletteBrushApplier(), getPaletteBrushChecker(), getPaletteBrushLongListener());
        palette.setBackgroundColor(backgroundColor);
        //button edit
        TextView palette_brush_edit_button=new AnimatedButton(context);
        palette_brush_edit_button.setText(Data.tools.getResource(R.string.settingsBrushcolorOther));
        LinearLayout.LayoutParams layout_params_common = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT);
        layout_params_common.setMargins(0, DPI / 64, 0, DPI / 64);
        palette_brush_edit_button.setLayoutParams(layout_params_common);
        palette_brush_edit_button.setOnClickListener(getPaletteBrushEditListener());
        ((LinearLayout) palette.getChildAt(0)).addView(palette_brush_edit_button);
        //spacer at beginning
        (palette.getChildAt(0)).setPadding(Tools.dp(15), 0,0,0);

        brushPaletteFrame.removeAllViews();
        brushPaletteFrame.addView(palette);
        if(listView != null)
            listView.invalidateViews();
    }
    public void getPaletteBrushBlock(ViewAdapter viewAdapter){
        viewAdapter.addView(getTitle(Data.tools.getResource(R.string.settingsBrushcolor)));
        //palette
        brushPaletteFrame = new FrameLayout(this);
        refreshPaletteBrushBlock();
        viewAdapter.addView(brushPaletteFrame);
        //opacity
        viewAdapter.addView(getSlider(Data.tools.getResource(R.string.settingsBrushopacity), 0, 255, (Integer) Data.get(Data.brushOpacityInt()), getBrushOpacityListener()));
        viewAdapter.addView(getHint(Data.tools.getResource(R.string.settingsBrushopacityTip)));
        //delimiter
        //viewAdapter.addView(getSpacer());
    }
    public void refreshPaletteBackgroundBlock(){
        // palette
        Palette palette = new Palette(this, Data.getPaletteBackground(), 2, getPaletteBackgroundApplier(), getPaletteBackgroundChecker(), getPaletteBackgroundLongListener());
        palette.setBackgroundColor(backgroundColor);
        //button edit
        TextView palette_brush_edit_button=new AnimatedButton(context);
        palette_brush_edit_button.setText(Data.tools.getResource(R.string.settingsBrushcolorOther));
        LinearLayout.LayoutParams layout_params_common = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT);
        layout_params_common.setMargins(0, DPI / 64, 0, DPI / 64);
        palette_brush_edit_button.setLayoutParams(layout_params_common);
        palette_brush_edit_button.setOnClickListener(getPaletteBackgroundEditListener());
        ((LinearLayout)palette.getChildAt(0)).addView(palette_brush_edit_button);
        //spacer at beginning
        (palette.getChildAt(0)).setPadding(Tools.dp(15), 0,0,0);

        backgroundPaletteFrame.removeAllViews();
        backgroundPaletteFrame.addView(palette);
        if(listView != null)
            listView.invalidateViews();
    }
    public void getPaletteBackgroundBlock(ViewAdapter viewAdapter){
        viewAdapter.addView(getTitle(Data.tools.getResource(R.string.settingsBackgroundcolor)));
        backgroundPaletteFrame = new FrameLayout(this);
        refreshPaletteBackgroundBlock();
        viewAdapter.addView(backgroundPaletteFrame);
        viewAdapter.addView(getButton(Data.tools.getResource(R.string.settingsSwap), getSwapColorsListener(), null));
    }
    public void getBrushSizeBlock(ViewAdapter viewAdapter){
        viewAdapter.addView(getSlider(Data.tools.getResource(R.string.settingsBrushsize), 1, (int)Data.store().DPI / 4, (Integer) Data.get(Data.brushSizeInt()), getBrushSizeListener()));
    }
    public void getGridSizeBlock(ViewAdapter viewAdapter){
        viewAdapter.addView(getSlider(Data.tools.getResource(R.string.settingsGridsize), 0, (int)Data.store().DPI, (Integer) Data.get(Data.gridSizeInt()), getGridSizeListener()));
        viewAdapter.addView(getSlider(getString(R.string.grid_opacity), 1, 99, (Integer) Data.get(Data.gridOpacityInt()), getGridOpacityListener()));
        viewAdapter.addView(getHint(Data.tools.getResource(R.string.settingsGridsizeTip)));
        viewAdapter.addView(getCheckbox(Data.tools.getResource(R.string.settingsGridVertical), (Boolean) Data.get(Data.gridVerticalBoolean()), getGridVerticalListener(), R.drawable.settings_grid, false));
        viewAdapter.addView(getSpacer());
    }
    public void getManageMethodBlock(ViewAdapter viewAdapter){
        viewAdapter.addView(getTitle(Data.tools.getResource(R.string.settingsBrushsizeManagement)));
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> tips = new ArrayList<>();
        ArrayList<Integer> icons = new ArrayList<>();
        ArrayList<View.OnClickListener> listeners = new ArrayList<>();
        ArrayList<MenuStrip.ActiveChecker> values = new ArrayList<>();

        if(Data.tools.isTouchPressureDetectionSupported()){
            names.add(Data.tools.getResource(R.string.settingsPressure));
            tips.add(Data.tools.getResource(R.string.settingsPressureTip));
            icons.add(R.drawable.settings_pressure);
            listeners.add(getManageMethodPressureListener());
            values.add(() -> Data.getManageMethod() == Data.MANAGE_METHOD_PRESSURE);
        }

        if(Data.tools.isTouchSizeDetectionSupported()){
            names.add(Data.tools.getResource(R.string.settingsManageMethodSize));
            tips.add(Data.tools.getResource(R.string.settingsManageMethodSizeTip));
            icons.add(R.drawable.settings_square);
            listeners.add(getManageMethodSizeListener());
            values.add(() -> Data.getManageMethod() == Data.MANAGE_METHOD_SIZE);
        }

        {
            names.add(Data.tools.getResource(R.string.settingsSpeed));
            tips.add(Data.tools.getResource(R.string.settingsSpeedTip));
            icons.add(R.drawable.settings_speed);
            listeners.add(getManageMethodSpeedListener());
            values.add(() -> Data.getManageMethod() == Data.MANAGE_METHOD_SPEED);
        }

        {
            names.add(Data.tools.getResource(R.string.settingsSpeedInverse));
            tips.add(Data.tools.getResource(R.string.settingsSpeedInverseTip));
            icons.add(R.drawable.settings_speed_inverse);
            listeners.add(getManageMethodSpeedInverseListener());
            values.add(() -> Data.getManageMethod() == Data.MANAGE_METHOD_SPEED_INVERSE);
        }

        {
            names.add(Data.tools.getResource(R.string.settingsConstant));
            tips.add(Data.tools.getResource(R.string.settingsConstantTip));
            icons.add(R.drawable.settings_constant);
            listeners.add(getManageMethodConstantListener());
            values.add(() -> Data.getManageMethod() == Data.MANAGE_METHOD_CONSTANT);
        }


        String[] namesArray = new String[names.size()];
        String[] tipsArray = new String[tips.size()];
        int[] iconsArray = new int[icons.size()];
        View.OnClickListener[] listenersArray = new View.OnClickListener[listeners.size()];
        MenuStrip.ActiveChecker[] valuesArray = new MenuStrip.ActiveChecker[values.size()];
        for(int i=0; i<names.size(); i++)
            namesArray[i] = names.get(i);
        for(int i=0; i<tips.size(); i++)
            tipsArray[i] = tips.get(i);
        for(int i=0; i<icons.size(); i++)
            iconsArray[i] = icons.get(i);
        for(int i=0; i<listeners.size(); i++)
            listenersArray[i] = listeners.get(i);
        for(int i=0; i<values.size(); i++)
            valuesArray[i] = values.get(i);

        viewAdapter.addView(getRadioGroup2(
                namesArray,   //names
                tipsArray,    //hints
                iconsArray,
                listenersArray,  //listeners
                valuesArray));         //checked
        viewAdapter.addView(getSpacer());
    }
    public void getImageProcessingBlock(ViewAdapter viewAdapter){
        viewAdapter.addView(getTitle(Data.tools.getResource(R.string.settingsProcessing)));

        getCheckboxBlock(viewAdapter,
                R.string.settingsAntialiasing,
                R.string.settingsAntialiasingTip,
                (Boolean) Data.get(Data.antialiasingBoolean()),
                getAntialiasingListener(),
                R.drawable.ic_antialiasing,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.settingsSmoothing,
                R.string.settingsSmoothingTip,
                (Boolean) Data.get(Data.smoothingBoolean()),
                getSmoothingListener(),
                R.drawable.ic_smoothing2,
                false);

        float currentSensivity = (Float) Data.get(Data.smoothingSensibilityFloat());
        viewAdapter.addView(getSlider(Data.tools.getResource(R.string.settingsSmoothingSensivity), 10, 100, ((int) (currentSensivity * 100f)), getSmoothingSensivityListener()));
        viewAdapter.addView(getHint(getString(R.string.settingsSmoothingSensivityTip)));
        viewAdapter.addView(getSpacer());
    }
    public void getInterfaceBlock(ViewAdapter viewAdapter){
        viewAdapter.addView(getTitle(Data.tools.getResource(R.string.uiElementsForm)));
        viewAdapter.addView(getRadioGroup2(
                new String[]{
                        Data.tools.getResource(R.string.form_squircle),
                        Data.tools.getResource(R.string.form_circle),
                        Data.tools.getResource(R.string.form_rectangle)
                },
                new String[]{
                        Data.tools.getResource(R.string.form_squircle_hint),
                        Data.tools.getResource(R.string.form_circle_hint),
                        Data.tools.getResource(R.string.form_rectangle_hint)
                },
                new int[]{
                        R.drawable.ic_squircle_form,
                        R.drawable.ic_circle_form,
                        R.drawable.ic_rectangle_button
                },
                new View.OnClickListener[]{
                        view -> {Data.setSquircle(); listView.invalidateViews();},
                        view -> {Data.setCircle(); listView.invalidateViews();},
                        view -> {Data.setRect(); listView.invalidateViews();}
                },
                new MenuStrip.ActiveChecker[]{
                        Data::isSqircle,
                        Data::isCircle,
                        Data::isRect
                }
        ));
        viewAdapter.addView(getSpacer());


        viewAdapter.addView(getTitle(Data.tools.getResource(R.string.settingsInterface)));
        viewAdapter.addView(getHint(Data.tools.getResource(R.string.settingsInterfaceTip)));


        //Если включено, касание холста двумя пальцами активирует масштабирование.
        //Если отключено, касание холста двумя пальцами нарисует две линии.



        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            View.OnClickListener listener = view ->
                    startActivityForResult(
                            new Intent(
                                    Settings.ACTION_APP_LOCALE_SETTINGS,
                                    Uri.parse("package:" + context.getPackageName())),
                            CODE_LANGUAGE_CHANGED
                    );
            int textId = R.string.change_language;
            int hintId = R.string.change_language_hint;
            int imageResId = R.drawable.ic_language;
            getButtonBlock(viewAdapter, listener, textId, hintId, imageResId);
        }



        getCheckboxBlock(viewAdapter,
                (R.string.two_fingers_zoom),
                (R.string.two_fingers_zoom_tip),
                (Boolean) Data.get(Data.twoFingersZoomBoolean()),
                getTwoFingersZoomListener(),
                R.drawable.ic_pinch,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.settingsShowInstrumentsSelector,
                R.string.settingsShowInstrumentsSelectorTip,
                (Boolean) Data.get(Data.showInstrumentsSelectorBoolean()),
                getShowInstrumentsSelectorListener(),
                R.drawable.ic_instrument_selector,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.settingsShowColorsSelector,
                R.string.settingsShowColorsSelectorTip,
                (Boolean) Data.get(Data.showColorsSelectorBoolean()),
                getShowColorsSelectorListener(),
                R.drawable.ic_palette,
                true);

        getCheckboxBlock(viewAdapter,
                R.string.settingsShowOnScreenMenuButton,
                R.string.settingsShowOnScreenMenuButtonTip,
                (Boolean) Data.get(Data.showScreenMenuButtonBoolean()),
                getOnScreenButtonListener(),
                R.drawable.ic_menu_button,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.settingsStatusBar,
                R.string.settingsStatusBarTip,
                (Boolean) Data.get(Data.statusBarBoolean()),
                getStatusBarListener(),
                R.drawable.ic_statusbar,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.settingsMenuStrip,
                R.string.settingsMenuStripTip,
                (Boolean) Data.get(Data.menuStripBoolean()),
                getMenuStripListener(),
                R.drawable.settings_menu_strip,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.settingsPaletteStrip,
                R.string.settingsPaletteStripTip,
                (Boolean) Data.get(Data.paletteStripBoolean()),
                getPaletteStripListener(),
                R.drawable.settings_palette,
                true);

        getCheckboxBlock(viewAdapter,
                (R.string.show_help_messages),
                (R.string.show_help_messages_tip),
                (Boolean) Data.get(Data.showHelpMessagesBoolean()),
                getShowHelpListener(),
                R.drawable.ic_help,
                false);

        viewAdapter.addView(getSpacer());

        //------------------------------------------------------------------------------------- BEHAVIOUR
        viewAdapter.addView(getTitle(Data.tools.getResource(R.string.settingsBehaviour)));

        getCheckboxBlock(viewAdapter,
                R.string.settingsBackKeyUndo,
                R.string.settingsBackKeyUndoTip,
                (Boolean) Data.get(Data.backKeyUndoBoolean()),
                getBackKeyUndoListener(),
                R.drawable.settings_back_undo,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.SettingsVolumeKeys,
                R.string.SettingsVolumeKeysHint,
                (Boolean) Data.get(Data.volumeButtonsBoolean()),
                getVolumeKeysListener(),
                R.drawable.settings_volume_keys,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.SettingsVibrationEnable,
                R.string.SettingsVibrationEnableHint,
                (Boolean) Data.get(Data.enableVibrationBoolean()),
                getVibrationEnabledListener(),
                R.drawable.ic_vibrate,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.settingsKeepScreenOn,
                R.string.settingsKeepScreenOnTip,
                (Boolean) Data.get(Data.keepScreenOnBoolean()),
                getKeepScreenOnListener(),
                R.drawable.settings_keep_screen_on,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.settingsMaximimBrightness,
                R.string.settingsMaximimBrightnessTip,
                (Boolean) Data.get(Data.maximumBrightnessBoolean()),
                getMaximumBrightnessListener(),
                R.drawable.settings_max_brigtness,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.settingsAddWatermark,
                R.string.settingsAddWatermarkTip,
                (Boolean) Data.get(Data.watermarkBoolean()),
                getAddWatermarkListener(),
                R.drawable.settings_watermark,
                true);

        viewAdapter.addView(getSpacer());

        //------------------------------------------------------------------------------------- SAMSUNG
        viewAdapter.addView(getTitle(Data.tools.getResource(R.string.settingsSamsung)));

        getCheckboxBlock(viewAdapter,
                R.string.fingerHovering,
                R.string.fingerHoveringTip,
                (Boolean) Data.get(Data.fingerHoverBoolean()),
                getFingerHoveringListener(),
                R.drawable.settings_finger_hovering,
                false);

        getCheckboxBlock(viewAdapter,
                R.string.settingsEinkCrear,
                R.string.settingsEinkCrearTip,
                (Boolean) Data.get(Data.einkClean()),
                getEinkCleanListener(),
                R.drawable.menu_clear,
                false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getCheckboxBlock(viewAdapter,
                    R.string.enable_hardware_acceleration,
                    R.string.enable_hardware_accelerationTip,
                    (Boolean) Data.get(Data.enableHardwareAcceleration()),
                    getHardwareAccelerationListener(),
                    R.drawable.ic_gpu,
                    false);
        }

        getGetFullVersionBlockIfNeeded(viewAdapter);
        viewAdapter.addView(getSpacer());
    }
    public void getOrientationBlock(ViewAdapter viewAdapter){
        viewAdapter.addView(getTitle(Data.tools.getResource(R.string.settingsRotate)));
        viewAdapter.addView(getRadioGroup2(
                new String[]{
                        Data.tools.getResource(R.string.settingsVertical),
                        Data.tools.getResource(R.string.settingsHorizontal),
                        Data.tools.getResource(R.string.settingsAuto)
                },
                new String[]{
                        Data.tools.getResource(R.string.orientation_portrait_hint),
                        Data.tools.getResource(R.string.orientation_landscape_hint),
                        Data.tools.getResource(R.string.orientation_auto_hint)
                },
                new int[]{R.drawable.settings_portrait, R.drawable.settings_landscape, R.drawable.settings_auto},
                new View.OnClickListener[]{getOrientationPortraitListener(), getOrientationLandscapeListener(), getOrientationAutoListener()},
                new MenuStrip.ActiveChecker[]{
                        () -> Data.getOrientationCanvas() == DrawCore.OrientationProvider.ORIENTATION_VERTICAL,
                        () -> Data.getOrientationCanvas() == DrawCore.OrientationProvider.ORIENTATION_HORIZONTAL,
                        () -> Data.getOrientationCanvas() == DrawCore.OrientationProvider.ORIENTATION_AUTO
                }
        ));
        viewAdapter.addView(getSpacer());
    }
    public void getOtherSettingsBlock(ViewAdapter viewAdapter){
        int textId = R.string.settingsOthers;
        int hintId = R.string.settingsOthersTip;
        int imageResId = R.drawable.ic_graduate;
        getButtonBlock(viewAdapter, getOtherSettingsListener(), textId, hintId, imageResId);
        //viewAdapter.addView(getBigButton(Data.tools.getResource(R.string.settingsOthers), Data.tools.getResource(R.string.settingsOthersTip), getOtherSettingsListener(), null));

    }
    public View getSpacer(){
        View view = new View(context);
        view.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Tools.dp(30)));
        view.setBackgroundColor(backgroundColor);
        return view;
    }
    public void getCheckboxBlock(ViewAdapter viewAdapter, int text, int hint, boolean value, View.OnClickListener listener, int image, boolean paidOnly){
        boolean locked = paidOnly && !Data.tools.isFullVersion();
        boolean paid = Data.tools.isPaid();

        MyCheckBox myCheckBox = (MyCheckBox) getCheckbox(Data.tools.getResource(text), value, listener, image, locked);
        viewAdapter.addView(myCheckBox);
        myCheckBox.addHintView(getHint(Data.tools.getResource(hint)));


        if(paidOnly && !paid)
            myCheckBox.addHintView(getPaidFunctionInfo());
    }
    public void getUnsupportedCheckboxBlock(ViewAdapter viewAdapter, int text, int hint, int image){
        MyCheckBox myCheckBox = (MyCheckBox) getCheckbox(Data.tools.getResource(text), false, null, image, true);
        viewAdapter.addView(myCheckBox);
        myCheckBox.addHintView(getHint(Data.tools.getResource(hint)));
        myCheckBox.addHintView(getUnsupportedFunctionInfo());
    }
    public View getPaidFunctionInfo(){
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);

        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(Tools.dp(15), Tools.dp(15)));
        imageView.setImageResource(R.drawable.ic_exclamation);
        linearLayout.addView(imageView);

        TextView textView = new TextView(context);
        textView.setPadding(Tools.dp(5), 0, 0, 0);
        textView.setGravity(Gravity.START);
        textView.setTextColor(Color.argb(150, 255, 255, 255));
        textView.setTextSize((int) (settings_header_text_size * 0.8));
        if(Data.tools.isFullVersion())
            textView.setText(R.string.fullVersionInfoSettingsDemoRemaining);
        else
            textView.setText(R.string.fullVersionInfoSettingsDemoExpired);
        linearLayout.addView(textView);

        return linearLayout;
    }
    public View getUnsupportedFunctionInfo(){
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);

        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(Tools.dp(15), Tools.dp(15)));
        imageView.setImageResource(R.drawable.ic_exclamation);
        linearLayout.addView(imageView);

        TextView textView = new TextView(context);
        textView.setPadding(Tools.dp(5), 0, 0, 0);
        textView.setGravity(Gravity.START);
        textView.setTextColor(Color.argb(150, 255, 255, 255));
        textView.setTextSize((int) (settings_header_text_size * 0.8));
        textView.setText(R.string.unsupportedOnYourDevice);
        linearLayout.addView(textView);

        return linearLayout;
    }
    public void getGetFullVersionBlockIfNeeded(ViewAdapter viewAdapter){
        if(!Data.tools.isPaid()) {
            try{
                LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                linearLayout.setPadding(Tools.dp(15), Tools.dp(10), Tools.dp(15), Tools.dp(10));

                {
                    String text = context.getString(R.string.thisIsFreeVersion);
                    View hint = getHint(text);
                    hint.setPadding(0,0,0,0);
                    linearLayout.addView(hint);
                }

                {
                    View.OnClickListener buttonListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            context.startActivity(new Intent(context, FullVersionInfoActivity.class));
                        }
                    };

                    String text = context.getString(R.string.moreInfo);
                    View button = getButton(text, buttonListener, null);
                    linearLayout.addView(button);
                }
                viewAdapter.addView(linearLayout);
            }
            catch (Exception e){
                Logger.log("Check if localization strings have enough lines to be parsed at SettingsScreen.getGetFullVersionBlockIfNeeded.\n" +
                        "error: " + e);
            }
        }
    }

   //listeners
   Palette.ColorApplier getPaletteBrushLongListener(){
       return (color, colorView, colorPositionIndex) -> {
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
               PopupMenu popupMenu = new PopupMenu(context, colorView);
               popupMenu.getMenu().add(R.string.settings_palette_edit_clone).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                   @Override
                   public boolean onMenuItemClick(MenuItem item) {
                       int[] originalPalette = Data.getPaletteBrush();
                       int[] editedPalette = new int[originalPalette.length+1];
                       for (int i = 0; i < editedPalette.length; i++) {
                           if(i <= colorPositionIndex)
                               editedPalette[i] = originalPalette[i];
                           else
                               editedPalette[i] = originalPalette[i-1];
                       }
                       Data.savePaletteBrush(editedPalette);
                       refreshPaletteBrushBlock();
                       return true;
                   }
               });
               popupMenu.getMenu().add(R.string.settings_palette_edit_delete).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                   @Override
                   public boolean onMenuItemClick(MenuItem item) {
                       int[] originalPalette = Data.getPaletteBrush();
                       if(originalPalette.length == 1){
                           Logger.show("You can't delete last available color");
                           return true;
                       }
                       int[] editedPalette = new int[originalPalette.length-1];
                       for (int i = 0; i < editedPalette.length; i++) {
                           if(i < colorPositionIndex)
                               editedPalette[i] = originalPalette[i];
                           else
                               editedPalette[i] = originalPalette[i+1];
                       }
                       Data.savePaletteBrush(editedPalette);
                       refreshPaletteBrushBlock();
                       return true;
                   }
               });
               popupMenu.getMenu().add(R.string.replace_by_current_color).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                   @Override
                   public boolean onMenuItemClick(MenuItem item) {
                       int brushColor = Data.getBrushColor();
                       int[] originalPalette = Data.getPaletteBrush();
                       originalPalette[colorPositionIndex] = brushColor;
                       Data.savePaletteBrush(originalPalette);
                       refreshPaletteBrushBlock();
                       return true;
                   }
               });
               popupMenu.getMenu().add(R.string.settings_palette_edit_edit).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                   @Override
                   public boolean onMenuItemClick(MenuItem item) {
                       ColorPickerDialog.OnColorChangedListener onColorChangedListener = new ColorPickerDialog.OnColorChangedListener() {
                           @Override
                           public void colorChanged(int color) {
                               int[] originalPalette = Data.getPaletteBrush();
                               originalPalette[colorPositionIndex] = color;
                               Data.savePaletteBrush(originalPalette);
                               refreshPaletteBrushBlock();
                           }
                       };
                       ColorPickerDialog dialog = new ColorPickerDialog(context, onColorChangedListener, color);
                       dialog.show();
                       return true;
                   }
               });
               popupMenu.show();
           }
       };
   }
   Palette.ColorApplier getPaletteBrushApplier(){
       return new Palette.ColorApplier() {
           @Override
           public void applyColor(int color, View colorView, int colorPositionIndex) {
               Data.save(color, Data.brushColorInt());
               preview.invalidate();
           }
       };
   }
    Palette.ColorChecker getPaletteBrushChecker(){
        return new Palette.ColorChecker() {
            @Override
            public boolean isActive(int color) {
                return (Integer)Data.get(Data.brushColorInt()) == color;
            }
        };
    }
    Palette.ColorApplier getPaletteBackgroundLongListener(){
        return (color, colorView, colorPositionIndex) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                PopupMenu popupMenu = new PopupMenu(context, colorView);
                popupMenu.getMenu().add(R.string.settings_palette_edit_clone).setOnMenuItemClickListener(item -> {
                    int[] originalPalette = Data.getPaletteBackground();
                    int[] editedPalette = new int[originalPalette.length+1];
                    for (int i = 0; i < editedPalette.length; i++) {
                        if(i <= colorPositionIndex)
                            editedPalette[i] = originalPalette[i];
                        else
                            editedPalette[i] = originalPalette[i-1];
                    }
                    Data.savePaletteBackground(editedPalette);
                    refreshPaletteBackgroundBlock();
                    return true;
                });
                popupMenu.getMenu().add(R.string.settings_palette_edit_delete).setOnMenuItemClickListener(item -> {
                    int[] originalPalette = Data.getPaletteBackground();
                    if(originalPalette.length == 1){
                        Logger.show(getString(R.string.settings_palette_edit_delete_lasterror));
                        return true;
                    }
                    int[] editedPalette = new int[originalPalette.length-1];
                    for (int i = 0; i < editedPalette.length; i++) {
                        if(i < colorPositionIndex)
                            editedPalette[i] = originalPalette[i];
                        else
                            editedPalette[i] = originalPalette[i+1];
                    }
                    Data.savePaletteBackground(editedPalette);
                    refreshPaletteBackgroundBlock();
                    return true;
                });
                popupMenu.getMenu().add(R.string.replace_by_current_color).setOnMenuItemClickListener(item -> {
                    int[] originalPalette = Data.getPaletteBackground();
                    int backgroundColor = (Integer) Data.get(Data.backgroundColorInt());
                    originalPalette[colorPositionIndex] = backgroundColor;
                    Data.savePaletteBackground(originalPalette);
                    refreshPaletteBackgroundBlock();
                    return true;
                });
                popupMenu.getMenu().add(R.string.settings_palette_edit_edit).setOnMenuItemClickListener(item -> {
                    ColorPickerDialog.OnColorChangedListener onColorChangedListener = color1 -> {
                        int[] originalPalette = Data.getPaletteBackground();
                        originalPalette[colorPositionIndex] = color1;
                        Data.savePaletteBackground(originalPalette);
                        refreshPaletteBackgroundBlock();
                    };
                    ColorPickerDialog dialog = new ColorPickerDialog(context, onColorChangedListener, color);
                    dialog.show();
                    return true;
                });
                popupMenu.show();
            }
        };
    }
    Palette.ColorApplier getPaletteBackgroundApplier(){
        return (color, colorView, colorPositionIndex) -> {
            Data.save(color, Data.backgroundColorInt());
            preview.invalidate();
        };
    }
    Palette.ColorChecker getPaletteBackgroundChecker(){
        return new Palette.ColorChecker() {
            @Override
            public boolean isActive(int color) {
                return (Integer)Data.get(Data.backgroundColorInt()) == color;
            }
        };
    }
    View.OnClickListener getPaletteBrushEditListener(){
        return new View.OnClickListener() {
            @Override public void onClick(View view) {
                ColorPickerDialog dialog = new ColorPickerDialog(context, new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void colorChanged(int color) {
                        Data.save(color, Data.brushColorInt());
                        preview.invalidate();
                        refreshPaletteBrushBlock();
                    }
                }, (Integer)Data.get(Data.brushColorInt()));
                dialog.show();
            }
        };
    }
    View.OnClickListener getPaletteBackgroundEditListener(){
        return new View.OnClickListener() {
            @Override public void onClick(View view) {
                ColorPickerDialog dialog = new ColorPickerDialog(context, new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void colorChanged(int color) {
                        Data.save(color, Data.backgroundColorInt());
                        preview.invalidate();
                        refreshPaletteBackgroundBlock();
                    }
                }, (Integer)Data.get(Data.backgroundColorInt()));
                dialog.show();
            }
        };
    }
    View.OnClickListener getSwapColorsListener(){
        return new View.OnClickListener() {
            @Override public void onClick(View view) {
                int brush=(Integer)Data.get(Data.brushColorInt());
                int background = (Integer)Data.get(Data.backgroundColorInt());
                Data.save(brush, Data.backgroundColorInt());
                Data.save(background, Data.brushColorInt());
                preview.invalidate();
                refreshPaletteBrushBlock();
                refreshPaletteBackgroundBlock();
            }
        };
    }
    SeekBar.OnSeekBarChangeListener getBrushSizeListener(){
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Data.save(i, Data.brushSizeInt());
                preview.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
    }
    SeekBar.OnSeekBarChangeListener getSmoothingSensivityListener(){
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Data.save((float) i / 100f, Data.smoothingSensibilityFloat());
                //preview.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
    }
    SeekBar.OnSeekBarChangeListener getBrushOpacityListener(){
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Data.save(i, Data.brushOpacityInt());
                preview.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
    }
    SeekBar.OnSeekBarChangeListener getGridSizeListener(){
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Data.save(i, Data.gridSizeInt());
                preview.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
    }
    SeekBar.OnSeekBarChangeListener getGridOpacityListener(){
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Data.save(i, Data.gridOpacityInt());
                preview.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
    }
    View.OnClickListener getGridVerticalListener(){
        return new View.OnClickListener() {
            @Override public void onClick(View view) {
                MyCheckBox checkBox = (MyCheckBox)view;
                Data.save(checkBox.isChecked(), Data.gridVerticalBoolean());
                preview.invalidate();
            }
        };
    }
    View.OnClickListener getManageMethodPressureListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Data.save(Data.MANAGE_METHOD_PRESSURE, Data.manageMethodInt());
                preview.invalidate();
            }
        };
    }
    View.OnClickListener getManageMethodSizeListener(){
        return view -> {
            Data.save(Data.MANAGE_METHOD_SIZE, Data.manageMethodInt());
            preview.invalidate();
        };
    }
    View.OnClickListener getManageMethodSpeedListener(){
        return view -> {
            Data.save(Data.MANAGE_METHOD_SPEED, Data.manageMethodInt());
            preview.invalidate();
        };
    }
    View.OnClickListener getManageMethodSpeedInverseListener(){
        return view -> {
            Data.save(Data.MANAGE_METHOD_SPEED_INVERSE, Data.manageMethodInt());
            preview.invalidate();
        };
    }
    View.OnClickListener getManageMethodConstantListener(){
        return view -> {
            Data.save(Data.MANAGE_METHOD_CONSTANT, Data.manageMethodInt());
            preview.invalidate();
        };
    }
    View.OnClickListener getAntialiasingListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.antialiasingBoolean());
            preview.invalidate();
        };
    }
    View.OnClickListener getSmoothingListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.smoothingBoolean());
            preview.invalidate();
        };
    }
    View.OnClickListener getMenuStripListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.menuStripBoolean());
            //Logger.show(Data.tools.getResource(R.string.settingsMenuStripMessage));
        };
    }
    View.OnClickListener getPaletteStripListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.paletteStripBoolean());
            //Logger.show(Data.tools.getResource(R.string.settingsPaletteStripMessage));
        };
    }
    View.OnClickListener getStatusBarListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.statusBarBoolean());
        };
    }
    View.OnClickListener getEinkCleanListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.einkClean());
        };
    }

    View.OnClickListener getHardwareAccelerationListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.enableHardwareAcceleration());
        };
    }
    View.OnClickListener getBackKeyUndoListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.backKeyUndoBoolean());
        };
    }
    View.OnClickListener getKeepScreenOnListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.keepScreenOnBoolean());
        };
    }
    View.OnClickListener getMaximumBrightnessListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.maximumBrightnessBoolean());
        };
    }
    View.OnClickListener getOnScreenButtonListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.showScreenMenuButtonBoolean());
        };
    }
    View.OnClickListener getShowColorsSelectorListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.showColorsSelectorBoolean());
        };
    }
    View.OnClickListener getShowInstrumentsSelectorListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.showInstrumentsSelectorBoolean());
        };
    }
    View.OnClickListener getTwoFingersZoomListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.twoFingersZoomBoolean());
        };
    }
    View.OnClickListener getShowHelpListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.showHelpMessagesBoolean());
        };
    }
    View.OnClickListener getVolumeKeysListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.volumeButtonsBoolean());
        };
    }
    View.OnClickListener getVibrationEnabledListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.enableVibrationBoolean());
        };
    }
    View.OnClickListener getFingerHoveringListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.fingerHoverBoolean());
        };
    }
    View.OnClickListener getAddWatermarkListener(){
        return view -> {
            MyCheckBox checkBox = (MyCheckBox)view;
            Data.save(checkBox.isChecked(), Data.watermarkBoolean());
            preview.invalidate();
        };
    }
    View.OnClickListener getOrientationLandscapeListener(){
        return view -> Data.save(DrawCore.OrientationProvider.ORIENTATION_HORIZONTAL, Data.orientationCanvasInt());
    }
    View.OnClickListener getOrientationPortraitListener(){
        return view -> Data.save(DrawCore.OrientationProvider.ORIENTATION_VERTICAL, Data.orientationCanvasInt());
    }
    View.OnClickListener getOrientationAutoListener(){
        return view -> Data.save(DrawCore.OrientationProvider.ORIENTATION_AUTO, Data.orientationCanvasInt());
    }
    View.OnClickListener getOtherSettingsListener(){
        return view -> startActivity(new Intent(context, OtherSettings.class));
    }
    View.OnClickListener getBuyFullVersionListener(){
        return view -> Data.tools.showBuyFullDialog();
    }

    //providers
    public class Preview extends View{
        ArrayList<Element> elements = new ArrayList<>();

        public Preview(Context context) {
            super(context);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (int) (Data.store().DPI * 0.5f));
            //это костыль нужен потому, что по неизвестным причинам справа от превью есть пустой пиксель. ПРичина где-то в FrameLayout (в системе)
            lp.rightMargin = -1;
            lp.leftMargin = 0;
            setLayoutParams(lp);
            setPadding(0, Tools.dp(4), 0, Tools.dp(4));

            elements.add(new BackgroundElement());
            elements.add(new GridElement());
            elements.add(new BrushExampleElement());
        }
        @Override  protected void onDraw(Canvas preview_canvas){
//                String report = "Preview drawing report: \n";
                    for (Element element : elements) {
//                    long start = System.nanoTime();
                        element.draw(preview_canvas);
//                    long end = System.nanoTime();
//                    report += element.getClass().getName() + " time=" + (end - start) + "ns \n";
                    }
//                Logger.log(report);
        }

        class Element{
            Element(){}
            void draw(Canvas c){}
        }
        class ShadowElement extends Element{
            Paint paint = new Paint();
            float minOpacity = 70;
            float maxOpacity = 00;


            ShadowElement() {
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(1);
                paint.setAntiAlias(false);
            }

            @Override
            void draw(Canvas c) {
                {
                    float begin = 0;
                    float end = getPaddingTop();
                    float step = (maxOpacity - minOpacity) / Math.abs(end - begin);
                    float opacity = minOpacity;
                    for (int y = (int) begin; y < end; y++) {
                        paint.setAlpha((int) opacity);
                        c.drawLine(0, y, getWidth() - 1, y, paint);
                        opacity += step;
                    }
                }
                {
                    float begin = getHeight() - 1;
                    float end = begin - getPaddingBottom();
                    float step = (maxOpacity - minOpacity) / Math.abs(end - begin);
                    float opacity = minOpacity;
                    for (int y = (int) begin; y > end; y--) {
                        paint.setAlpha((int) opacity);
                        c.drawLine(0, y, getWidth() - 1, y, paint);
                        opacity += step;
                    }
                }
            }
        }
        class BackgroundElement extends Element{
            Paint paint = new Paint();
            RectF rectF = new RectF();
            Transition transition = new Transition();

            BackgroundElement() {
                super();
                paint.setAntiAlias(false);
                paint.setStyle(Paint.Style.FILL);
                transition.setAnimTime(800);
            }

            @Override
            void draw(Canvas c) {
                super.draw(c);
                transition.draw(c);
            }
            class Transition extends AnimationAdapter{
                int lastColor = 0;
                int oldColor = 0;


                @Override
                public void draw(Canvas c) {
                    float top = 0;//getPaddingTop();
                    float bottom = getHeight() - 1;// - getPaddingBottom();
                    rectF.set(0, top, getWidth() - 1, bottom);

                    int backgroundColor = (Integer)Data.get(Data.backgroundColorInt());
                    if(lastColor == 0){
                        lastColor = backgroundColor;
                        oldColor = backgroundColor;
                    }
                    if (lastColor != backgroundColor) {
                        oldColor = lastColor;
                        startAnimation();
                    }

                    if(getAnimPercent() == 100) {
                        paint.setColor(backgroundColor);
                        c.drawRect(rectF, paint);
                    }
                    else {
                        paint.setColor(oldColor);
                        c.drawRect(rectF, paint);
                        int cx = getWidth() / 2;
                        int cy = (int)(top + (bottom - top)/2);
                        int maxRadius = getWidth();
                        int radius = maxRadius * (int)getAnimPercent() / 100;
                        paint.setColor(backgroundColor);
                        c.drawCircle(cx, cy, radius, paint);
                    }
                    lastColor = backgroundColor;
                }

                @Override
                protected void redraw() {
                    invalidate();
                }
            }
        }
        class GridElement extends Element{
            Paint paint;

            GridElement() {
                paint = new Paint();
                paint.setStrokeWidth(1);
            }
            @Override void draw(Canvas preview_canvas) {
                int gridSize = (Integer)Data.get(Data.gridSizeInt());
                if(gridSize > 1) {
                    int backgroundColor = (Integer) Data.get(Data.backgroundColorInt());
                    paint.setColor(Data.tools.getGridColor(backgroundColor));

//                    int[] location = new int[2];
//                    getLocationOnScreen(location);
//                    int positionY = location[1];
//
//                    int offset = positionY % gridSize;
//                    offset = gridSize - offset;

                    int offset = 0;

                    int top = 0;//getPaddingTop();
                    int bottom = getHeight() - 1;// - getPaddingBottom();
                    int left = 0;
                    int right = getWidth() - 1;

                    boolean gridVertical = (Boolean)Data.get(Data.gridVerticalBoolean());

                    for (int c = top + offset; c < bottom; c += gridSize)
                        preview_canvas.drawLine(left, c, right, c, paint);

                    if (gridVertical)
                        for (int c = gridSize; c < right; c += gridSize)
                            preview_canvas.drawLine(c, top, c, bottom, paint);
                }
            }
        }
        class BrushExampleElement extends Element{
            Paint paint = null;
            float[] points = null;
            boolean smoothing = false;

            BrushExampleElement() {
                paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
            }

            @Override void draw(Canvas c) {
                float top = 0;//getPaddingTop();
                float bottom = getHeight() - 1;// - getPaddingBottom();

                int brushColor = Data.getBrushColor();
                int brushSize = (Integer)Data.get(Data.brushSizeInt());

                paint.setColor(brushColor);
                paint.setStrokeWidth(brushSize);
                boolean antialiasing = (Boolean)Data.get(Data.antialiasingBoolean());
                paint.setAntiAlias(antialiasing);

                //SINUSOIDA
                {
                    //GENERATE POINTS
                    boolean smoothing_current = (Boolean)Data.get(Data.smoothingBoolean());
                    if(smoothing_current != smoothing) {
                        points = null;
                        smoothing = smoothing_current;
                    }
                    if(points == null) {
                        ArrayList<Float> pointArrayList = new ArrayList<>();
                        float a = (top - bottom) / 3;           //амплитуда
                        float cy = (bottom - top) / 2;          //подъём (центральная точка)
                        float b = 10f / getWidth();             //раздвигание
                        float xmin = getWidth() * 0.05f;          //лево
                        float xmax = getWidth() * 0.96f;          //право
                        float step = getWidth() / 100f;           //точность
                        step = Math.max(step, 1);
                        if(!smoothing_current)
                            step *= 5;
                        float lastX = -1;
                        float lastY = -1;
                        for (float x = xmin; x < xmax; x += step) {
                            float y = a * (float) Math.sin(b * x) + cy;
                            if (lastX != -1) {
                                pointArrayList.add(lastX);
                                pointArrayList.add(lastY);
                                pointArrayList.add(x);
                                pointArrayList.add(y);
                                //Logger.log(lastX + " " + lastY + " - " + x + " " + y);
                            }
                            lastX = x;
                            lastY = y;
                        }


                        points = new float[pointArrayList.size()]; //bx, by, ex, ey, ...
                        for (int i = 0; i < pointArrayList.size(); i++)
                            points[i] = pointArrayList.get(i);
                    }

                    float coef = 1;//Math.min(1, Math.max(0, (getAnimPercent()) / 100f));
                    float totalLength = 0;
                    for (int i = 0; i < points.length; i += 4) {
                        float bx = points[i];
                        float by = points[i + 1];
                        float ex = points[i + 2];
                        float ey = points[i + 3];
                        float dx = ex - bx;
                        float dy = ey - by;
                        totalLength += Math.sqrt(dx * dx + dy * dy);
                    }

                    float allowedLength = totalLength * coef;
                    float drawedLength = 0;
                    float center = allowedLength/2;
                    int manage_method = (Integer)Data.get(Data.manageMethodInt());
                    for (int i = 0; i < points.length; i += 4) {
                        float bx = points[i];
                        float by = points[i + 1];
                        float ex = points[i + 2];
                        float ey = points[i + 3];
                        float dx = ex - bx;
                        float dy = ey - by;
                        float d = (float) Math.sqrt(dx * dx + dy * dy);
                        float remainingLength = allowedLength - drawedLength;
                        float lineCoef = Math.min(1f, Math.max(0, remainingLength / d));
                        ex = bx + lineCoef * dx;
                        ey = by + lineCoef * dy;

                        //Вычисление толщины линии для визуализации динамисеской толщины
                        float dc = Math.abs(center - drawedLength);
                        float dcc = dc / center;                    //1 ... 0 ... 1
                        if(manage_method != Data.MANAGE_METHOD_SPEED)
                            dcc *= dcc;
                        float decrem = brushSize*dcc;               //BS ... 0 ... BS
                        float size = brushSize-decrem;
                        if(manage_method == Data.MANAGE_METHOD_CONSTANT)
                            size = brushSize;
                        else if(manage_method == Data.MANAGE_METHOD_SPEED)
                            size = Math.max(decrem, brushSize / 5f);

                        //Logger.log("size = "+size);
                        paint.setStrokeWidth(size);
                        c.drawCircle(bx, by, size / 2, paint);
                        c.drawLine(bx, by, ex, ey, paint);
                        float drawed_d = (float) Math.sqrt(dx * dx + dy * dy);
                        drawedLength += drawed_d;
                        if(drawedLength >= allowedLength) {
                            c.drawCircle(ex, ey, size/2, paint);
                            break;
                        }
                    }
                }
            }

        }
    }
    public class EasterEggsProvider{
        double brushAtStartup = -1;
        int gridAtStartup = -1;

        void whenStarted(){
            brushAtStartup = (Integer)Data.get(Data.brushSizeInt());
            gridAtStartup = (Integer)Data.get(Data.gridSizeInt());
        }
        void whenFinished(){
            if(Math.abs(brushAtStartup - (Integer)Data.get(Data.brushSizeInt())) == 1) //если пользователь изменил значение кисти ровно на единицу
                Logger.show("Достижение получено: \"Мастер точности!\"");
            if(gridAtStartup != 1 && (Integer)Data.get(Data.gridSizeInt()) == 1)       //если пользователь установил значение размера сетки ровно на единицу
                Logger.show("Достижение получено: \"Бессмысленно и беспощадно!\"");
        }
    }
}