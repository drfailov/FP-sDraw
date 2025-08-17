package com.fsoft.FP_sDraw;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;

/**
 * Created with Android Studio
 * User: Dr. Failov
 * Date: 15.04.21
 * Time: 19:15

 * Формат тени для иконок:
 * Иконка 64х64, расширяется до 90х90
 * Тень, 0,0,0, непрозрачность 30%
 * смешение 2 пикселя вниз
 * размах 0
 * размер 20
 */

public class TextInput extends Activity {
    static public Bitmap initBitmapBackground = null; //Какая картинка будет отображаться в качестве вона при вводе
    public interface OnApply{
        void apply(Rect undoRect);
    }

    private String text = "";

    @SuppressWarnings("FieldCanBeLocal")
    private final int topButtonsTextSize = 17; //sp
    //edit text screen
    private EditText editText = null;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
        if(getIntent() != null && getIntent().hasExtra("text"))
            text = getIntent().getStringExtra("text");

        setEditView();
    }
    void setEditView(){

        editText = new EditText(this);
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setTextColor(Color.WHITE);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setHint(R.string.text_input_hint);
        editText.setTextSize(25); //sp
        editText.setGravity(Gravity.CENTER);
        editText.setText(text);
        editText.setSelection(editText.getText().length());

        TextView cancelButtonText = new TextView(this);
        cancelButtonText.setTextColor(Color.WHITE);
        cancelButtonText.setTextSize(topButtonsTextSize);//sp
        cancelButtonText.setPadding(Tools.dp(7),0, Tools.dp(0), Tools.dp(2));
        cancelButtonText.setText(R.string.cancel);

        ImageView cancelButtonImage = new ImageView(this);
        cancelButtonImage.setImageResource(R.drawable.ic_cancel);
        cancelButtonImage.setBackgroundColor(Color.TRANSPARENT);
        cancelButtonImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout cancelButtonLinear = new LinearLayout(this);
        cancelButtonLinear.setOrientation(LinearLayout.HORIZONTAL);
        cancelButtonLinear.setVerticalGravity(Gravity.CENTER_VERTICAL);
        cancelButtonLinear.setBackgroundResource(R.drawable.highlighting_background);
        cancelButtonLinear.setPadding(Tools.dp(15),Tools.dp(15),Tools.dp(15),Tools.dp(15));
        cancelButtonLinear.addView(cancelButtonImage, new LinearLayout.LayoutParams(Tools.dp(25), Tools.dp(25)));
        cancelButtonLinear.addView(cancelButtonText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        cancelButtonLinear.setOnClickListener(v -> {
            Tools.vibrate(v);
            setResult(RESULT_CANCELED);
            finish();
        });

        TextView nextButtonText = new TextView(this);
        nextButtonText.setTextColor(Color.WHITE);
        nextButtonText.setTextSize(topButtonsTextSize);//sp
        nextButtonText.setPadding(0,0,Tools.dp(7), Tools.dp(2));
        nextButtonText.setText(R.string.text_input_next);

        ImageView nextButtonImage = new ImageView(this);
        nextButtonImage.setImageResource(R.drawable.ic_next);
        nextButtonImage.setBackgroundColor(Color.TRANSPARENT);
        nextButtonImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout nextButtonLinear = new LinearLayout(this);
        nextButtonLinear.setOrientation(LinearLayout.HORIZONTAL);
        nextButtonLinear.setVerticalGravity(Gravity.CENTER_VERTICAL);
        nextButtonLinear.setBackgroundResource(R.drawable.highlighting_background);
        nextButtonLinear.setPadding(Tools.dp(15),Tools.dp(15),Tools.dp(15),Tools.dp(15));
        nextButtonLinear.addView(nextButtonText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        nextButtonLinear.addView(nextButtonImage, new LinearLayout.LayoutParams(Tools.dp(25), Tools.dp(25)));
        nextButtonLinear.setOnClickListener(v -> {
            if(editText == null)
                return;
            text = editText.getText().toString();
            if(text.trim().length() > 0){
                Intent dataIntent = new Intent();
                dataIntent.putExtra("text", text);
                setResult(RESULT_OK, dataIntent);
                Tools.vibrate(v);
                finish();
            }
                //setMoveView();
        });

        BackgroundView backgroundView = new BackgroundView(this);


        RelativeLayout relativeLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        relativeLayout.addView(backgroundView, lp);
        lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp.addRule(RelativeLayout.CENTER_VERTICAL);
        relativeLayout.addView(editText, lp);
        lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeLayout.addView(cancelButtonLinear, lp);
        lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeLayout.addView(nextButtonLinear, lp);

        setContentView(relativeLayout);
        //show keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        editText.post(() -> {
            if (editText.requestFocus()) {
                try {
                    InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) { //чтобы после входа на экран ввода жестом не регулировалась громкость резко
        if(!(Boolean)Data.get(Data.volumeButtonsBoolean()))  //если обработка клавиш отключена - не обрабатываем
            return super.dispatchKeyEvent(event);
        return (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) || super.dispatchKeyEvent(event);
    }

    private static class BackgroundView extends View{
        private final Matrix backgroundMatrix;
        private final Paint backgroundPaint;

        public BackgroundView(Context context){
            super(context);
            backgroundMatrix = new Matrix();
            backgroundPaint = new Paint();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if(initBitmapBackground != null)
                canvas.drawBitmap(initBitmapBackground, backgroundMatrix, backgroundPaint);
            canvas.drawColor(Color.argb(180, 0,0,0));
        }
    }
}
