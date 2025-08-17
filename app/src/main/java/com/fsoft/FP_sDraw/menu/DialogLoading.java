package com.fsoft.FP_sDraw.menu;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Tools;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 17.01.2015.
 */
public class DialogLoading{
    private final int backgroundColor = MenuPopup.menuBackgroundColor;
    private final Context _context;
    private final Dialog menu_dialog;
    private final LinearLayout _linearLayout;
    private final TextView _textView;
    private TextView _button;
    private final WaitingCircle _waWaitingCircle;
    private final DialogInterface.OnClickListener _cancelListener;

    public DialogLoading(Context in_context, int message){
        this(in_context, in_context.getString(message));
    }
    public DialogLoading(Context in_context, String message){
        this(in_context, null, null, message);
    }
    public DialogLoading(Context in_context, String buttonText, DialogInterface.OnClickListener cancelListener, String message){
        _context = in_context;
        _cancelListener = cancelListener;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(2,20,2,20);
        _linearLayout = new LinearLayout(in_context);
        _linearLayout.setBackgroundColor(backgroundColor);
        _linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        _linearLayout.setPadding(Tools.dp(15), Tools.dp(10), Tools.dp(15), Tools.dp(10));
        _waWaitingCircle = new WaitingCircle(_context);
        _waWaitingCircle.setLayoutParams(new ViewGroup.LayoutParams((int)Data.store().DPI / 3, (int)Data.store().DPI / 3));
        _linearLayout.addView(_waWaitingCircle);
        _textView = new TextView(in_context);
        _textView.setText(message);
        _textView.setTextSize(12);
        _textView.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams textLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);//(int)(new Paint().measureText(message)*3+Data.store().DPI/5)
        textLP.setMargins(2,10,2,10);
        _textView.setLayoutParams(textLP);
        _textView.setPadding(Tools.dp(10), 0, Tools.dp(10), 0);
        _textView.setGravity(Gravity.CENTER);
        _linearLayout.addView(_textView);
        if(buttonText != null && cancelListener != null){
            _button = new AnimatedButton(in_context);
            _button.setText(buttonText);
            _button.setOnClickListener(getCancelButtonListener());
            _linearLayout.addView(_button);
        }

        menu_dialog = new Dialog(in_context);
        menu_dialog.setCanceledOnTouchOutside(true);
        menu_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams wLayoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
        wLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wLayoutParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        wLayoutParams.horizontalMargin = 0f;
        wLayoutParams.verticalMargin = 0.1f;
        wLayoutParams.windowAnimations = R.style.DialogAnimation;
        wLayoutParams.gravity = Gravity.BOTTOM;// | Gravity.FILL_HORIZONTAL;
        menu_dialog.getWindow().setAttributes(wLayoutParams);
        menu_dialog.setContentView(_linearLayout);
        menu_dialog.setOnKeyListener(getOnKeyListener());
        menu_dialog.setCancelable(false);
    }
    public void show(){
        //dialog_loading.show();
        menu_dialog.show();
    }
    public void cancel(){
        //dialog_loading.dismiss();
        try {
            menu_dialog.cancel();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void setText(String text)
    {
        _textView.setText(text);
        //dialog_loading.setMessage(text);
    }
    private View.OnClickListener getCancelButtonListener(){
        return  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu_dialog.cancel();
                if(_cancelListener != null)
                    _cancelListener.onClick(null, 0);
            }
        };
    }
    private DialogInterface.OnKeyListener getOnKeyListener(){
        return new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_UP){
                    getCancelButtonListener().onClick(null);
                    return true;
                }
                return false;
            }
        };
    }

    private class WaitingCircle extends View{
        Context wc_context;
        Timer _timer;
        android.os.Handler _handler;
        int _width;
        int _height;
        int _desiredSize;
        int strokeSize = Tools.dp(4);
        int color = Color.WHITE;
        RectF circleRect;
        int degress = 0; //0...360
        int size = 0; //0...360
        Paint _paint = new Paint();

        public WaitingCircle(Context _wc_context){
            super(_wc_context);
            wc_context = _wc_context;
            _paint.setAntiAlias(true);
            _paint.setStyle(Paint.Style.STROKE);
            _paint.setColor(color);
            _paint.setStrokeWidth(strokeSize);
            _desiredSize = _context.getResources().getDisplayMetrics().densityDpi/2;
            _width = _height = _desiredSize;
        }
        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)  {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);

            height = width = (int)(Data.store().DPI * 0.3f);
//                if(getLayoutParams() != null) {
//                    height = Math.min(height, getLayoutParams().height);
//                    width = Math.min(height, getLayoutParams().width);
//
//                }else{
//                    height = width = Math.min(height, width);
//                }

            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
            );
        }
        @Override protected void onSizeChanged (int w, int h, int oldw, int oldh){
            super.onSizeChanged(w,h,oldw, oldh);
            _width = w;
            _height = h;
            strokeSize = w / 15;
            circleRect = new RectF(strokeSize*2, strokeSize*2, getWidth()-strokeSize*2, getHeight()-strokeSize*2);
        }
        @Override protected void onDraw(Canvas canvas){
            canvas.drawColor(backgroundColor);
            canvas.drawArc(circleRect, degress, size, false, _paint);
        }
        @Override protected void onAttachedToWindow (){
            super.onAttachedToWindow();
            _handler = new Handler();
            _timer = new Timer();
            _timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    step();
                    postInvalidate();
                }
            }, 0, 20);
        }
        @Override protected void onDetachedFromWindow (){
            super.onDetachedFromWindow();
            _timer.cancel();
        }

        boolean addSize = true;
        int sizeSpeed = 10;
        int moveSpeed = 20;
        void step(){
            if(addSize) {
                degress = addDegress(degress, moveSpeed - sizeSpeed);
                size += sizeSpeed;
                if(size > 300)
                    addSize = !addSize;
            }
            else{
                degress = addDegress(degress, moveSpeed + sizeSpeed);
                size -= sizeSpeed;
                if(size < 10)
                    addSize = !addSize;
            }
        }
        int addDegress(int orig, int add){
            int result = orig + add;
            while(result > 360){
                result -= 360;
            }
            return result;
        }
        int subDegress(int orig, int sub){
            int result = orig + sub;
            while(result < 0){
                result += 360;
            }
            return result;
        }
    }
}
