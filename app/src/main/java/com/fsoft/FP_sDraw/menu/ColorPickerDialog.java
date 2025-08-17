package com.fsoft.FP_sDraw.menu;

import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;

public class ColorPickerDialog extends Dialog {
    public interface OnColorChangedListener {
        void colorChanged(int color);
    }
    public Dialog dialog = this;

    private final OnColorChangedListener finalListener;
    private final int defaultColor;
    private final Context context;
    private final int DPI;

    private final float[] HSV; //   0 hue(0...360); 1 saturation(0...1); 2 - value(0...1)
    HueSelector hueSelector;
    ValueSelector valueSelector;
    ColorPreview defaultPreview;
    ColorPreview newPreview;
    EditText codeTextView;


    public ColorPickerDialog(Context in_context, OnColorChangedListener in_listener, int in_defaultColor) {
        super(in_context);
        context = in_context;
        finalListener = in_listener;
        defaultColor = in_defaultColor;

        HSV = new float[3];
        Color.colorToHSV(defaultColor, HSV);

        DPI = context.getResources().getDisplayMetrics().densityDpi;
    }
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        hueSelector = new HueSelector(context);
        valueSelector = new ValueSelector(context);
        defaultPreview = new ColorPreview(context, defaultColor);
        newPreview = new ColorPreview(context, defaultColor);
        codeTextView = new EditText(context);

        //MAIN LAYOUT
        LinearLayout mainLinear = new LinearLayout(context);
        mainLinear.setBackgroundColor(Color.rgb(39, 50, 56));
        mainLinear.setOrientation(LinearLayout.VERTICAL);
        mainLinear.setPadding(Tools.dp(10), Tools.dp(10), Tools.dp(10), Tools.dp(10));
        mainLinear.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        //CODE TEXT
        FrameLayout topFrame = new FrameLayout(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        topFrame.setLayoutParams(lp);
        LinearLayout topLinear = new LinearLayout(context);
        topLinear.setOrientation(LinearLayout.HORIZONTAL);
        topLinear.setGravity(Gravity.CENTER);
        topFrame.addView(topLinear);
        topLinear.setPadding(0, Tools.dp(10), 0, Tools.dp(2));

        codeTextView.setText(getCode(defaultColor));
        //codeTextView.setInputType(InputType.TYPE_CLASS_TEXT);


        //Assign a TextWatcher to the EditText
        codeTextView.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,int after){}

