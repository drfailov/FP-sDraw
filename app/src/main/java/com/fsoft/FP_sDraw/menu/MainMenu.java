package com.fsoft.FP_sDraw.menu;

import static com.fsoft.FP_sDraw.common.Tools.setNavBarForeground;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fsoft.FP_sDraw.AboutActivity;
import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.FileSelector;
import com.fsoft.FP_sDraw.FullVersionInfoActivity;
import com.fsoft.FP_sDraw.ImageCropper;
import com.fsoft.FP_sDraw.MainActivity;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.SettingsScreen;
import com.fsoft.FP_sDraw.TestActivity;
import com.fsoft.FP_sDraw.TimeProvider;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.MyImageView;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.instruments.Instrument;
import com.fsoft.FP_sDraw.instruments.Saver;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class created to show main menu
 * Created by Dr. Failov on 17.01.2015.
 */
public class MainMenu extends Dialog {
    private final MainActivity context;
    private final DrawCore draw;
    ScrollView menuView = null;
    int DPI;
    public static int transparentBackgroundColor = Color.argb(150,0,0,0);
    public static int menuBackgroundColor = Color.rgb(39, 50, 56);
    public static int menuAccentColor = Color.rgb(141, 217, 203);
    public static int menuHintColor = Color.argb(150, 255, 255, 255);
    public static int menuTextColor = Color.rgb(255,255,255);
    ColorDrawable menuBackgroundDrawable = new ColorDrawable(menuBackgroundColor);
    int imageSize;
    int textSize;
    int TopStringColor = Color.rgb(57, 66, 73);
    int TopStringTextColor = Color.rgb(117, 126, 133);
    int trialTextColor = Color.rgb(107, 116, 123);
    Palette palette;

    View clipboardPasteButton = null;


