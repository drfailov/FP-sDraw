package com.fsoft.FP_sDraw;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.FrameLayout;

import com.fsoft.FP_sDraw.common.*;
import com.fsoft.FP_sDraw.menu.DialogLoading;
import com.fsoft.FP_sDraw.menu.FloatingMenu;

import java.io.InputStream;

/**
 * Экран позволяющий позиционировать вставку изображения

 * ОБЯЗАТЕЛЬНО! Перед вызовом заполнить поле initBitmap;

 * Также функцию отмен там всяких надо применять в интерфейсе
 * Created with IntelliJ IDEA.
 * User: Dr. Failov
 * Date: 07.06.13
 * Time: 16:38
 */
public class ImageInserter extends Activity {

    static public Bitmap initCanvasBitmap = null;
    static public OnApply initListener ;

    interface OnApply{
        void apply(Rect rectOnUndo);
    }


    private Uri fileUri = null;         //URI of file we're inserting
    private Bitmap canvasBitmap = null;     //Bitmap which will be modified   (got from MainActibity by static members)
    private OnApply applyListener = null;   //action which be called when user click "Apply"
    private final Handler handler = new Handler();
    private DialogLoading dialogLoading = null;
    private PreviewView preview; //object which draws everything in this activity
    private BitmapFactory.Options imageParameters = null;
    private Pinch pinch = null;
    private Move move = null;
    private final Crop crop = new Crop();
    private FloatingMenu floatingMenu;
    private FloatingMenu.FloatingMenuButton buttonRemoveBackground;
    private FloatingMenu.FloatingMenuButton buttonBinarizeImage;
    private FloatingMenu.FloatingMenuSlider sliderOpacity;
    private FloatingMenu.FloatingMenuSlider sliderBackgroundDetectionStrength;
    private FloatingMenu.FloatingMenuSlider sliderBinarizeCoefficient;


    //data used to draw
    private Bitmap previewBitmap = null;  //Bitmap with small size of image what we're inserting
    private Bitmap previewCroppedBitmapCache = null;
    private final RectF previewCroppedBitmapCoefs = new RectF();
    private Bitmap previewBitmapWithNoModifications = null; //when background is removed here will be stored original one
    private Thread previewBitmapLoadingThread = null;
    private PointF previewSize = new PointF(0,0);  //size transformation
    private PointF previewPosition = new PointF(0,0);  //move transformation
    private final RectF previewCropCoefficient = new RectF(0.001f,0.001f,0.001f,0.001f); //which part (from 0 to 1) is cropped from every side
    private float previewRotationRad = 0;    //rotation transformation, radians
    private float backgroundDetectionStrength = 32; //10 - 200 //for removing background
    private int previewOpacity = 255; //0...255
    private int binarizeMode = BitmapBinarizer.MODE_NONE;
    private int binarizeCoefficient = 128;



    @SuppressLint({"RtlHardcoded", "SetTextI18n", "SourceLockedOrientationActivity"})
    @Override protected void onCreate(final Bundle bundle){
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


            if(initListener == null)
                Logger.log("There is no action received! Did you forgot to fill static fields before calling???");
            if(initCanvasBitmap == null)
                return;
            canvasBitmap = initCanvasBitmap;
            initCanvasBitmap = null;
            Logger.log("Image Inserter: canvasBitmap=" + canvasBitmap);
            applyListener = initListener;
            initListener = null;

            //workaround for cutout areas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                getWindow().setAttributes(layoutParams);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }

            //сделать так чтобы было красивенько
            requestWindowFeature(Window.FEATURE_NO_TITLE);  //убрать панель уведомлений
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);   //убрать панель названия

            //load info
            //filename = getIntent().getStringExtra("file");
            fileUri = Uri.parse(getIntent().getStringExtra("fileUri"));

            preview = new PreviewView(this);
            preview.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            FrameLayout frameLayout = new FrameLayout(this);
            frameLayout.addView(preview);

            floatingMenu = new FloatingMenu(preview);
            floatingMenu.addButton(R.drawable.ic_fit_image, Data.tools.getResource(R.string.insert_fit), this::fitInside, true);
            floatingMenu.addButton(R.drawable.ic_one_to_one, Data.tools.getResource(R.string.scale_one_to_one), this::onOneToOnePressed, true);
            buttonRemoveBackground = floatingMenu.addButton(R.drawable.ic_remove_background, Data.tools.getResource(R.string.deleteBackground), this::onRemoveBackgroundClick, true);
            buttonBinarizeImage = floatingMenu.addButton(R.drawable.ic_black_and_white, getString(R.string.binarize_image), this::onBinarizeImageClick, true);
            floatingMenu.addButton(R.drawable.ic_copy, Data.tools.getResource(R.string.copy), ()->OK(false, true), true);
            floatingMenu.addButton(R.drawable.ic_check, Data.tools.getResource(R.string.apply), ()->OK(true, false), true);

            sliderOpacity = floatingMenu.addSlider(R.drawable.ic_opacity, previewOpacity,1, 255, this::opacityChanged, this::opacityChanged, true);
            sliderBackgroundDetectionStrength = floatingMenu.addSlider(R.drawable.ic_remove_background, (int)backgroundDetectionStrength,10, 200, null, this::backgroundDetectionStrengthChanged,false);
            sliderBinarizeCoefficient = floatingMenu.addSlider(R.drawable.ic_black_and_white, binarizeCoefficient,-240, 240, null, this::binarizeCoefficientChanged,false);