            //Right after the text is changed
            @Override
            public void afterTextChanged(Editable s)
            {
                //Store the text on a String
                String text = s.toString();

                //Get the length of the String
                int length = s.length();

                /*If the String length is bigger than zero and it's not
                composed only by the following characters: A to F and/or 0 to 9 */
                if(!text.matches("[a-fA-F0-9#]+") && length > 0)
                {
                    //Delete the last character
                    s.delete(length - 1, length);
                }

                while(s.length() > 7)
                {
                    s.delete(s.length() - 2, s.length() - 1);
                }
            }
        });
        codeTextView.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        // Identifier of the action. This will be either the identifier you supplied,
                        // or EditorInfo.IME_NULL if being called due to the enter key being pressed.
                        if (actionId == EditorInfo.IME_ACTION_SEARCH
                                || actionId == EditorInfo.IME_ACTION_DONE
                                || event.getAction() == KeyEvent.ACTION_DOWN
                                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

                            try {
                                String data = codeTextView.getText().toString();
                                data = data.replace("#", "");
                                data = "#" + data;
                                if (data.length() == 7) {
                                    int color = Color.parseColor(data);
                                    Color.colorToHSV(color, HSV);
                                    invalidateWindow();
                                }
                            } catch (Exception e) {//5655
                            }

                            return true;
                        }
                        // Return true if you have consumed the action, else false.
                        return false;
                    }
                });
        codeTextView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        codeTextView.setGravity(Gravity.CENTER);
        codeTextView.setTextSize(10);
        codeTextView.setPadding(Tools.dp(10), Tools.dp(8), Tools.dp(10), Tools.dp(8));
        //codeTextView.setPadding(0,0,0,0);
        codeTextView.setBackgroundColor(Color.argb(50, 255, 255, 255));
        codeTextView.setTextColor(Color.WHITE);
        codeTextView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topLinear.addView(codeTextView);

        TextView button3 = new AnimatedButton(context);
        button3.setText(R.string.applyCode);//apply
        button3.setTextSize(10);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        lp1.setMargins(Tools.dp(10), 0, 0, 0);
        button3.setLayoutParams(lp1);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String data = codeTextView.getText().toString();
                    data = data.replace("#", "");
                    data = "#" + data;
                    if (data.length() == 7) {
                        int color = Color.parseColor(data);
                        Color.colorToHSV(color, HSV);
                        invalidateWindow();
                    }
                } catch (Exception e) {//5655
                }
            }
        });
        topLinear.addView(button3);
        //VALUE FRAME
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        //INNER LAYOUT
        LinearLayout firstLinear = new LinearLayout(context);
        firstLinear.setOrientation(LinearLayout.HORIZONTAL);
        firstLinear.setGravity(Gravity.CENTER);
        firstLinear.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        frameLayout.addView(firstLinear);
        //VALUE SELECTOR
        LinearLayout.LayoutParams valueLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        valueLP.rightMargin = DPI/15;
        valueSelector.setLayoutParams(valueLP);
        firstLinear.addView(valueSelector);
        //HUE SELECTOR
        hueSelector.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT));
        firstLinear.addView(hueSelector);

        //BUTTONS
        LinearLayout buttonLinear = new LinearLayout(context);
        buttonLinear.setPadding(0, Tools.dp(2), 0, 0);
        buttonLinear.setOrientation(LinearLayout.HORIZONTAL);
        buttonLinear.setGravity(Gravity.CENTER);
        buttonLinear.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        //BUTTON2
        TextView button2 = new AnimatedButton(context);
        button2.setText(Data.tools.getResource(R.string.ColorPickerClose));//apply
        button2.setTextSize(10);
        button2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        buttonLinear.addView(button2);
        //PREVIEW
        LinearLayout.LayoutParams previewLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        previewLP.rightMargin=Tools.dp(5);
        previewLP.leftMargin=Tools.dp(5);

        defaultPreview.setLayoutParams(previewLP);
        buttonLinear.addView(defaultPreview);

        newPreview.setLayoutParams(previewLP);
        buttonLinear.addView(newPreview);
        //BUTTON1
        TextView button1 = new AnimatedButton(context);
        button1.setText(Data.tools.getResource(R.string.ColorPickerApply));//apply
        button1.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        button1.setTextSize(10);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finalListener.colorChanged(Color.HSVToColor(HSV));
                dialog.cancel();
            }
        });
        buttonLinear.addView(button1);

        //FINISH
        mainLinear.addView(frameLayout);
        mainLinear.addView(topFrame);
        mainLinear.addView(buttonLinear);
        setContentView(mainLinear);
        //setTitle(name);

    }
    public String getCode(int color){
        return String.format("#%06X", (0xFFFFFF & color));
    }
    public void invalidateWindow(){
        hueSelector.invalidate();
        valueSelector.invalidate();
        defaultPreview.setColor(defaultColor);
        int color = Color.HSVToColor(HSV);
        newPreview.setColor(color);
        codeTextView.setText(getCode(color));
    }
    private class HueSelector extends View{
        Bitmap cache;
        Paint paint;

        public HueSelector(Context in_context){
            super(in_context);
        }
        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)  {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);

            if (width > DPI/4) {
                width = DPI/4;
            }

            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
            );
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE){
                //if(event.getY() >= 0 && event.getY() < getHeight() && event.getX() >= 0 && event.getX() < getWidth())
                float y = event.getY();
                if(y < 0)
                    y = 0;
                if(y > getHeight() - 1)
                    y = getHeight()-1;
                HSV[0] = (y * 360) / getHeight();
            }
            invalidateWindow();
            return true;
        }
        @Override protected void onDraw(Canvas canvas){
            if(cache != null){
                canvas.drawBitmap(cache, 0, 0, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.BLACK);
                int gap = DPI/100 < 1 ? 1 : DPI/100;
                paint.setStrokeWidth(gap);//чтобы всегда было больше нуля
                int position = (int)((HSV[0] * getHeight() ) / 360f);
                canvas.drawRect(gap + 1, position - gap, getWidth() - 2 - gap, position + gap, paint);
            }
            else {
                //canvas.drawColor(Color.GREEN);
                initBitmap();
            }
        }
        void initBitmap(){
            try{
                cache = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                paint = new Paint();
                paint.setStrokeWidth(1);
                Canvas canvas = new Canvas(cache);
                float[] hsv = new float[3];
                hsv[1] = 1;
                hsv[2] = 1;
                for(int i=0; i< getHeight(); i++){
                    hsv[0] = ((float)i*360f)/(float)getHeight();
                    paint.setColor(Color.HSVToColor(hsv));
                    canvas.drawLine(0, i, getWidth(), i, paint);
                }
                invalidate();
            }catch (Exception | OutOfMemoryError e) {
                Logger.log("Error: " + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
        }
    }
    private class ValueSelector extends View{
        Bitmap cache;
        Paint paint;
        Matrix matrix;
        float cachedHue = -1;
        int previewSize;

        @Override public boolean onTouchEvent(MotionEvent event){
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE){
                float y = event.getY();
                if(y < 0)
                    y = 0;
                if(y > getHeight() - 1)
                    y = getHeight()-1;

                float x = event.getX();
                if(x < 0)
                    x = 0;
                if(x > getWidth() - 1)
                    x = getWidth()-1;

                HSV[1] = x / getWidth();
                HSV[2] = y / getHeight();
            }
            invalidateWindow();
            return true;
        }
        public ValueSelector(Context in_context){
            super(in_context);
        }
        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)  {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);

            if (height > width) {
                height = width;
            } else {
                width = height;
            }

            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
            );
        }
        @Override protected void onDraw(Canvas canvas){
            if(cache == null || cachedHue != HSV[0]){
                initBitmap();
            }
            canvas.drawBitmap(cache, matrix, paint);

            paint.setColor(HSV[2] < 0.4f ? Color.WHITE : Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(DPI / 100);
            canvas.drawCircle(HSV[1] * getWidth(), HSV[2] * getHeight(), DPI/20, paint);
        }
        void initBitmap(){
            try{
                //INIT
                previewSize = Math.min(getHeight(), 50);
                if(cache == null)
                    cache = Bitmap.createBitmap(previewSize, previewSize, Bitmap.Config.ARGB_8888);
                paint = new Paint();

                //RENDER IMAGE
                float[] hsv = new float[3] ;
                hsv[0] = HSV[0];
                for(int i=0; i < previewSize; i++){
                    for(int j=0; j < previewSize; j++){
                        hsv[1] = (float)i / (float)previewSize;//saturation
                        hsv[2] = (float)j / (float)previewSize;//value
                        cache.setPixel(i, j, Color.HSVToColor(hsv));
                    }
                }
                cachedHue = HSV[0];

                //PREPARE TO DRAW
                if(matrix == null)
                    matrix = new Matrix();
                else
                    matrix.reset();
                matrix.postScale(getWidth()/(float)cache.getWidth(), getHeight()/(float)cache.getHeight());
            }catch (Exception | OutOfMemoryError e) {
                Logger.log("Error: " + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
        }
    }
    private class ColorPreview extends View{
        int color = Color.BLUE;

        public ColorPreview(Context in_context, int in_color){
            super(in_context);
            color = in_color;
        }

        public void setColor(int in_color){
            color = in_color;
            invalidate();
        }

        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)  {
            int height = DPI/7;
            int width = DPI/7;

            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            );
        }

        @Override protected void onDraw(Canvas canvas){
            canvas.drawColor(color);
        }
    }
}