    public MainMenu(MainActivity context, DrawCore d) {
        super(context);
        this.context = context;
        setCanceledOnTouchOutside(true);
        draw = d;
        //GET DPI
        DisplayMetrics dm = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);
        DPI = dm.densityDpi;
        //SET PARAMETERS
        imageSize = DPI/5;
        textSize = 14;//DPI / 20;
    }
    public void showMenu(){
        if (menuView == null) {
            //init
            int orientation = draw.orientationProvider.getScreenOrientation();
            LinearLayout row;
            //разметка
            //Logger.log("sDraw.menu", "Ваше меню готовится...", false);
            TimeProvider.start("MainMenu");
            menuView = new ScrollView(context);
            LinearLayout linear = new LinearLayout(context);
            linear.setOrientation(LinearLayout.VERTICAL);
            linear.setBackgroundColor(menuBackgroundColor);
            menuView.addView(linear);

            //fill
            linear.addView(new TopString(context));
            if(Data.tools != null && !Data.tools.isPaid()){
                linear.addView(new FullVersionInfo(context));
            }
            row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackgroundColor(menuBackgroundColor);
            linear.addView(row);
            row.addView(getCell(Data.tools.getResource(R.string.menuSave), R.drawable.menu_save, getSaveListener(), getSaveLongListener(), true));      //SAVE
            row.addView(getCell(Data.tools.getResource(R.string.menuOpen), R.drawable.menu_open, getOpenListener(), null, false));                 //OPEN
            row.addView(getCell(Data.tools.getResource(R.string.menuInsert), R.drawable.ic_images, getInsertListener(), getInsertLongListener(), true));           //INSERT
            row.addView(clipboardPasteButton = getCell(Data.tools.getResource(R.string.menuPaste), R.drawable.menu_insert, getPasteListener(), null, false));           //paste
            //PALETTE
            int size = orientation == DrawCore.OrientationProvider.ORIENTATION_HORIZONTAL ? 1 : 2;
            palette = new Palette(context, Data.getPaletteBrush(), size, getPaletteApplier(), getPaletteChecker(), getPaletteLongApplier());
            palette.setBackgroundDrawable(menuBackgroundDrawable);
            linear.addView(palette);

            //BRUSH SIZE
            final BrushSizeView brushSizeView = new BrushSizeView(context, draw);
            brushSizeView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DPI/4));
            linear.addView(brushSizeView);
            palette.addApplier((color, colorView, colorPositionIndex) -> brushSizeView.invalidate());
            //TOOLBAR
            //Эта операция выполняется во втором потоке.
            //Ожидание пока меню будет готово
            int waitingCounter = 0;
            while (draw.currentInstrument == null) {
                try {
                    Logger.log("Menu: Waiting, while instruments will be ready...");
                    //noinspection BusyWait
                    Thread.sleep(100);
                } catch (Exception e) {
                    Logger.log("Exception while sleep() o_O");
                }
                waitingCounter++;
                if (waitingCounter > 100) {
                    Logger.log("Menu: время ожидания исчерпано. Будь что будет!");
                    break;
                }
            }
            ArrayList<Instrument> instruments = new ArrayList<>();
            for (Instrument instrument:draw.instruments)
                if(instrument.isVisibleToUser())
                    instruments.add(instrument);
            linear.addView(getToolbar(instruments));

            row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            linear.addView(row);
            //разные варианты для разных положений экрана
            if (orientation == DrawCore.OrientationProvider.ORIENTATION_HORIZONTAL) {//ландшафт
                row.addView(getCell(Data.tools.getResource(R.string.menuSettings), R.drawable.menu_settings, getSettingsListener(), null, false));
                row.addView(getCell(Data.tools.getResource(R.string.menuUndo), R.drawable.menu_undo, getUndoListener(), null, false));
                row.addView(getCell(Data.tools.getResource(R.string.menuClear), R.drawable.menu_clear, getClearListener(), null, false));
                row.addView(getCell(Data.tools.getResource(R.string.menuRedo), R.drawable.menu_redo, getRedoListener(), null, false));
                row.addView(getCell(Data.tools.getResource(R.string.menuAbout), R.drawable.menu_about, getAboutListener(), getDeveloperLongListener(), false));
            } else {
                row.addView(getCell(Data.tools.getResource(R.string.menuUndo), R.drawable.menu_undo, getUndoListener(), null, false));                 //UNDO
                row.addView(getCell(Data.tools.getResource(R.string.menuClear), R.drawable.menu_clear, getClearListener(), null, false));              //CLEAR
                row.addView(getCell(Data.tools.getResource(R.string.menuRedo), R.drawable.menu_redo, getRedoListener(), null, false));
                row = new LinearLayout(context);
                linear.addView(row);
                row.addView(getCell(Data.tools.getResource(R.string.menuSettings), R.drawable.menu_settings, getSettingsListener(), null, false));     //SETTINGS
                row.addView(getCell(Data.tools.getResource(R.string.menuAbout), R.drawable.ic_about, getAboutListener(), getDeveloperLongListener(), false)); //ABOUT
            }

            //CONFIG DIALOG
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            setContentView(menuView);
            setCanceledOnTouchOutside(true);
            WindowManager.LayoutParams wLayoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
            wLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            wLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            //Эта строка почему-то ломает позиционирование окна снизу экрана. Без этой хуже не становится, зато прохожит баг
            //wLayoutParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            wLayoutParams.horizontalMargin = 0f;
            wLayoutParams.verticalMargin = 0f;
            wLayoutParams.windowAnimations = R.style.DialogAnimation;
            //Аналогично, убрав строку FILL_HORIZONTAL хуже не стало
            //wLayoutParams.gravity = Gravity.BOTTOM | Gravity.FILL_HORIZONTAL;
            wLayoutParams.gravity = Gravity.BOTTOM;
            getWindow().setAttributes(wLayoutParams);
            //getWindow().setBackgroundDrawable(menuBackgroundDrawable);
            //запретить обработку клавиш громкости, т.к. меню ими можно вызвать
            setOnKeyListener((dialogInterface, i, keyEvent) -> ((Boolean) Data.get(Data.volumeButtonsBoolean()))
                    && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                    || keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN));

            TimeProvider.finish("MainMenu");
        }
        palette.refresh();

        //show or hide paste button based on clipboard content
        if(clipboardPasteButton != null){
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                ClipboardManager mClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (mClipboard == null
                        || mClipboard.getPrimaryClip() == null
                        || mClipboard.getPrimaryClip().getItemCount() == 0
                        || mClipboard.getPrimaryClip().getItemAt(0) == null
                        || mClipboard.getPrimaryClip().getItemAt(0).getUri() == null) {
                    clipboardPasteButton.setVisibility(View.GONE);
                } else {
                    Uri uri = mClipboard.getPrimaryClip().getItemAt(0).getUri();
                    Logger.log(mClipboard.getPrimaryClip().getItemAt(0).toString());
                    String path = uri.getPath();
                    Logger.log("Clipboard path=" + path);
                    Logger.log("Clipboard path=" + path);
                    if (path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".png") || path.toLowerCase().contains("image"))
                        try {
                            InputStream inputStream = context.getContentResolver().openInputStream(uri);
                            inputStream.close();
                            clipboardPasteButton.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            clipboardPasteButton.setVisibility(View.GONE);
                        }
                    else
                        clipboardPasteButton.setVisibility(View.GONE);
                }
            }
            else
                clipboardPasteButton.setVisibility(View.GONE);
        }

        show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.getWindow().setNavigationBarColor(menuBackgroundColor);
            setNavBarForeground(true, context, draw.view);
        }
    }
    private void closeMenu(){
        if(context != null){
            context.mainMenu = null;
        }
        dismiss();
    }

    @Override
    public void hide() {
        super.hide();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int backgroundColor = (Integer) Data.get(Data.backgroundColorInt());
            context.getWindow().setNavigationBarColor(backgroundColor);
            setNavBarForeground(!Tools.isLightColor(backgroundColor), context, draw.view);
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int backgroundColor = (Integer) Data.get(Data.backgroundColorInt());
            context.getWindow().setNavigationBarColor(backgroundColor);
            setNavBarForeground(!Tools.isLightColor(backgroundColor), context, draw.view);
        }
    }

    View getToolbar(ArrayList<Instrument> instruments){
        MenuStrip menuStrip = new MenuStrip(context, menuBackgroundColor);
        //здесь список инструментов точно готов
        for(int i=0; i<instruments.size(); i++) {
            Instrument cur = instruments.get(i);
            MenuStrip.ActiveChecker activeChecker = cur::isActive;
            View.OnClickListener onClick = cur.getOnClickListener();
            menuStrip.addButton(cur.getImageResourceID(), /*transformListener*/(onClick), activeChecker, (int)(Data.store().DPI * 0.28), cur.getVisibleName());
        }
        LinearLayout.LayoutParams lp= new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Tools.dp(5);
        menuStrip.setLayoutParams(lp);
        return menuStrip;
    }
    private View getCell(String itemText, int imageID, View.OnClickListener onClick, View.OnLongClickListener onLongClick, boolean dots) {
        Cell item = null;
        try {
            //make item layout
            item = new Cell(context);
            item.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams itemLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            itemLP.weight = 1;
            item.setLayoutParams(itemLP);
            item.setBackgroundColor(menuBackgroundColor);

            //fill image
            MyImageView image = new MyImageView(context);
            image.setDrawable(imageID);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(imageSize, imageSize);
            tlp.gravity = Gravity.CENTER;
            image.setLayoutParams(tlp);
            item.addView(image);

            //fill text
            TextView text = new TextView(context);
            text.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            //lp.setMargins(0, 0, 0, DPI/32);
            text.setLayoutParams(lp);
            text.setTextColor(menuTextColor);
            text.setText(itemText);
            text.setTextSize(textSize);
            item.addView(text);

            if (dots) {
                LinearLayout bottomLayout = new LinearLayout(context);
                bottomLayout.setGravity(Gravity.CENTER_HORIZONTAL);
                bottomLayout.setOrientation(LinearLayout.HORIZONTAL);
                TextView textView = new TextView(context);
                textView.setText("●  ●  ●");
                textView.setTextSize(2);
                textView.setTextColor(Color.argb(90, 255,255,255));
                LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                l.setMargins(0, DPI/50, 0, 0);
                textView.setLayoutParams(l);
                bottomLayout.addView(textView);
                item.addView(bottomLayout);
            }

            item.setPadding(0,0,0, DPI/25);
            item.setOnClickListener(onClick);
            if (onLongClick != null)
                item.setOnLongClickListener(onLongClick);
            //Logger.log("sDraw.menu", "Мы добавили " + itemText + " в Ваше меню.", false);
        } catch (Exception | OutOfMemoryError e) {
            Logger.log("MenuProvider.getCell ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return item;
    }
    View.OnClickListener getSaveListener() {
        return view -> {
            if(Build.VERSION.SDK_INT < 33) {
                //check for permission
                context.requestFileReadWritePermission(
                        () -> {
                            draw.saver.save(false);
                            closeMenu();
                        },
                        () -> Logger.log("SaveButton", R.string.permissionNeededFileAccess, true));
            }
            else{
                draw.saver.save(false);
                closeMenu();
            }

        };
    }
    View.OnLongClickListener getSaveLongListener() {
        return view -> {
            Tools.vibrate(view);
            Logger.log(context.getClass().getName());
            if(Build.VERSION.SDK_INT < 33) {
                context.requestFileReadWritePermission(
                        this::doSaveLongActionWhenPermissionGranted,
                        () -> Logger.log("SaveButton", R.string.permissionNeededFileAccess, true));
            }
            else {
                //if android 13+, then permission always denied, but file is writing because uses ContentManager not File.
                doSaveLongActionWhenPermissionGranted();
            }
            return true;
        };
    }
    void doSaveLongActionWhenPermissionGranted(){
        closeMenu();
        MenuPopup menuPopup = new MenuPopup(context);
        if(draw.selectandmove.selectedImage != null){
            menuPopup.addButton(context.getString(R.string.saveSelectedFragment), v -> draw.selectandmove.onSaveClick());
        }
        menuPopup.addButton(
                context.getString(R.string.saveFragment),
                view13 -> {
                    ImageCropper.initBitmap = draw.createFullBitmap();
                    ImageCropper.initFixedSize = null;
                    ImageCropper.initTransparent = false;
                    ImageCropper.initListener = croppedImage -> {
                        try {
                            draw.saver.saveImageToGallery(croppedImage, Saver.addDateTimeExtension("sDraw"));
                        }
                        catch (Exception | OutOfMemoryError e){
                            Logger.log("MainMenu.getSaveLongListener.OnCropListener.OnCrop", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                        }
                    };
                    context.startActivity(new Intent(context, ImageCropper.class));
                }
        );
        menuPopup.addButton(
                context.getString(R.string.saveFragmentWithTransparency),
                view1 -> {
                    ImageCropper.initBitmap = draw.createFullTransparentBitmap();
                    ImageCropper.initTransparent = true;
                    ImageCropper.initFixedSize = null;
                    ImageCropper.initListener = croppedImage -> {
                        try {
                            draw.saver.saveImageToGallery(croppedImage, Saver.addDateTimeExtension("sDraw"));
                        }
                        catch (Exception | OutOfMemoryError e){
                            Logger.log("MainMenu.getSaveLongListener.OnCropListener.OnCrop", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                        }
                    };
                    context.startActivity(new Intent(context, ImageCropper.class));
                }
        );
        menuPopup.addButton(
                context.getString(R.string.saveFragmentWithoutGrid),
                view1 -> {
                    draw.saver.save(true);
                }
        );

        menuPopup.addButton(
                context.getString(R.string.save_save_fragment_with_fixed_size),
                view12 -> openInputSaveSizeWindow()
        );
        menuPopup.addSpacer();
        menuPopup.setHeaderColor(Color.WHITE);
        menuPopup.setHeader(Data.tools.getResource(R.string.menuSave));
        menuPopup.show();
    }
    void openInputSaveSizeWindow(){
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(context.getString(R.string.save_enter_image_size));
        alertDialog.setIcon(R.drawable.menu_save);
        alertDialog.setCancelable(false);
        alertDialog.setMessage(context.getString(R.string.save_enter_image_size_tip));


        final EditText etWidth = new EditText(context);
        etWidth.setHint(R.string.save_width);
        etWidth.setInputType(InputType.TYPE_CLASS_NUMBER);
        int lastWidth = (Integer)Data.get(Data.saveFixedSizeLastWidthInt());
        etWidth.setText(String.valueOf(lastWidth));

        final EditText etHeight = new EditText(context);
        etHeight.setHint(R.string.save_height);
        etHeight.setInputType(InputType.TYPE_CLASS_NUMBER);
        int lastHeight = (Integer)Data.get(Data.saveFixedSizeLastHeightInt());
        etHeight.setText(String.valueOf(lastHeight));

        TextView textViewX = new TextView(context);
        textViewX.setText(" x ");
        textViewX.setTextSize(18);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.addView(etWidth, Tools.dp(80), ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayout.addView(textViewX);
        linearLayout.addView(etHeight, Tools.dp(80), ViewGroup.LayoutParams.WRAP_CONTENT);

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.text_input_next), (dialog, which) -> {});
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel), (dialog, which) -> alertDialog.dismiss());

        alertDialog.setView(linearLayout);
        alertDialog.show();

        //https://stackoverflow.com/a/15619098/2203337
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                String widthString = etWidth.getText().toString();
                String heightString = etHeight.getText().toString();
                if(widthString.trim().length()==0 || heightString.trim().length()==0)
                    throw new Exception(context.getString(R.string.save_size_must_be_not_empty));
                int width = Integer.parseInt(widthString);
                int height = Integer.parseInt(heightString);
                if(width <= 3 || height <= 3)
                    throw new Exception(context.getString(R.string.save_size_is_too_small));
                if(width > 5500 || height > 5500)
                    throw new Exception(context.getString(R.string.save_size_is_too_big));
                Runtime runtime = Runtime.getRuntime();
                long freeMemory = runtime.maxMemory()-runtime.totalMemory();
                long memoryOfBitmap = width*height*4L;
                Logger.log("freeMemory: " + freeMemory/1000000+"M");
                Logger.log("memoryOfBitmap: " + memoryOfBitmap/1000000+"M");
                if(memoryOfBitmap > freeMemory)
                    throw new Exception(context.getString(R.string.save_not_enough_memory));
                Data.save(width, Data.saveFixedSizeLastWidthInt());
                Data.save(height, Data.saveFixedSizeLastHeightInt());
                Point pointSize = new Point(width, height);
                alertDialog.dismiss();
                ImageCropper.initBitmap = draw.createFullBitmap();
                ImageCropper.initFixedSize = pointSize;
                ImageCropper.initTransparent = false;
                ImageCropper.initListener = croppedImage -> {
                    try {
                        draw.saver.saveImageToGallery(croppedImage, Saver.addDateTimeExtension("sDraw"));
                    }
                    catch (Exception | OutOfMemoryError e){
                        Logger.log("MainMenu.getSaveLongListener.OnCropListener.OnCrop", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                    }
                };
                context.startActivity(new Intent(context, ImageCropper.class));
            }
            catch (Exception e){
                Logger.log(e);
                Logger.show(e.getMessage());
            }
        });
    }
    View.OnClickListener getOpenListener() {
        if(Build.VERSION.SDK_INT < 33) {
            return view -> context.requestFileReadWritePermission(
                    this::doOpenActionWhenPermissionGranted,
                    () -> Logger.log("OpenButton", R.string.permissionNeededFileAccess, true));
        }
        else {
            return view -> doOpenActionWhenPermissionGranted();
        }
    }
    void doOpenActionWhenPermissionGranted(){
        FileSelector.initNames = new String[]{
                Data.tools.getResource(R.string.openSave),
                Data.tools.getResource(R.string.openAutosave)
        };
        FileSelector.initPaths = new File[]{
                Data.getTransparentFolder(context),
                Data.getAutosaveFolder(context)
        };
        FileSelector.initAuxPaths = new File[]{
                new File(Data.getPicturessDrawFolder(), "transparent"),
                null
        };
        FileSelector.initAction = path -> draw.saver.open(path.getPath());
        context.startActivity(new Intent(context, FileSelector.class));
        context.overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
        closeMenu();
    }
    View.OnClickListener getInsertListener() {
        if(Build.VERSION.SDK_INT < 33) {
            return view -> context.requestFileReadWritePermission(
                    this::doInsertActionWhenPermissionGranted,
                    () -> Logger.log("InsertButton", R.string.permissionNeededFileAccess, true));
        }
        else{
            return view -> doInsertActionWhenPermissionGranted();
        }
    }
    void doInsertActionWhenPermissionGranted(){
        try {
            closeMenu();
            // create an instance of the intent of the type image
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType("image/*");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            // pass the constant to compare it with the returned requestCode
            context.startActivityForResult(Intent.createChooser(i, "Select Picture"), MainActivity.REQUEST_INSERT_FILE_PROGRAM);
            context.overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
        }
        catch (Exception e){
            Logger.log("InsertButton", R.string.no_gallery_app, true);
        }
    }
    View.OnLongClickListener getInsertLongListener() {
        if(Build.VERSION.SDK_INT < 33) {
            return view -> {
                context.requestFileReadWritePermission(
                        this::doInsertLongActionWhenPermissionGranted,
                        () -> Logger.log("InsertButton", R.string.permissionNeededFileAccess, true));
                return true;
            };
        }
        else {
            return view -> {
                doInsertLongActionWhenPermissionGranted();
                return true;
            };
        }
    }
    View.OnClickListener getPasteListener() {
        return view -> {
            try {
                //show or hide paste button based on clipboard content
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB){
                    ClipboardManager mClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if(mClipboard == null
                            || mClipboard.getPrimaryClip() == null
                            || mClipboard.getPrimaryClip().getItemCount() == 0
                            || mClipboard.getPrimaryClip().getItemAt(0) == null
                            || mClipboard.getPrimaryClip().getItemAt(0).getUri() == null){
                        clipboardPasteButton.setVisibility(View.GONE);
                    }
                    else{
                        String path = mClipboard.getPrimaryClip().getItemAt(0).getUri().getPath();
                        if(path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".png") || path.toLowerCase().contains("image")) {
                            closeMenu();
                            draw.context.openFileInserter(mClipboard.getPrimaryClip().getItemAt(0).getUri());
                        }
                    }
                }
            }
            catch (Exception e){
                Logger.log(e);
            }
        };

    }
    void doInsertLongActionWhenPermissionGranted(){
        try {
            closeMenu();
            // create an instance of the intent of the type image
            Intent i = new Intent();
            i.setType("image/*");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            i.setAction(Intent.ACTION_GET_CONTENT);
            // pass the constant to compare it with the returned requestCode
            context.startActivityForResult(Intent.createChooser(i, "Select Picture"), MainActivity.REQUEST_INSERT_FILE_PROGRAM);
            context.overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
        }
        catch (Exception e){
            Logger.log("InsertButton", R.string.no_gallery_app, true);
        }
    }
    View.OnClickListener getUndoListener() {
        return view -> {
            draw.undoProvider.undo();
            closeMenu();
        };
    }
    View.OnClickListener getRedoListener() {
        return view -> {
            draw.undoProvider.redo();
            closeMenu();
        };
    }
    View.OnClickListener getClearListener() {
        return view -> {
            draw.clear();
            closeMenu();
        };
    }
    View.OnClickListener getSettingsListener() {
        return view -> {
            context.startActivity(new Intent(context, SettingsScreen.class));
            context.overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
            closeMenu();
        };
    }
    View.OnClickListener getAboutListener() {
        return view -> {
            context.startActivity(new Intent(context, AboutActivity.class));
            context.overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
            closeMenu();
        };
    }
    View.OnLongClickListener getDeveloperLongListener() {
        return view -> {
            closeMenu();
            OnClickListener listenerDeveloperZone = (dialogInterface, i) -> {
                context.startActivity(new Intent(context, TestActivity.class));
                context.overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
            };
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle(Data.tools.getResource(R.string.FunnyCenterQuestionHeader));
            alert.setMessage(Data.tools.getResource(R.string.FunnyCenterQuestionMessage));
            alert.setPositiveButton(Data.tools.getResource(R.string.FunnyCenterQuestionOK), listenerDeveloperZone);
            alert.setNegativeButton(Data.tools.getResource(R.string.FunnyCenterQuestionCancel), null);
            alert.show();
            return false;
        };
    }
    Palette.ColorApplier getPaletteLongApplier(){
        return (color, colorView, colorPositionIndex) -> {
            Data.save(color, Data.backgroundColorInt());
            draw.refresh();
            draw.redraw();
        };
    }
    Palette.ColorApplier getPaletteApplier(){
        return (color, colorView, colorPositionIndex) -> {
            Data.save(color, Data.brushColorInt());
            draw.refresh();
            draw.redraw();
        };
    }
    Palette.ColorChecker getPaletteChecker(){
        return color -> (Integer)Data.get(Data.brushColorInt()) == color;
    }


    class TopString extends View {
        int menuColor = menuBackgroundColor;
        int textColor = TopStringTextColor;
        int shadowSize = (int)(Data.store().DPI * 0.05f);
        String text = "FP sDraw " + Data.tools.getResource(R.string.version);
        Paint paint = new Paint();

        TopString(Context context) {
            super(context);
            setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, (int)Data.store().DPI / 4));
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(TopStringColor);
            {//DRAW TEXT
                int top = 0;
                int bottom = getHeight() - shadowSize;
                int fund = shadowSize;
                int textSize = bottom - top - fund*2;
                int width = getWidth();
                paint.setAntiAlias(true);
                paint.setTextSize(textSize);
                paint.setColor(textColor);
                float textWidth = paint.measureText(text);
                float textX = (width - textWidth)/2;
                float textY = bottom - fund*1.5f;
                canvas.drawText(text, textX, textY, paint);
            }
            {//DRAW SHADOW
                int top = getHeight() - shadowSize;
                int bottom = getHeight();
                paint.setColor(menuColor);
                paint.setAntiAlias(false);
                paint.setStrokeWidth(1);
                canvas.drawRect(0, top, getWidth(), bottom, paint);
                for (int y = top; y < bottom; y++) {
                    float dif = bottom - y;
                    float totDif = bottom - top;
                    paint.setColor(Color.argb((int) (50f * (dif / totDif)), 0, 0, 0));
                    canvas.drawLine(0, y, getWidth(), y, paint);
                }
            }
        }
    }
    public static class Cell extends LinearLayout{
        int backgroudColor = Color.DKGRAY;
        int circleOpacityMax = 160;
        int circleDelay = 20;
        boolean hovered = false;

        int circleX = -1;
        int circleY = -1;
        int circleSize = 0;
        int circleOpacity = 0;
        Timer circleTimer = null;
        Paint circlePaint = new Paint();

        public Cell(Context context) {
            super(context);
        }

        @Override
        protected void onDetachedFromWindow() {
            if(circleTimer != null)
                circleTimer.cancel();
            circleOpacity = 0;
            super.onDetachedFromWindow();
        }

        @Override
        public void setBackgroundColor(int color) {
            backgroudColor = color;
            super.setBackgroundColor(Color.TRANSPARENT);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(backgroudColor);
            if(hovered)
                canvas.drawColor(Color.argb(20, 255, 255, 255));
            if(circleOpacity > 10) {
                circlePaint.setColor(Color.argb(circleOpacity, 255, 255, 255));
                canvas.drawCircle(circleX, circleY, circleSize, circlePaint);
            }
            super.onDraw(canvas);
        }

        private void beginHighlight(int x, int y){
            if(circleTimer != null){
                circleTimer.cancel();
                circleTimer = null;
            }
            circleX = x;
            circleY = y;
            circleSize = 20;
            circleOpacity = circleOpacityMax;
            circleTimer = new Timer();
            circleTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    circleSize *= 1.8;
                    circleOpacity -= 15;
                    if(circleOpacity <= 0) {
                        circleOpacity = 0;
                        circleTimer.cancel();
                    }
                    postInvalidate();
                }
            }, circleDelay, circleDelay);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if(!Data.tools.isAllowedDeviceForUi(event))
                return false;
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if(action == MotionEvent.ACTION_DOWN){                                         //залить черным
                beginHighlight((int)event.getX(), (int)event.getY());
            }
            return super.onTouchEvent(event);
        }

        @Override
        public void onHoverChanged(boolean hovered) {
            super.onHoverChanged(hovered);
            this.hovered = hovered;
            invalidate();
        }

        Handler clickHandler;
        @Override
        public boolean performClick() {
            //
            clickHandler = new Handler();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    clickHandler.post(Cell.super::performClick);
                }
            }, 100);
            return true;
        }
    }
    class FullVersionInfo extends LinearLayout{
        public FullVersionInfo(final Context context) {
            super(context);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setPadding(Tools.dp(10), 0, Tools.dp(10), Tools.dp(20));
            if(Data.tools.isPaid())
                return;

            if(Data.tools.isFullVersion()) {
                int remaining = (Integer) Data.get(Data.paidCounter());
                TextView text = new TextView(context);
                text.setGravity(Gravity.CENTER);
                text.setText(context.getString(R.string.fullVersionInfoMainMenuDemoRemaining).replace("%DAYS%", String.valueOf(remaining)));
                text.setTextColor(trialTextColor);
                if(Build.VERSION.SDK_INT >= 11)
                    text.setAlpha(0.7f);
                text.setTextSize(14);
                addView(text);
            }
            else {
                TextView text = new TextView(context);
                text.setGravity(Gravity.CENTER);
                text.setText(R.string.fullVersionInfoMainMenuDemoExpired);
                text.setTextColor(Color.WHITE);
                if(Build.VERSION.SDK_INT >= 11)
                    text.setAlpha(0.7f);
                text.setTextSize(14);
                addView(text);
            }
            setOnClickListener(v -> {
                //closeMenu();
                context.startActivity(new Intent(context, FullVersionInfoActivity.class));
            });
        }

    }

}