            setContentView(frameLayout);
            redraw();
        }
        catch (Throwable e){
            Logger.log("Где-то в ImageInserter.onCreate произошла ошибка ",e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if(canvasBitmap != null)
            initCanvasBitmap = canvasBitmap;
        if(applyListener != null)
            initListener = applyListener;
        super.onSaveInstanceState(outState);
    }

    void redraw(){
        preview.invalidate();
    }
    private Rect rectOfUndo(){

        //crop
        PointF dCut = new PointF(previewSize.x * previewCropCoefficient.left, previewSize.y * previewCropCoefficient.top);
        dCut = rotateVector(dCut, magnetRad(previewRotationRad));
        //calculate area which need to be saved as undo area
        float rcx = previewPosition.x + dCut.x;// + previewCropCoefficient.left*previewSize.x;
        float rcy = previewPosition.y + dCut.y;
        PointF dTL = new PointF(previewSize.x - (previewSize.x*(previewCropCoefficient.right+previewCropCoefficient.left)), 0);
        PointF dBL = new PointF(previewSize.x - (previewSize.x*(previewCropCoefficient.right+previewCropCoefficient.left)), previewSize.y  - (previewSize.y*(previewCropCoefficient.top+previewCropCoefficient.bottom)));
        PointF dBR = new PointF(0, previewSize.y  - (previewSize.y*(previewCropCoefficient.top+previewCropCoefficient.bottom)));
        dTL = rotateVector(dTL, magnetRad(previewRotationRad));
        dBL = rotateVector(dBL, magnetRad(previewRotationRad));
        dBR = rotateVector(dBR, magnetRad(previewRotationRad));
        dTL.set(dTL.x+rcx, dTL.y+rcy);
        dBL.set(dBL.x+rcx, dBL.y+rcy);
        dBR.set(dBR.x+rcx, dBR.y+rcy);
        float padding = Tools.dp(10);
        Rect rectOfUndo = new Rect();
        rectOfUndo.top = (int)(Math.min(Math.min(rcy, dTL.y), Math.min(dBL.y, dBR.y)) - padding);
        rectOfUndo.bottom = (int)(Math.max(Math.max(rcy, dTL.y), Math.max(dBL.y, dBR.y)) + padding);
        rectOfUndo.right = (int)(Math.max(Math.max(rcx, dTL.x), Math.max(dBL.x, dBR.x)) + padding);
        rectOfUndo.left = (int)(Math.min(Math.min(rcx, dTL.x), Math.min(dBL.x, dBR.x)) - padding);
        return rectOfUndo;
    }
    void OK(boolean finishAfterComplete, boolean shiftAfterComplete){
        dialogLoading = new DialogLoading(this, R.string.inserting);
        dialogLoading.show();
        Tools.vibrate(preview);
        preview.playSoundEffect(SoundEffectConstants.CLICK);
        new Thread(() -> OkAsync(finishAfterComplete, shiftAfterComplete)).start();
    }
    void OkAsync(boolean finishAfterComplete, boolean shiftAfterComplete) { //this function is called by OK()
        try{
            if(canvasBitmap != null && previewBitmap != null) {
                //aaply here
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                {
                    InputStream ims = getBaseContext().getContentResolver().openInputStream(fileUri);
                    BitmapFactory.decodeStream(ims, null, options);
                    ims.close();
                }
                //BitmapFactory.decodeFile(filename, options);
                // Calculate inSampleSize
                float fileWidth = options.outWidth;
                float fileHeight = options.outHeight;
                float loadSizeX = previewSize.x;
                float loadSizeY = previewSize.y;

                if(previewSize.x > fileWidth || previewSize.y > fileHeight ) {
                    loadSizeX = fileWidth;
                    loadSizeY = fileHeight;
                }

                //load and draw
                //if error retry
                int retries = 0;

                while(true){
                    try {
                        Logger.log("ImageInserter", "Decoding " + loadSizeX + "x" + loadSizeY + "...", false);
                        Bitmap releaseBitmap = Tools.decodeFile(getBaseContext(), fileUri, (int) loadSizeX, (int) loadSizeY);
                        if(releaseBitmap == null){
                            loadSizeX /= 2;
                            loadSizeY /= 2;
                            retries ++;
                            continue;
                        }
                        if(buttonRemoveBackground != null && buttonRemoveBackground.highlighted)
                            releaseBitmap = new BackgroundRemover(backgroundDetectionStrength, previewCropCoefficient).removeBackgroundOnFrame(releaseBitmap);
                        if(buttonBinarizeImage != null && buttonBinarizeImage.highlighted)
                            releaseBitmap = BitmapBinarizer.doWorkAsync(releaseBitmap, binarizeCoefficient, binarizeMode);
                        synchronized (DrawCore.drawSync) {
                            Canvas canvas = new Canvas(canvasBitmap);
                            Paint paint = new Paint();
                            paint.setAntiAlias(false);
                            paint.setFilterBitmap(previewSize.x / (float) previewBitmap.getWidth() < 25); //don't filter if user wants to play pixel-art
                            paint.setAlpha(255);
                            Matrix imageMatrix = new Matrix();

                            //DRAW IMAGE
                            //prepare rotation
                            canvas.save();
                            canvas.rotate((float) Math.toDegrees(magnetRad(previewRotationRad)), previewPosition.x, previewPosition.y);
                            //draw
                            imageMatrix.reset();
                            imageMatrix.setScale(previewSize.x / (float) releaseBitmap.getWidth(), previewSize.y / (float) releaseBitmap.getHeight());
                            imageMatrix.postTranslate(Math.round(previewPosition.x), Math.round(previewPosition.y));
                            //DRAW CROPPED BITMAP
                            paint.setAlpha(previewOpacity);
                            canvas.drawBitmap(Tools.cropBitmap(releaseBitmap, previewCropCoefficient), imageMatrix, paint);
                            //canvas.drawBitmap(releaseBitmap, imageMatrix, paint);

                            //restore canvas
                            canvas.restore();
                        }
                        break;
                    }
                    catch (OutOfMemoryError e){
                        e.printStackTrace();
                        Logger.log("ImageInserter", "OUT OF MEMORY", false);

                        System.gc();
                        loadSizeX /= 2;
                        loadSizeY /= 2;

                        retries ++;
                        if(retries > 5)
                            throw new OutOfMemoryError();
                    }
                }
                handler.post(() -> {
                    try {
                        if(applyListener != null)
                            applyListener.apply(rectOfUndo());
                        if (dialogLoading != null) {
                            dialogLoading.cancel();
                            dialogLoading = null;
                        }
                        if(finishAfterComplete)
                            finish();
                        if(shiftAfterComplete) {
                            previewPosition.x += Tools.dp(20);
                            previewPosition.y += Tools.dp(20);
                            if(preview != null)
                                preview.postInvalidate();
                        }

                    }
                    catch (Throwable e) {
                        Logger.log("Error in ImageInserter.apply(...): " + Tools.getStackTrace(e));
                        e.printStackTrace();
                    }
                });
            }
            else {
                if(dialogLoading != null) {
                    dialogLoading.cancel();
                    dialogLoading = null;
                }
                Logger.log("ImageInserter.OK", "кажется, у нас проблемы... Холст исчез!!!", true);
                Logger.show(Data.tools.getResource(R.string.errorWhileInserting));
            }
        }
        catch (Throwable e){
            if(dialogLoading != null) {
                dialogLoading.cancel();
                dialogLoading = null;
            }
            Logger.log("Где-то в ImageInserter.OK произошла ошибка ",e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            Logger.show(Data.tools.getResource(R.string.errorWhileInserting));
        }
    }
    private float degree(float rad){
        return (float)Math.toDegrees(rad);
    }
    private float rad(float degree){
        return (float)Math.toRadians(degree);
    }
    private float mod360(float degree){ //0-360
        while(degree < 0)
            degree += 360;
        return degree % 360;
    }
    private float magnetDegree(float degree){
        //0 ... 90 ... 180 ... 270 ... 0 ... 90
        //  45     135     255     315   45
        //if(degree > 315 && degree <= 45 ) return 0;
        if(degree > 45  && degree <= 135) return 90;
        if(degree > 135  && degree <= 255) return 180;
        if(degree > 255  && degree <= 315) return 270;
        return 0;
    }
    @SuppressWarnings("SuspiciousNameCombination")
    private void fitInside(){
        float degree = degree(previewRotationRad);
        degree = mod360(degree);
        degree = magnetDegree(degree);

        float imageAspect = previewSize.x / previewSize.y;
        float imageAspectIncludingRotation = previewSize.x / previewSize.y;
        if(degree == 90 || degree == 270)
            imageAspectIncludingRotation = previewSize.y / previewSize.x;
        float windowWidth = preview.getWidth();
        float windowHeight = preview.getHeight();
        float windowAspect = windowWidth / windowHeight;
        float imageHeight = 10;
        float imageWidth = 10;
        float imageX = imageWidth/2;
        float imageY = imageHeight/2;

        if(imageAspectIncludingRotation < windowAspect){
            //высокое изображение. Выше, чем экран.  вписать по высоте.
            //вычислить размер
            imageHeight = windowHeight;
            imageWidth = imageHeight * imageAspect;
            if(degree == 90 || degree == 270) {
                imageWidth = windowHeight;
                imageHeight = imageWidth / imageAspect;
            }
        }
        else{
            //Широкое изображение. Шире, чем экран. Вписать по ширине.
            //вычислить размер
            imageWidth = windowWidth;
            imageHeight = imageWidth / imageAspect;
            if(degree == 90 || degree == 270) {
                imageHeight = windowWidth;
                imageWidth = imageHeight * imageAspect;
            }
        }
        //вычислить положение
        if(degree == 0) {
            imageX = (windowWidth - imageWidth) / 2;
            imageY = (windowHeight - imageHeight) / 2;
        }
        if(degree == 90){
            imageX = (windowWidth + imageHeight) / 2;
            imageY = (windowHeight - imageWidth) / 2;
        }
        if(degree == 180) {
            imageX = (windowWidth + imageWidth) / 2;
            imageY = (windowHeight + imageHeight) / 2;
        }
        if(degree == 270){
            imageX = (windowWidth - imageHeight) / 2;
            imageY = (windowHeight + imageWidth) / 2;
        }
        //применить
        previewSize.set(imageWidth, imageHeight);
        previewPosition.set(imageX, imageY);
        previewRotationRad = rad(degree);
        redraw();
    }
    private void opacityChanged(){
        previewOpacity = (int)sliderOpacity.getValue();
    }
    private void backgroundDetectionStrengthChanged(){
        if(sliderBackgroundDetectionStrength != null)
            backgroundDetectionStrength = sliderBackgroundDetectionStrength.getValue();
        BackgroundRemover backgroundRemover = new BackgroundRemover(ImageInserter.this, (bitmap,color) -> {
            ImageInserter.this.previewBitmap = bitmap;
            previewCroppedBitmapCache=null;
            redraw();
        }, () -> {
            buttonRemoveBackground.highlighted = false;
            sliderBackgroundDetectionStrength.visible = false;
            Logger.show(getString(R.string.errorWhileInserting));
            redraw();
        }, previewBitmapWithNoModifications);
        backgroundRemover.setCropCoefficient(previewCropCoefficient);
        backgroundRemover.setBackgroundDetectionStrength(backgroundDetectionStrength);
        backgroundRemover.startOnFrame();
    }

    private void onOneToOnePressed(){
        if(imageParameters != null) {
            previewSize.set(imageParameters.outWidth, imageParameters.outHeight);
            preview.postInvalidate();
        }
    }

    private void onRemoveBackgroundClick(){
        try {
            if(buttonRemoveBackground == null)
                return;
            if(buttonBinarizeImage == null)
                return;
            if(previewBitmapWithNoModifications == null)
                return;
            if(sliderBackgroundDetectionStrength == null)
                return;
            buttonRemoveBackground.highlighted = !buttonRemoveBackground.highlighted;
            sliderBackgroundDetectionStrength.visible = buttonRemoveBackground.highlighted;
            buttonBinarizeImage.highlighted = false;
            buttonBinarizeImage.visible = !buttonRemoveBackground.highlighted;
            sliderBinarizeCoefficient.visible = false;
            if (buttonRemoveBackground.highlighted) {
                BackgroundRemover backgroundRemover = new BackgroundRemover(ImageInserter.this, (bitmap,color)  -> {
                    ImageInserter.this.previewBitmap = bitmap;
                    previewCroppedBitmapCache=null;
                    redraw();
                }, () -> {
                    buttonRemoveBackground.highlighted = false;
                    sliderBackgroundDetectionStrength.visible = false;
                    Logger.show(getString(R.string.errorWhileInserting));
                    redraw();
                }, previewBitmapWithNoModifications);
                backgroundRemover.setBackgroundDetectionStrength(backgroundDetectionStrength);
                backgroundRemover.setCropCoefficient(previewCropCoefficient);
                backgroundRemover.startOnFrame();
            } else {
                //RESTORE
                previewBitmap = previewBitmapWithNoModifications;
                previewCroppedBitmapCache = null;
                redraw();
            }
        }
        catch (Throwable e){
            Logger.show("onRemoveBackgroundClick Error: " + e.getMessage());
        }
    }
    void onBinarizeImageClick(){
        try {
            if(buttonBinarizeImage == null)
                return;
            if(buttonRemoveBackground == null)
                return;
            if(previewBitmapWithNoModifications == null)
                return;
            if(sliderBackgroundDetectionStrength == null)
                return;

            if(binarizeMode == BitmapBinarizer.MODE_NONE) binarizeMode = BitmapBinarizer.MODE_BW;
            else if(binarizeMode == BitmapBinarizer.MODE_BW) binarizeMode = BitmapBinarizer.MODE_BT;
            else if(binarizeMode == BitmapBinarizer.MODE_BT) binarizeMode = BitmapBinarizer.MODE_TW;
            else if(binarizeMode == BitmapBinarizer.MODE_TW) binarizeMode = BitmapBinarizer.MODE_NONE;
            buttonBinarizeImage.highlighted = binarizeMode != BitmapBinarizer.MODE_NONE;
            buttonRemoveBackground.visible = !buttonBinarizeImage.highlighted;
            sliderBinarizeCoefficient.visible = buttonBinarizeImage.highlighted;
            buttonRemoveBackground.highlighted = false;
            sliderBackgroundDetectionStrength.visible = false;

            if (buttonBinarizeImage.highlighted) {
                BitmapBinarizer bitmapBinarizer = new BitmapBinarizer(ImageInserter.this,
                        bitmap -> {
                    ImageInserter.this.previewBitmap = bitmap;
                    previewCroppedBitmapCache = null;
                    redraw();
                }, () -> {
                    buttonBinarizeImage.highlighted = false;
                    buttonRemoveBackground.highlighted = false;
                    buttonRemoveBackground.visible = true;
                    sliderBackgroundDetectionStrength.visible = false;
                    Logger.show(getString(R.string.errorWhileInserting));
                    redraw();
                }, previewBitmapWithNoModifications, binarizeCoefficient, binarizeMode);
                bitmapBinarizer.start();
            } else {
                //RESTORE
                previewBitmap = previewBitmapWithNoModifications;
                previewCroppedBitmapCache = null;
                redraw();
            }
        }
        catch (Throwable e){
            Logger.show("onBinarizeImageClick Error: " + e.getMessage());
        }
    }
    private void binarizeCoefficientChanged(){
        if(sliderBinarizeCoefficient != null)
            binarizeCoefficient = (int)sliderBinarizeCoefficient.getValue();
        BitmapBinarizer bitmapBinarizer = new BitmapBinarizer(ImageInserter.this,
                bitmap -> {
                    ImageInserter.this.previewBitmap = bitmap;
                    previewCroppedBitmapCache = null;
                    redraw();
                }, () -> {
                    buttonBinarizeImage.highlighted = false;
                    buttonRemoveBackground.highlighted = false;
                    buttonRemoveBackground.visible = true;
                    sliderBackgroundDetectionStrength.visible = false;
                    Logger.show(getString(R.string.errorWhileInserting));
                    redraw();
                },
                previewBitmapWithNoModifications, binarizeCoefficient, binarizeMode);
        bitmapBinarizer.start();
    }

    private float d(PointF point1, PointF point2){
        float dx = point1.x - point2.x;
        float dy = point1.y - point2.y;
        return (float)Math.sqrt( dx*dx+dy*dy );
    }
    private PointF subst(PointF point1, PointF point2){
        return new PointF(point1.x - point2.x, point1.y - point2.y);
    }
    private PointF sum(PointF point1, PointF point2){
        return new PointF(point1.x + point2.x, point1.y + point2.y);
    }
    private PointF mult(PointF point1, float number){
        return new PointF(point1.x * number, point1.y * number);
    }
    private PointF center(PointF point1, PointF point2){
        return new PointF((point1.x + point2.x)/2, (point1.y + point2.y)/2);
    }
    private float k(PointF point1, PointF point2){
        //float tgk = (point1.x - point2.x)/(point1.y - point2.y);
        return (float)Math.atan2((point1.y - point2.y), (point1.x - point2.x));
    }
    private PointF rotateVector(PointF vector, double radians){
        double x = vector.x * Math.cos(radians) - vector.y*Math.sin(radians);
        double y = vector.x * Math.sin(radians) + vector.y*Math.cos(radians);
        return new PointF((float)x, (float)y);
    }
    private double magnetRad(double input){
        double radians = input;
        while(radians > Math.PI*2d)
            radians -= Math.PI*2d;
        while(radians < 0)
            radians += Math.PI*2d;
        double treshold = 0.03f;

        //0 *
        if(radians < 0d + treshold || radians > Math.PI*2d - treshold)
            radians = 0d;

        //90 *
        if(radians < Math.PI*0.5d + treshold && radians > Math.PI*0.5d - treshold)
            radians = Math.PI*0.5d;

        //180 *
        if(radians < Math.PI + treshold && radians > Math.PI - treshold)
            radians = Math.PI;

        //270 *
        if(radians < Math.PI*1.5d + treshold && radians > Math.PI*1.5d - treshold)
            radians = Math.PI*1.5d;

        return radians;
    }


    class PreviewView extends View {
        Matrix imageMatrix = null;
        Paint paint;


        PreviewView(Context context) {
            super(context);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            //Определение размера целевого изображения...
            try{
                floatingMenu.setBounds(w,h);
                imageParameters = new BitmapFactory.Options();
                imageParameters.inJustDecodeBounds = true;
                {
                    InputStream ims = getContext().getContentResolver().openInputStream(fileUri);
                    BitmapFactory.decodeStream(ims, null, imageParameters);
                    ims.close();
                }
                //BitmapFactory.decodeFile(filename, options);
                //BitmapFactory.decodeFile(filename, options);
                // Calculate inSampleSize
                float fileWidth = imageParameters.outWidth;
                float fileHeight = imageParameters.outHeight;
                float maxWidth = getWidth() * 0.7f;
                float maxHeight = getHeight() * 0.7f;
                float coefWidth = maxWidth / fileWidth;
                float coefHeight = maxHeight / fileHeight;
                float coef = Math.min(coefHeight, coefWidth);
                float previewWidth = fileWidth * coef;
                float previewHeight = fileHeight * coef;
                previewSize.set(previewWidth, previewHeight);
                previewBitmapLoadingThread = new Thread(() -> {
                    try {
                        Bitmap tmpPreviewBitmap = Tools.decodeFile(getBaseContext(), fileUri, (int) previewWidth, (int) previewHeight);
                        if (Thread.currentThread() == previewBitmapLoadingThread) {
                            previewBitmap = tmpPreviewBitmap;
                            previewCroppedBitmapCache = null;
                            previewBitmapWithNoModifications = tmpPreviewBitmap;
                            if(preview != null) preview.postInvalidate();
                        }
                    }catch (Exception e){
                        Logger.show("Error reading image: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                previewBitmapLoadingThread.start();

                //previewBitmap = loadBitmap(filename, (int)previewWidth, (int)previewHeight);
            }
            catch (Throwable e){
                Logger.log("Где-то в ImageInserter.onSizeChanged произошла ошибка ",e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            }
            previewPosition.set(
                    (getWidth() - previewSize.x) / 2f,
                    (getHeight() - previewSize.y) / 2f);


        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            try{
                if(paint == null){
                    paint = new Paint();
                    paint.setAntiAlias(false);
                    //paint.setFilterBitmap(false);
                    paint.setAlpha(255);
                    imageMatrix = new Matrix();
                }
                //DRAW BACKGROUND
                if (canvasBitmap != null) {
                    //color
                    int color = (Integer)Data.get(Data.backgroundColorInt());
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(color);
                    canvas.drawRect(0,0, canvasBitmap.getWidth(), canvasBitmap.getHeight(), paint);

                    //draw grid
                    float gridSize = (Integer)Data.get(Data.gridSizeInt());
                    boolean gridVertical = (Boolean)Data.get(Data.gridVerticalBoolean());
                    int gridColor = Data.tools.getGridColor();
                    if(gridSize>1) {
                        paint.setColor(gridColor);
                        paint.setStrokeWidth(1);
                        for(float c=gridSize; c<canvasBitmap.getHeight(); c+=gridSize)
                            canvas.drawLine(0, c, canvasBitmap.getWidth(), c, paint);
                        if(gridVertical)
                            for(float c=gridSize; c<canvasBitmap.getWidth(); c+=gridSize)
                                canvas.drawLine(c, 0, c, canvasBitmap.getHeight(), paint);
                    }

                    //image
                    synchronized (DrawCore.drawSync) {
                            canvas.drawBitmap(canvasBitmap, 0, 0, paint);
                    }
                }

                //DRAW IMAGE, prepare rotation
                canvas.save();
                canvas.rotate((float)Math.toDegrees(magnetRad(previewRotationRad)), previewPosition.x, previewPosition.y);
                if(previewBitmap != null) {
                    //draw
                    paint.setFilterBitmap(previewSize.x / (float) previewBitmap.getWidth() < 25);
                    imageMatrix.reset();
                    imageMatrix.setScale(previewSize.x / (float) previewBitmap.getWidth(), previewSize.y / (float) previewBitmap.getHeight());
                    imageMatrix.postTranslate(previewPosition.x, previewPosition.y);
                    //draw original, not cropped bitmap
                    paint.setAlpha((int) (previewOpacity * 0.1f));
                    canvas.drawBitmap(previewBitmap, imageMatrix, paint);
                    //DRAW CROPPED BITMAP
                    paint.setAlpha(previewOpacity);
                    if(!previewCroppedBitmapCoefs.equals(previewCropCoefficient))
                        previewCroppedBitmapCache = null;
                    if(previewCroppedBitmapCache == null){
                        previewCroppedBitmapCache = Tools.cropBitmap(previewBitmap, previewCropCoefficient);
                        previewCroppedBitmapCoefs.set(previewCropCoefficient);
                    }
                    canvas.drawBitmap(previewCroppedBitmapCache, imageMatrix, paint);
                }
                //draw crop frame
                crop.drawCropFrame(canvas);
                //restore canvas
                canvas.restore();

                //draw black cover to hide image part which will not be inserter
                if(canvasBitmap != null && getHeight() > canvasBitmap.getHeight()){
                    paint.setColor(Color.BLACK);
                    canvas.drawRect(0, canvasBitmap.getHeight(), getWidth()-1, getHeight()-1, paint);
                }

                //DRAW POINTERS
                crop.drawControlDots(canvas);

                //draw menu
                Rect undoRect = rectOfUndo();
                floatingMenu.setTargetPosition(undoRect.centerX() - floatingMenu.width()/2f, undoRect.bottom + Data.store().DPI/10);
                floatingMenu.clearNoGoZones();
                if(crop.cropFrameRect != null) floatingMenu.addNoGoZone(crop.cropFrameRect);
                floatingMenu.draw(canvas);

                //debug and undoFrame
                if((Boolean) Data.get(Data.debugBoolean())){
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Tools.dp(1));
                    paint.setColor(Color.WHITE);
                    canvas.drawRect(rectOfUndo(), paint);
                    canvas.drawLine(0, previewPosition.y, getWidth()-1, previewPosition.y, paint);
                    canvas.drawLine( previewPosition.x, 0,  previewPosition.x, getHeight()-1, paint);
                }
            }catch (Exception e){
                Logger.log("Где-то в ImageInserter.redraw произошла ошибка ",e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            } catch (OutOfMemoryError e) {
                Logger.log("Где-то в ImageInserter.redraw Недостаточно памяти: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            try {
                int action = event.getAction() & MotionEvent.ACTION_MASK;

                if(Data.tools.isAllowedDeviceForUi(event) && floatingMenu.processTouch(event)){
                    return true;
                }

                if(crop.processEvent(event)){

                    return true;
                }

                if(event.getPointerCount() == 2 && action == MotionEvent.ACTION_MOVE){
                    if(pinch == null)
                        pinch = new Pinch(event);
                    else
                        pinch.move(event);
                }
                else
                    pinch = null;


                if(event.getPointerCount() == 1 && action == MotionEvent.ACTION_MOVE){
                    if(move == null)
                        move = new Move(event);
                    else
                        move.move(event);
                }
                else
                    move = null;

                redraw();
            }
            catch (Throwable e){
                Logger.log("Где-то в ImageInserter.dispatchTouchEvent произошла ошибка ",e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            }
            //return super.onTouchEvent(event);
            return true;
        }

    }

    class Move {
        private final PointF initialPreviewPosition = new PointF(0,0);
        private final PointF initialTouchPosition0 = new PointF(0,0);

        public Move(MotionEvent motionEvent) {
            this.initialTouchPosition0.set(motionEvent.getX(0), motionEvent.getY(0));
            initialPreviewPosition.set(previewPosition);
        }
        public void move(MotionEvent motionEvent){
            PointF currentTouchPosition0 = new PointF(motionEvent.getX(0), motionEvent.getY(0));

            //process movement
            //how to change initial POSITION
            PointF movement = subst(currentTouchPosition0, initialTouchPosition0);
            previewPosition = sum(initialPreviewPosition, movement);
        }
    }
    class Pinch {
        private final PointF initialPreviewSize = new PointF(0,0);
        private final PointF initialPreviewPosition = new PointF(0,0);
        private final float initialRotation;
        private final PointF initialTouchPosition0 = new PointF(0,0);
        private final PointF initialTouchPosition1 = new PointF(0,0);

        public Pinch(MotionEvent motionEvent) {
            this.initialTouchPosition0.set(motionEvent.getX(0), motionEvent.getY(0));
            this.initialTouchPosition1.set(motionEvent.getX(1), motionEvent.getY(1));
            initialPreviewSize.set(previewSize);
            initialPreviewPosition.set(previewPosition);
            initialRotation = previewRotationRad;
        }
        public void move(MotionEvent motionEvent){
            PointF currentTouchPosition0 = new PointF(motionEvent.getX(0), motionEvent.getY(0));
            PointF currentTouchPosition1 = new PointF(motionEvent.getX(1), motionEvent.getY(1));

            PointF initialCenter = center(initialTouchPosition0, initialTouchPosition1);
            PointF currentCenter = center(currentTouchPosition0, currentTouchPosition1);

            float initialDistance = d(initialTouchPosition0, initialTouchPosition1);
            float currentDistance = d(currentTouchPosition0, currentTouchPosition1);

            float initialAngle = k(initialTouchPosition0, initialTouchPosition1);
            float currentAngle = k(currentTouchPosition0, currentTouchPosition1);

            //process movement
            //how to change initial POTITION
            PointF movement = subst(currentCenter, initialCenter);


            //process zooming
            //change zoom
            float zoomCoefficient = currentDistance / initialDistance;
            previewSize = mult(initialPreviewSize, zoomCoefficient);
            //move image in way it zooms from center of pinch
            PointF initialPinchCenterToImageEdgeVector = subst(initialPreviewPosition, initialCenter);
            PointF currentPinchCenterToImageEdgeVector = mult(initialPinchCenterToImageEdgeVector, zoomCoefficient);
            PointF pinchCenterToImageEdgeVectorDifference = subst(currentPinchCenterToImageEdgeVector, initialPinchCenterToImageEdgeVector);

            //process rotating
            float angleDifference = currentAngle - initialAngle;
            previewRotationRad = initialRotation + angleDifference;
            //move image in way it rotates from center of pinch
            //нужно чтобы не дергалась картинка при примагничивании
            float magnetedDifference = angleDifference + ((float)magnetRad(previewRotationRad) - previewRotationRad);
            PointF rotatedPinchCenterToImageEdgeVector = rotateVector(currentPinchCenterToImageEdgeVector, magnetedDifference);
            PointF rotatedMovementDifference = subst(rotatedPinchCenterToImageEdgeVector, currentPinchCenterToImageEdgeVector);
            //apply changing position by several factors
            PointF positionChange = sum(movement, pinchCenterToImageEdgeVectorDifference);
            positionChange = sum(positionChange, rotatedMovementDifference);
            previewPosition = sum(initialPreviewPosition, positionChange);
        }
    }
    @SuppressWarnings("FieldCanBeLocal")
    class Crop{
        float cropControlPointTouchRadius = Tools.dp(20);
        //variables to optimize drawing of crop frame
        private Paint cropFramePaint = null;
        private RectF cropFrameRect = null;
        //variables to optimize drawing of control dots
        private final PointF cropDotsCenterOfRotation = new PointF();
        private final PointF cropDotsTopVector = new PointF();
        private final PointF cropDotsTopPoint = new PointF();
        private final PointF cropDotsBottomVector = new PointF();
        private final PointF cropDotsBottomPoint = new PointF();
        private final PointF cropDotsRightVector = new PointF();
        private final PointF cropDotsRightPoint = new PointF();
        private final PointF cropDotsLeftVector = new PointF();
        private final PointF cropDotsLeftPoint = new PointF();
        private final int STATE_FREE = 0;
        private final int STATE_LOCK_TOP = 1;
        private final int STATE_LOCK_BOTTOM = 2;
        private final int STATE_LOCK_RIGHT = 3;
        private final int STATE_LOCK_LEFT = 4;
        private int touchState = STATE_FREE;
        private final PointF touchDownLocation = new PointF();
        private final RectF touchDownCropCoefs = new RectF();


        public boolean processEvent(MotionEvent event){
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            PointF location = new PointF(event.getX(0), event.getY(0));
            PointF touchMovement = subst(location, touchDownLocation);
            PointF touchMovementRotated = rotateVector(touchMovement, -previewRotationRad);
            if(action == MotionEvent.ACTION_DOWN){
                touchDownLocation.set(location);
                touchDownCropCoefs.set(previewCropCoefficient);
                if(d(location, cropDotsTopPoint) < cropControlPointTouchRadius)
                    touchState = STATE_LOCK_TOP;
                else if(d(location, cropDotsBottomPoint) < cropControlPointTouchRadius)
                    touchState = STATE_LOCK_BOTTOM;
                else if(d(location, cropDotsLeftPoint) < cropControlPointTouchRadius)
                    touchState = STATE_LOCK_LEFT;
                else if(d(location, cropDotsRightPoint) < cropControlPointTouchRadius)
                    touchState = STATE_LOCK_RIGHT;
            }
            if(action == MotionEvent.ACTION_MOVE){
                if(touchState == STATE_LOCK_TOP) {
                    float height = previewSize.y;
                    float coefDifference = touchMovementRotated.y / height;
                    float newLeft = touchDownCropCoefs.left;
                    float newRight = touchDownCropCoefs.right;
                    float newTop = touchDownCropCoefs.top + coefDifference;
                    float newBottom = touchDownCropCoefs.bottom;
                    if(newTop < 0) newTop = 0;
                    if(newTop > 0.95f-newBottom) newTop = 0.95f-newBottom;
                    previewCropCoefficient.set(newLeft, newTop, newRight, newBottom);
                    redraw();
                    return true;
                }
                if(touchState == STATE_LOCK_BOTTOM) {
                    float height = previewSize.y;
                    float coefDifference = touchMovementRotated.y / height;
                    float newLeft = touchDownCropCoefs.left;
                    float newRight = touchDownCropCoefs.right;
                    float newTop = touchDownCropCoefs.top;
                    float newBottom = touchDownCropCoefs.bottom - coefDifference;
                    if(newBottom < 0) newBottom = 0;
                    if(newBottom > 0.95f-newTop) newBottom = 0.95f-newTop;
                    previewCropCoefficient.set(newLeft, newTop, newRight, newBottom);
                    redraw();
                    return true;
                }
                if(touchState == STATE_LOCK_RIGHT) {
                    float width = previewSize.x;
                    float coefDifference = touchMovementRotated.x / width;
                    float newLeft = touchDownCropCoefs.left;
                    float newRight = touchDownCropCoefs.right - coefDifference;
                    float newTop = touchDownCropCoefs.top;
                    float newBottom = touchDownCropCoefs.bottom;
                    if(newRight < 0) newRight = 0;
                    if(newRight > 0.95f-newLeft) newRight = 0.95f-newLeft;
                    previewCropCoefficient.set(newLeft, newTop, newRight, newBottom);
                    redraw();
                    return true;
                }
                if(touchState == STATE_LOCK_LEFT) {
                    float width = previewSize.x;
                    float coefDifference = touchMovementRotated.x / width;
                    float newLeft = touchDownCropCoefs.left + coefDifference;
                    float newRight = touchDownCropCoefs.right;
                    float newTop = touchDownCropCoefs.top;
                    float newBottom = touchDownCropCoefs.bottom;
                    if(newLeft < 0) newLeft = 0;
                    if(newLeft > 0.95f-newRight) newLeft = 0.95f-newRight;
                    previewCropCoefficient.set(newLeft, newTop, newRight, newBottom);
                    redraw();
                    return true;
                }
            }
            if(action == MotionEvent.ACTION_UP) {
                touchDownLocation.set(0, 0);
                touchDownCropCoefs.set(0, 0, 0, 0);
                touchState = STATE_FREE;
            }
            return false;
        }
        public void drawCropFrame(Canvas canvas){ //has to be called on already rotated canvas
            float top = previewPosition.y;
            float bottom = previewPosition.y + previewSize.y;
            float left = previewPosition.x;
            float right = previewPosition.x + previewSize.x;
            float width = right - left;
            float height = bottom - top;

            top += previewCropCoefficient.top * height;
            bottom -= previewCropCoefficient.bottom * height;
            left += previewCropCoefficient.left * width;
            right -= previewCropCoefficient.right * width;

            if(cropFrameRect == null)
                cropFrameRect = new RectF(left, top, right, bottom);
            else
                cropFrameRect.set(left, top, right, bottom);
            if(cropFramePaint == null) {
                cropFramePaint = new Paint();
                cropFramePaint.setStyle(Paint.Style.STROKE);
                cropFramePaint.setAntiAlias(true);
                cropFramePaint.setStrokeWidth(Tools.dp(3));
                cropFramePaint.setColor(Color.argb(200, 255, 255, 255));
            }
            canvas.drawRect(cropFrameRect, cropFramePaint);
        }
        private final Paint p = new Paint();
        public void drawControlDots(Canvas canvas){ //has to be called on not rotated canvas
            float top = previewPosition.y;
            float bottom = previewPosition.y + previewSize.y;
            float left = previewPosition.x;
            float right = previewPosition.x + previewSize.x;
            float width = right - left;
            float height = bottom - top;

            top += previewCropCoefficient.top * height; //поправка на кроп
            bottom -= previewCropCoefficient.bottom * height; //поправка на кроп
            left += previewCropCoefficient.left * width; //поправка на кроп
            right -= previewCropCoefficient.right * width; //поправка на кроп
            width = right - left; //обновить после поправки
            height = bottom - top; //обновить после поправки

            float cx = left + width / 2;
            float cy = top + height / 2;
            cropDotsCenterOfRotation.set(previewPosition.x, previewPosition.y); //center of rotation
            cropDotsTopVector.set(cx - cropDotsCenterOfRotation.x, top - cropDotsCenterOfRotation.y);
            cropDotsTopPoint.set(sum(cropDotsCenterOfRotation, rotateVector(cropDotsTopVector, magnetRad(previewRotationRad))));
            cropDotsBottomVector.set(cx - cropDotsCenterOfRotation.x, bottom - cropDotsCenterOfRotation.y);
            cropDotsBottomPoint.set(sum(cropDotsCenterOfRotation, rotateVector(cropDotsBottomVector, magnetRad(previewRotationRad))));
            cropDotsRightVector.set(right - cropDotsCenterOfRotation.x, cy - cropDotsCenterOfRotation.y);
            cropDotsRightPoint.set(sum(cropDotsCenterOfRotation, rotateVector(cropDotsRightVector, magnetRad(previewRotationRad))));
            cropDotsLeftVector.set(left - cropDotsCenterOfRotation.x, cy - cropDotsCenterOfRotation.y);
            cropDotsLeftPoint.set(sum(cropDotsCenterOfRotation, rotateVector(cropDotsLeftVector, magnetRad(previewRotationRad))));


            drawSquareDot(canvas, cropDotsTopPoint.x, cropDotsTopPoint.y, p);
            drawSquareDot(canvas, cropDotsBottomPoint.x, cropDotsBottomPoint.y, p);
            drawSquareDot(canvas, cropDotsRightPoint.x, cropDotsRightPoint.y, p);
            drawSquareDot(canvas, cropDotsLeftPoint.x, cropDotsLeftPoint.y, p);
        }
        void drawSquareDot(Canvas canvas, float x, float y, Paint paint){
            paint.setColor(Color.argb(100, 0,0,0)); //BLACK
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            float radius = Tools.dp(20) / 3f;
            canvas.drawCircle(x, y, radius + Tools.dp(1), paint);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y,radius, paint);
        }
    }

    static class BitmapBinarizer {


        public interface OnSuccessBinarized{
            void success(Bitmap bitmap);
        }
        static final int MODE_NONE = 0;
        static final int MODE_BW = 1;
        static final int MODE_BT = 2;
        static final int MODE_TW = 3;
        private final int coefficient;//0..255
        private final int mode;
        private final OnSuccessBinarized onSuccess;
        private final Runnable onError;
        private Bitmap bitmap;
        private final Context context;
        private final Handler handler;
        private DialogLoading dialogLoading = null;

        public BitmapBinarizer(Context context, OnSuccessBinarized onSuccess, Runnable onError, Bitmap bitmap, int coefficient, int mode) {
            this.context = context;
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.bitmap = bitmap;
            this.coefficient = coefficient;
            this.mode = mode;
            handler = new Handler();
        }


        public void start() {
            if (bitmap == null)
                return;
            dialogLoading = new DialogLoading(context, R.string.analyzingImage);
            dialogLoading.show();

            new Thread(() -> {
                try {
                    bitmap = doWorkAsync(bitmap, coefficient, mode);

                    handler.post(() -> {
                        dialogLoading.cancel();
                        if (onSuccess != null)
                            onSuccess.success(bitmap);
                    });
                } catch (Throwable e) {
                    e.printStackTrace();

                    handler.post(() -> {
                        Logger.show(context.getString(R.string.errorWhileInserting));
                        dialogLoading.cancel();
                        if (onError != null)
                            onError.run();
                    });
                }
            }).start();
        }

        public static Bitmap doWorkAsync(Bitmap bitmap, int coefficient, int mode) {
            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            for (int x=0; x<mutableBitmap.getWidth(); x++){
                for(int y=0; y<mutableBitmap.getHeight(); y++){
                    int color = mutableBitmap.getPixel(x,y);
                    int[] rgb = { Color.red(color), Color.green(color), Color.blue(color) };
                    int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1] * rgb[1] * .691 + rgb[2] * rgb[2] * .068);
                    if(coefficient >= 0) {
                        if (mode == MODE_BW)
                            mutableBitmap.setPixel(x, y, brightness < coefficient ? Color.BLACK : Color.WHITE);
                        if (mode == MODE_BT)
                            mutableBitmap.setPixel(x, y, brightness < coefficient ? Color.BLACK : Color.TRANSPARENT);
                        if (mode == MODE_TW)
                            mutableBitmap.setPixel(x, y, brightness < coefficient ? Color.TRANSPARENT : Color.WHITE);
                    }
                    if(coefficient < 0){
                        if (mode == MODE_BW)
                            mutableBitmap.setPixel(x, y, brightness > -coefficient ? Color.BLACK : Color.WHITE);
                        if (mode == MODE_BT)
                            mutableBitmap.setPixel(x, y, brightness > -coefficient ? Color.BLACK : Color.TRANSPARENT);
                        if (mode == MODE_TW)
                            mutableBitmap.setPixel(x, y, brightness > -coefficient ? Color.TRANSPARENT : Color.WHITE);
                    }
                }
            }
            return mutableBitmap;
        }
    }
}
