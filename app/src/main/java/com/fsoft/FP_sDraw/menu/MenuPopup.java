package com.fsoft.FP_sDraw.menu;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Tools;

/**
 * Created by Dr. Failov on 17.01.2015.
 */
public class MenuPopup extends Dialog {
    Activity context = null;
    MenuPopup menuPopup = this;
    LinearLayout linearLayout;
    public static int menuBackgroundColor = Color.rgb(39, 50, 56);
    static int menuTextColor = Color.rgb(255,255,255);
    public static int topStringColor = Color.rgb(57, 66, 73);
    ColorDrawable menuBackgroundDrawable = new ColorDrawable(menuBackgroundColor);
    int buttonColorActive = Color.argb(255, 60, 100, 200);
    int DPI = 0;
    String header = "Export image";
    int headerColor = Data.tools.getGridColor(topStringColor, 0.4f);

    public MenuPopup(Activity context) {
        super(context);
        this.context = context;

        //GET DPI
        DisplayMetrics dm = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);
        DPI = dm.densityDpi;
        setCanceledOnTouchOutside(true);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //noinspection ResourceType
        //getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams wLayoutParams =
                new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
        //wLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        //wLayoutParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        //wLayoutParams.horizontalMargin = 0f;
        //wLayoutParams.verticalMargin = 0f;
        wLayoutParams.windowAnimations = R.style.DialogAnimation;
        wLayoutParams.gravity = Gravity.BOTTOM;// | Gravity.FILL_HORIZONTAL;
        getWindow().setAttributes(wLayoutParams);
        getWindow().setBackgroundDrawable(menuBackgroundDrawable);

        linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        MenuPopup.TopString topString = new MenuPopup.TopString(context);
        linearLayout.addView(topString);
        linearLayout.setBackgroundDrawable(menuBackgroundDrawable);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(linearLayout);
        setContentView(scrollView);
    }

    public void setHeader(String header){
        this.header = header;
    }
    public void setHeaderColor(int color){
        headerColor = color;
    }
    public void addButton(String text, View.OnClickListener action){
        addButton(text, action, true);
    }
    public void addButton(String text, View.OnClickListener action, boolean autoClose){
        MenuPopup.myButton button1 = new MenuPopup.myButton(context, autoClose);
        if(text != null)
            button1.setText(text);
        if(action != null)
            button1.setOnClickListener(action);
        linearLayout.addView(button1);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Tools.dp(1)));
        frameLayout.setBackgroundColor(Color.argb(15, 255, 255, 255));
        linearLayout.addView(frameLayout);
    }
    public void addCustomView(View view){
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Tools.dp(10),Tools.dp(10),Tools.dp(10),Tools.dp(10));
        linearLayout.addView(view, lp);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Tools.dp(1)));
        frameLayout.setBackgroundColor(Color.argb(15, 255, 255, 255));
        linearLayout.addView(frameLayout);
    }
    public void addClassicButton(String text, View.OnClickListener action, boolean autoClose){
        AnimatedButton animatedButton = new AnimatedButton(context);
        animatedButton.setText(text);
        animatedButton.setPadding(Tools.dp(30), Tools.dp(10),Tools.dp(30), Tools.dp(10));
        animatedButton.setOnClickListener(view -> {
            if(action != null)
                action.onClick(view);
            if(autoClose)
                menuPopup.cancel();
        });

        FrameLayout frameLayout = new FrameLayout(context);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if(Build.VERSION.SDK_INT < 14){
            lp.height = Tools.dp(50);
        }
        lp.gravity = Gravity.END;
        lp.setMargins(Tools.dp(10),Tools.dp(10),Tools.dp(10),Tools.dp(10));
        frameLayout.addView(animatedButton, lp);
        linearLayout.addView(frameLayout);
    }
    public void addSeparator(){
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Tools.dp(1)));
        frameLayout.setBackgroundColor(Color.argb(15, 255, 255, 255));
        linearLayout.addView(frameLayout);
    }
    public MyCheckBox addCheckbox(String text, int image, boolean checked){
        MyCheckBox checkBox = new MyCheckBox(context, false);
        if(text != null)
            checkBox.setText(text);
        checkBox.setChecked(checked);
        checkBox.setOnClickListener(view -> { });
        //checkBox.setPadding(DPI , DPI / 16, DPI / 4, DPI / 16);
        checkBox.setImage(image);
        linearLayout.addView(checkBox);

        return checkBox;
    }


    public void addSpacer(){
        View view = new View(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Tools.dp(20));
        view.setLayoutParams(layoutParams);
        linearLayout.addView(view);
    }
    public void addLittleText(String string){
        TextView textView = new TextView(context);
        textView.setText(string);
        textView.setTextColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            textView.setAlpha(0.5f);
        textView.setTextSize(11);
        textView.setPadding(Tools.dp(10), Tools.dp(10), Tools.dp(10), Tools.dp(10));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayout.addView(textView, layoutParams);
    }
    class TopString extends View {
        int menuColor = menuBackgroundColor;
        int shadowSize = (int)(Data.store().DPI * 0.05f);
        Paint paint = new Paint();

        TopString(Context context) {
            super(context);
            setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, (int)Data.store().DPI / 4));
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(topStringColor);
            {//DRAW TEXT
                int top = 0;
                int bottom = getHeight() - shadowSize;
                int fund = shadowSize;
                int textSize = bottom - top - fund*2;
                int width = getWidth();
                paint.setAntiAlias(true);
                paint.setTextSize(textSize);
                paint.setColor(headerColor);
                float textWidth = paint.measureText(header);
                float textX = (width - textWidth)/2;
                float textY = bottom - fund*1.5f;
                canvas.drawText(header, textX, textY, paint);
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
    class myButton extends FrameLayout{
        private TextView textView = null;
        private final int colorBackground = Color.argb(0,0,0,0);
        private final int colorHovered = Color.argb(20, 255, 255, 255);
        private final int colorPressed = Color.argb(90, 255, 255, 255);
        private final boolean autoCloseMenu;

        public myButton(Context context) {
            this(context, true);
        }
        public myButton(Context context, boolean autoCloseMenu) {
            super(context);
            this.autoCloseMenu = autoCloseMenu;
            textView = new TextView(context);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(16);
            //textView.setText("Menu item");

            setPadding(Tools.dp(15), Tools.dp(13), Tools.dp(15), Tools.dp(13));
            addView(textView);
        }

        @Override
        public void setPressed(boolean pressed) {
            super.setPressed(pressed);
            setBackgroundColor(pressed?colorPressed:colorBackground);
        }

        @Override
        public void setHovered(boolean hovered) {
            super.setHovered(hovered);
            setBackgroundColor(hovered?colorHovered:colorBackground);
        }

        public void setText(String text){
            textView.setText(text);
        }

        @Override
        public boolean performClick() {
            if(autoCloseMenu)
                menuPopup.cancel();
            return super.performClick();
        }
    }
}
