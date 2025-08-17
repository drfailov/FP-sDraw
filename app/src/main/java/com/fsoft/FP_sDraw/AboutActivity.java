package com.fsoft.FP_sDraw;

/*
 *
 * Экран "О прграмме" - показывает описание программы и кнопки разные для открытия руководства и проверки обновлений
 * Created with IntelliJ IDEA.
 * User: Dr. Failov
 * Date: 27.05.13
 * Time: 21:52
 * Updated: 31-10-2017
 */
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.MenuPopup;
import java.util.Calendar;

public class AboutActivity extends Activity {
    private int logoClickCounter=0;

    private View menuButton = null;
    //private TextView copyrightLabel = null;
    private ImageView logoView = null;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        requestWindowFeature(Window.FEATURE_NO_TITLE);  //убрать панель уведомлений
        setContentView(R.layout.about);
        menuButton = findViewById(R.id.button_menu);
        //copyrightLabel = (TextView)findViewById(R.id.textViewCopyright);
        logoView = findViewById(R.id.image_logo);

//        if(copyrightLabel != null){
//            int year = Calendar.getInstance().get(Calendar.YEAR);
//            if(year < 2021)
//                year = 2021;
//            String text = copyrightLabel.getText().toString().replace("%YEAR%", String.valueOf(year));
//            copyrightLabel.setText(text);
//        }

        if(menuButton != null)
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMenu();
                }
            });

        if(logoView != null)                  //пасхалка
            logoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {logoClickCounter++;
                    if(logoClickCounter==5){
                        logoView.setImageResource(R.drawable.bird);
                        logoView.setOnLongClickListener(new View.OnLongClickListener() { @Override  public boolean onLongClick(View view) {
                            openEasterEgg();
                            return true;
                        }});
                    }
                }
            });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(Color.parseColor("#273238"));
                getWindow().setNavigationBarColor(Color.parseColor("#273238"));
        }

    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getKeyCode()==KeyEvent.KEYCODE_MENU) {
            if(event.getAction() == KeyEvent.ACTION_UP){
                Logger.log("TextInput.dispatchKeyEvent", "Сработала KEYCODE_MENU", false);
                showMenu();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    void showMenu() {
        try{
            MenuPopup menuPopup = new MenuPopup(this);
            menuPopup.setHeader(Data.tools.getResource(R.string.menuAbout));

            menuPopup.addButton(Data.tools.getResource(R.string.aboutShowChangelog), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openChangelog();
                }
            });

            menuPopup.addButton(Data.tools.getResource(R.string.aboutCopyrightNote), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openCopyright();
                }
            });

            menuPopup.addButton(Data.tools.getResource(R.string.aboutPermissionsNote), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPermissiongNote();
                }
            });

            menuPopup.addSpacer();
            menuPopup.show();
        }catch (Exception | OutOfMemoryError e){
            Logger.log("AboutActivity.dispatchKeyEvent " + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
        }
    }
    void openChangelog(){
        AlertDialog.Builder dialog= new AlertDialog.Builder(this);
        ScrollView scroll=new ScrollView(this);
        scroll.setPadding(Tools.dp(10), Tools.dp(10), Tools.dp(10), Tools.dp(10));
        //scroll.setBackgroundColor(Color.rgb(100,100,100));
        final TextView text=new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setText(R.string.loading);
        text.setTextSize(12);
        text.setScrollContainer(true);
        scroll.addView(text);
        dialog.setView(scroll);
        dialog.setTitle("История изменений");
        dialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Tools.sleep(500);

                menuButton.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String utf8str = Data.tools.readFromAssetsWin1251(getApplicationContext(), "changelog.txt");
                            text.setText(utf8str);
                        }
                        catch (Exception e){
                            text.setText("Error: " + e.getMessage());
                        }
                    }
                });
            }
        }).start();
    }
    void openCopyright(){
        TextView textView = new TextView(AboutActivity.this);
        textView.setText(Html.fromHtml(
                "In this program used third-party icons. Here's icons sources:" +
                        "<br/><br/>" +
                        "<div>Icon made by <a href=\"http://circularchaos.com\" title=\"Balraj Chana\">Balraj Chana</a> from <a href=\"http://www.flaticon.com\" title=\"Flaticon\">www.flaticon.com</a> is licensed under <a href=\"http://creativecommons.org/licenses/by/3.0/\" title=\"Creative Commons BY 3.0\">CC BY 3.0</a></div>" +
                        "<div>Icon made by <a href=\"http://mobiletuxedo.com\" title=\"Mobiletuxedo\">Mobiletuxedo</a> from <a href=\"http://www.flaticon.com\" title=\"Flaticon\">www.flaticon.com</a> is licensed under <a href=\"http://creativecommons.org/licenses/by/3.0/\" title=\"Creative Commons BY 3.0\">CC BY 3.0</a></div>" +
                        "<div>Icon made by <a href=\"http://fontawesome.io\" title=\"Dave Gandy\">Dave Gandy</a> from <a href=\"http://www.flaticon.com\" title=\"Flaticon\">www.flaticon.com</a> is licensed under <a href=\"http://creativecommons.org/licenses/by/3.0/\" title=\"Creative Commons BY 3.0\">CC BY 3.0</a></div>" +
                        "<div>Icon made by <a href=\"http://www.google.com\" title=\"Google\">Google</a> from <a href=\"http://www.flaticon.com\" title=\"Flaticon\">www.flaticon.com</a> is licensed under <a href=\"http://creativecommons.org/licenses/by/3.0/\" title=\"Creative Commons BY 3.0\">CC BY 3.0</a></div>" +
                        "<div>Icon made by <a href=\"http://www.freepik.com\" title=\"Freepik\">Freepik</a> from <a href=\"http://www.flaticon.com\" title=\"Flaticon\">www.flaticon.com</a> is licensed under <a href=\"http://creativecommons.org/licenses/by/3.0/\" title=\"Creative Commons BY 3.0\">CC BY 3.0</a></div>" +
                        "<div>Icon made by <a href=\"http://www.elegantthemes.com\" title=\"Elegant Themes\">Elegant Themes</a> from <a href=\"http://www.flaticon.com\" title=\"Flaticon\">www.flaticon.com</a> is licensed under <a href=\"http://creativecommons.org/licenses/by/3.0/\" title=\"Creative Commons BY 3.0\">CC BY 3.0</a></div>" +
                        "<div>Icon made by <a href=\"http://www.behance.net/Bart9339\" title=\"Pavel Kozlov\">Pavel Kozlov</a> from <a href=\"http://www.flaticon.com\" title=\"Flaticon\">www.flaticon.com</a> is licensed under <a href=\"http://creativecommons.org/licenses/by/3.0/\" title=\"Creative Commons BY 3.0\">CC BY 3.0</a></div>" +
                        "<div>Icons made by <a href=\"https://www.flaticon.com/authors/pixel-perfect\" title=\"Pixel perfect\">Pixel perfect</a> from <a href=\"https://www.flaticon.com/\" title=\"Flaticon\">www.flaticon.com</a> is licensed by <a href=\"http://creativecommons.org/licenses/by/3.0/\" title=\"Creative Commons BY 3.0\" target=\"_blank\">CC 3.0 BY</a></div>" +
                        "<div>Icons made by <a href=\"https://www.flaticon.com/authors/roundicons\" title=\"Roundicons\">Roundicons</a> from <a href=\"https://www.flaticon.com/\" title=\"Flaticon\">www.flaticon.com</a></div>" +
                        "<div>Icons made by <a href=\"https://www.flaticon.com/authors/good-ware\" title=\"Good Ware\">Good Ware</a> from <a href=\"https://www.flaticon.com/\" title=\"Flaticon\">www.flaticon.com</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/trash\" title=\"trash icons\">Trash icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/copy\" title=\"copy icons\">Copy icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/plus\" title=\"plus icons\">Plus icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/cursor\" title=\"cursor icons\">Cursor icons created by Pixel perfect - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/minus\" title=\"minus icons\">Minus icons created by Freepik - Flaticon</a></div>" +
                        "<div>https://www.pngegg.com/en/png-pjpul</div>" +
                        "<div><a href=\"https://www.flaticon.com/ru/free-icons/\" title=\"мозаика иконки\">Мозаика иконки от Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/opacity\" title=\"opacity icons\">Opacity icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/ui\" title=\"ui icons\">Ui icons created by shin_icons - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/liquid\" title=\"liquid icons\">Liquid icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/black-and-white\" title=\"black and white icons\">Black and white icons created by Ch.designer - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/reset\" title=\"reset icons\">Reset icons created by KP Arts - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/correct\" title=\"correct icons\">Correct icons created by Octopocto - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/close\" title=\"close icons\">Close icons created by Pixel perfect - Flaticon</a></div>" +
                        "<div>https://www.flaticon.com/free-icon/flip_4211752</div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/vibration\" title=\"vibration icons\">Vibration icons created by IconKanan - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/rounded-rectangle\" title=\"rounded rectangle icons\">Rounded rectangle icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/triangle\" title=\"triangle icons\">Triangle icons created by Roundicons Premium - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/oval\" title=\"oval icons\">Oval icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/download\" title=\"download icons\">Download icons created by Roundicons Premium - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/triangle\" title=\"triangle icons\">Triangle icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/rectangle\" title=\"rectangle icons\">Rectangle icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/globe\" title=\"globe icons\">Globe icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/image-comics\" title=\"Image Comics icons\">Image Comics icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/gallery\" title=\"gallery icons\">Gallery icons created by Nsu Rabo Elijah - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/selection\" title=\"selection icons\">Selection icons created by Pixel perfect - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/graduation-cap\" title=\"graduation cap icons\">Graduation cap icons created by Hilmy Abiyyu A. - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/information\" title=\"information icons\">Information icons created by Anggara - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/hand-pointer\" title=\"hand pointer icons\">Hand pointer icons created by Anggara - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/menu\" title=\"menu icons\">Menu icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/keyboard\" title=\"keyboard icons\">Keyboard icons created by Gregor Cresnar - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/font\" title=\"font icons\">Font icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/info\" title=\"info icons\">Info icons created by Chanut - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/question\" title=\"question icons\">Question icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/stamp\" title=\"stamp icons\">Stamp icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/feather\" title=\"feather icons\">Feather icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/alert\" title=\"alert icons\">Alert icons created by Freepik - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/gpu\" title=\"gpu icons\">Gpu icons created by yaicon - Flaticon</a></div>" +
                        "<div><a href=\"https://www.flaticon.com/free-icons/height\" title=\"height icons\">Height icons created by sonnycandra - Flaticon</a></div>" +
                        "<div></div>" +
                        "<div></div>" +
                        "<div></div>" +
                        "<div></div>" +
                        "<div></div>" +
                        "<div></div>"));
        textView.setLinksClickable(true);
        textView.setPadding(Tools.dp(10), Tools.dp(10), Tools.dp(10), Tools.dp(10));
        textView.setTextSize(12);
        textView.setTextColor(Color.WHITE);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setScrollContainer(true);

        ScrollView scrollView = new ScrollView(this);
        //scrollView.setBackgroundColor(Color.rgb(100,100,100));
        scrollView.addView(textView);

        Dialog dialog = new AlertDialog.Builder(this)
                .setView(scrollView)
                .setTitle("Copyright note")	// заголовок диалога
                .setPositiveButton("OK", null)
                .create();

        dialog.show();	// показываем диалог
    }
    void openPermissiongNote(){
        TextView textView = new TextView(AboutActivity.this);
        textView.setText(getString(R.string.permissionsNoteText));
        textView.setLinksClickable(true);
        textView.setPadding(Tools.dp(10), Tools.dp(10), Tools.dp(10), Tools.dp(10));
        textView.setTextSize(12);
        textView.setTextColor(Color.WHITE);
        textView.setScrollContainer(true);

        ScrollView scrollView = new ScrollView(this);
        //scrollView.setBackgroundColor(Color.rgb(100,100,100));
        scrollView.addView(textView);

        Dialog dialog = new AlertDialog.Builder(this)
                .setView(scrollView)
                .setTitle(R.string.aboutPermissionsNote)	// заголовок диалога
                .setPositiveButton("OK", null)
                .create();

        dialog.show();	// показываем диалог
    }
    void openEasterEgg(){
        startActivity(new Intent(this, EasterEggScreen.class));
    }
}
