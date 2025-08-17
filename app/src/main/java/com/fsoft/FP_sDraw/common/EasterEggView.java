package com.fsoft.FP_sDraw.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import com.fsoft.FP_sDraw.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
/**
 *
 * Created by Dr. Failov on 03.11.2015.
 */


public class EasterEggView extends View {
    private Timer timer;
    private final Handler handler = new Handler();
    private final int resourceID;
    private final ArrayList<Flyer> flyers = new ArrayList<>();
    private boolean autonomy = false;
    private Thread autoThread = null;
    private long lastAutoSchedule = System.currentTimeMillis();
    private Thread autoScheduler = null;

    private final Clock clock = new Clock();
    private final sDrawText sDraw = new sDrawText();
    private final DebugCollisions debugCollisions = new DebugCollisions();
    private final TouchControl touch = new TouchControl();


    public EasterEggView(Context context, int _resourceID, boolean autonomy) {
        super(context);
        resourceID = _resourceID;
        this.autonomy = autonomy;
    }
    public void vibrate(){
        if(autonomy)
            return;
        Tools.vibrate(this);
    }
    private void scheduleAuto(){
        if(autonomy)
            return;
        lastAutoSchedule = System.currentTimeMillis();
        if(autoScheduler == null){
            autoScheduler = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(autoScheduler != null) {
                        if (System.currentTimeMillis() - lastAutoSchedule > 20000 && autoThread == null)
                            startAuto();
                        Tools.sleep(10000);
                    }
                }
            });
            autoScheduler.start();
        }
    }
    private void startAuto(){
        if(autoThread == null){
            autoThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while(autoThread != null && flyers != null && !flyers.isEmpty()){
                        while (!isStopped())
                            Tools.sleep(500);

                        Tools.sleep(2000);

                        for(int i=0; i<120; i++) {
                            Tools.sleep(1000);
                            if (autoThread == null || flyers == null || flyers.isEmpty())
                                break;
                            getRamdomSilentFlyer().dropRandom();
                        }

                        while (!isStopped())
                            Tools.sleep(500);

                        if (autoThread == null || flyers == null || flyers.isEmpty())
                            break;
                        reset();
                        Tools.sleep(2000);
                    }
                }
            });
            autoThread.start();
        }
    }
    private void stopAuto(){
        if(autoThread != null)
            autoThread = null;
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        flyers.clear();
        for(int i=0; i<9; i++){
            flyers.add(new Flyer());
        }
        reset();
        if(autonomy)
            startAuto();
        else
            scheduleAuto();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        clock.draw(canvas);
        sDraw.draw(canvas);
        touch.draw(canvas);
        for(int i=0; i<flyers.size(); i++)
            flyers.get(i).draw(canvas);

        //debugCollisions.draw(canvas);

    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        touch.touchEvent(event);
        return true;
    }

    private float d(PointF point1, PointF point2){
        float dx = point1.x - point2.x;
        float dy = point1.y - point2.y;
        return (float)Math.sqrt( dx*dx+dy*dy );
    }
    private PointF subst(PointF point1, PointF point2){
        PointF result = new PointF(point1.x - point2.x, point1.y - point2.y);
        return result;
    }
    private float len(PointF vector){
        float dx = vector.x;
        float dy = vector.y;
        return (float)Math.sqrt( dx*dx+dy*dy );
    }
    private PointF sum(PointF point1, PointF point2){
        PointF result = new PointF(point1.x + point2.x, point1.y + point2.y);
        return result;
    }
    private PointF mult(PointF point1, float number){
        PointF result = new PointF(point1.x * number, point1.y * number);
        return result;
    }
    private PointF center(PointF point1, PointF point2){
        PointF result = new PointF((point1.x + point2.x)/2, (point1.y + point2.y)/2);
        return result;
    }
    private PointF up(PointF point1, int count){
        PointF result = new PointF(point1.x, point1.y + count);
        return result;
    }
    private float k(PointF point1, PointF point2){
        //float tgk = (point1.x - point2.x)/(point1.y - point2.y);
        float k = (float)Math.atan2((point1.y - point2.y), (point1.x - point2.x));
        return k;
    }
    private PointF normalize(PointF vector, float length){
        float d = len(vector);
        float coef = length / d;
        PointF result = new PointF(vector.x * coef, vector.y * coef);
        return result;
    }
    private PointF X3(float k, PointF x2_Mine, float d_Energy){

        //расчитывает вектор скорости после отбивания если известны:
        // k - угол между точками Х1 и Х2
        // х2 точка центра щара который будет отбиваться
        // суммарная энергия друх шаров (сумма скоростей) деленная пополам
        // .

        float x3 = x2_Mine.x - (float) Math.cos(k) * d_Energy;
        float y3 = x2_Mine.y - (float) Math.sin(k) * d_Energy;
        PointF point = new PointF(x3, y3);
        return point;
    }
    private PointF getSpeedAfterCollision(Flyer thisFlyer, Flyer anotherFlyer){
        Flyer f1 = anotherFlyer;
        Flyer f2 = thisFlyer;
        float sumEnergy = len(f1.v) + len(f2.v);
        float d = d(f1.pos, f2.pos);
        //для 2 шара
        //расичтать угол
        PointF x1 = f1.pos;
        PointF x2 = f2.pos;
        PointF v1 = f1.v;
        PointF v2 = f2.v;
        float k = k(x1, x2);

        //расчитать траекторию воздействия энергии при разбрасывании
        PointF x3_abs = X3(k, x2, sumEnergy/2);
        PointF x3 = subst(x3_abs, x2);

        //Расчитать траекторию отброса с учётом скорости шара
        PointF dropV = sum(x3, v2);
        //нормализация вектора для сохранения єнергии
        dropV = normalize(dropV, sumEnergy/2);
        return dropV;
    }
    private PointF getPointAroundCenter(PointF center, float radius, double angle){
        //0...360
        float y = center.y + (float)Math.sin(Math.toRadians(angle)) * radius;
        float x = center.x + (float)Math.cos(Math.toRadians(angle)) * radius;
        return new PointF(x, y);
    }
    private boolean isStopped(){
        for(int i=0; i<flyers.size(); i++)
            if(!flyers.get(i).isStopped())
                return false;
        return true;
    }
    private void reset(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!autonomy)
                    Logger.log("Resetting");
                PointF pointBit = new PointF(getWidth()/2, getHeight() * 0.8f);
                flyers.get(0).magnet(pointBit);

                PointF pointCenter = new PointF(getWidth()/2, getHeight() * 0.3f);
                int radius = Math.min(getWidth(), getHeight()) / 3;
                int totalFlyers = flyers.size();
                int maxDegree = 360;
                int degreeStep = maxDegree / (totalFlyers-1);
                int degree = 0;
                for(int i=1; i<totalFlyers; i++){
                    PointF point = getPointAroundCenter(pointCenter, radius, degree);
                    flyers.get(i).magnet(point);
                    degree += degreeStep;
                }

                Tools.sleep(1000);
                while(!isStopped()){
                    Tools.sleep(100);
                }
                for(int i=0; i<totalFlyers; i++){
                    flyers.get(i).magnet(null);
                }
            }
        }).start();
    }
    private Flyer getByPoint(PointF point){
        for(int i=0; i<flyers.size(); i++)
            if(flyers.get(i).isHere(point))
                return flyers.get(i);
        return null;
    }
    private Flyer getRamdomSilentFlyer(){
        Flyer flyer = getRamdomFlyer();
        while(!flyer.isStopped()) {
            Tools.sleep(10);
            flyer = getRamdomFlyer();
        }
        return flyer;
    }
    private Flyer getRamdomFlyer(){
        Random random = new Random();
        return flyers.get(random.nextInt(flyers.size()));
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for(int i=0; i<flyers.size(); i++)
                    flyers.get(i).fly();
                postInvalidate();
            }
        }, 50, 20);
    }
    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(timer != null)
            timer.cancel();
        clock.stop();
        stopAuto();
    }


    class Flyer{
        public PointF pos;
        public PointF v;
        public float m;
        public float r;


        private final Paint paint;
        private final Bitmap bitmap;
        private final Random random;
        private PointF magnet = null;

        public Flyer(){
            paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Tools.dp(1));
            random = new Random();

            r = Math.min(getWidth(),getHeight())/20;
            pos = new PointF(random.nextInt(getWidth()-(int) r -2), random.nextInt(getHeight()-(int) r -2));
            int maxSpeed = (int) r / 8;
            v = new PointF((random.nextInt(maxSpeed))-maxSpeed/2, (random.nextInt(maxSpeed))-maxSpeed/2);

            //уменьшить размер картинки до требуемого
            Bitmap original = null;
            {
                Bitmap tmp = BitmapFactory.decodeResource(getContext().getResources(), resourceID);
                original = Bitmap.createScaledBitmap(tmp, (int)r*2, (int)r*2, false);

            }
            Bitmap mask = getMask((int)r);

            //применить маску круга
            {
                Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas tempCanvas = new Canvas(result);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                tempCanvas.drawBitmap(original, 0, 0, null);
                tempCanvas.drawBitmap(mask, 0, 0, paint);
                paint.setXfermode(null);
                //Draw result after performing masking
                bitmap = result;
            }

        }
        public void draw(Canvas canvas){
            canvas.drawBitmap(bitmap, pos.x - r, pos.y-r, paint);
            canvas.drawCircle(pos.x, pos.y, r - paint.getStrokeWidth()/2, paint);
            if(magnet != null)
                touch.area(canvas, magnet, Tools.dp(40));
        }
        public void fly(){
            pos.x += v.x;
            pos.y += v.y;
            //потеря энергии при перемещении (эффект воды)
            v.x *= 0.98;
            v.y *= 0.98;

            //если что-то магнитит
            reactMagnet();

            //притягивание к другим объектам
            //если использовать вместе с коллизиями, получается херня
            //reactMagnetic();

            //коллизии
            reactCollisions();

            //отражения от стен
            reactWalls();
        }
        public void magnet(PointF point){
            //NULL to disable
            magnet = point;
        }
        public void drop(PointF v){
            this.v = sum(v, this.v);
        }
        public void dropRandom(){
            Random random = new Random();
            float max = r*2;
            PointF point = new PointF(random.nextFloat() * max, random.nextFloat() * max);
            drop(point);
        }
        public boolean isStopped(){
            return v.x <= r/100 && v.y <= r/100;
        }
        public boolean isHere(PointF point){
            return d(point, pos) < r;
        }


        private void reactWalls(){
            if(pos.x < r){//LEFT
                pos.x = r;
                v.x = Math.abs(v.x);
                vibrate();
            }
            if(pos.x > getWidth() - r) {//RIGHT
                pos.x = getWidth() - r;
                v.x = -Math.abs(v.x);
                vibrate();
            }
            if(pos.y < r){//TOP
                pos.y = r;
                v.y = Math.abs(v.y);
                vibrate();
            }
            if(pos.y > getHeight() - r) {//BOTTOM
                pos.y = getHeight() - r;
                v.y = -Math.abs(v.y);
                vibrate();
            }
        }
        private void reactCollisions(){
            //реагировать между парой только один раз.
            //в данном случае цикл будет обрабатывать только те, что после него в массиве
            //все что до него будет пропускать
            boolean skip = true;
            for(int i=0; i<flyers.size(); i++) {
                Flyer f = flyers.get(i);
                if (!skip) {
                    float distance = d(f.pos, pos);
                    float sumRadius = r + f.r;
                    if(distance < sumRadius) {
                        //отбить тогда
                        //обрабатывать оба объекта здесь
                        PointF vThis = getSpeedAfterCollision(this, f);
                        PointF vAnot = getSpeedAfterCollision(f, this);
                        v = vThis;
                        f.v = vAnot;
                        vibrate();
                    }
                }
                if(f == this)
                    skip = false;
            }
        }
        private void reactMagnetic(){
            for(int i=0; i<flyers.size(); i++) {
                Flyer f = flyers.get(i);
                if (f != this) {
                    float dx = f.pos.x - this.pos.x;
                    float dy = f.pos.y - this.pos.y;
                    float distance = (float)Math.sqrt( dx*dx+dy*dy );
                    float sumRadius = r + f.r;
                    if(distance > sumRadius) {
                        float m1 = 1;
                        float m2 = 1;
                        float G = 0.01f;
                        v.x += dx * G * m1 * m2 / distance;
                        v.y += dy * G * m1 * m2 / distance;
                    }
                }
            }
        }
        private void reactMagnet(){
            if(magnet != null) {
                float dx = magnet.x - this.pos.x;
                float dy = magnet.y - this.pos.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float m1 = 1;
                float m2 = 1;
                float G = 0.01f;
                v.x += dx * G * m1 * m2 * Math.sqrt(distance);
                v.y += dy * G * m1 * m2 * Math.sqrt(distance);
                //защита от превышения
                if(v.x > r)
                    v.x = r;
                if(v.y > r)
                    v.y = r;
                if(v.x < -r)
                    v.x = -r;
                if(v.y < -r)
                    v.y = -r;
            }
        }



        private Bitmap getMask(int radius){
            Bitmap result = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setAntiAlias(true);
            canvas.drawCircle(radius, radius, radius, paint);
            return result;
        }
    }

    class TouchControl{
        private PointF touch = null;
        private Flyer holding = null;
        private final Paint paint = new Paint();
        private int nullTouches = 0;
        private final PointF lunkaCenter = null;
        private final long lastTimeCheckedStop = 0;
        private final boolean isStoppedLastTime = false;

        public TouchControl() {
            paint.setColor(Color.WHITE);
            paint.setAlpha(5);
        }

        void draw(Canvas canvas){
            if(touch != null && holding != null) {
                PointF dif = subst(touch, holding.pos);
                PointF lineTo = sum(mult(dif, -3f), holding.pos);
                line(canvas, holding.pos, lineTo);
            }
        }
        void line(Canvas canvas, PointF p1, PointF p2){
            int minStroke = 1;
            int maxStroke = Tools.dp(10);
            int strokeSteps = 10;
            int strokeStep = (maxStroke - minStroke)/(strokeSteps);
            paint.setStyle(Paint.Style.STROKE);
            int stroke = maxStroke;
            for(int i=0; i<strokeSteps; i++){
                paint.setStrokeWidth(stroke);
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint);
                stroke -= strokeStep;
            }
        }
        void circle(Canvas canvas, PointF center, int radius){
            int minStroke = Tools.dp(1);
            int maxStroke = Tools.dp(40);
            int strokeSteps = 10;
            int strokeStep = (maxStroke - minStroke)/(strokeSteps);
            paint.setStyle(Paint.Style.STROKE);
            int stroke = maxStroke;
            for(int i=0; i<strokeSteps; i++){
                paint.setStrokeWidth(stroke);
                canvas.drawCircle(center.x, center.y, radius, paint);
                stroke -= strokeStep;
            }
        }
        void area(Canvas canvas, PointF center, int radius){
            int minRadius = radius/5;
            int maxRaduis = radius;
            int steps = 10;
            int step = (maxRaduis - minRadius)/(steps);
            paint.setStyle(Paint.Style.FILL);
            int r = maxRaduis;
            for(int i=0; i<steps; i++){
                canvas.drawCircle(center.x, center.y, r, paint);
                r -= step;
            }
        }


        void touchEvent(MotionEvent motionEvent){
            if(autonomy)
                return;
            stopAuto();
            scheduleAuto();
//            if(!isStopped())
//                return;
            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                touch = new PointF(motionEvent.getX(), motionEvent.getY());
                Flyer flyer = getByPoint(touch);
                if(flyer == null) {
                    nullTouches++;
                    if(nullTouches >= 10){
                        reset();
                        nullTouches = 0;
                    }
                }
                else {
                    holding = flyer;
                    nullTouches = 0;
                }
            }
            else if(motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                touch = new PointF(motionEvent.getX(), motionEvent.getY());
            }
            else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                if(touch != null && holding != null) {
                    PointF dif = subst(touch, holding.pos);
                    PointF dropTo = mult(dif, -0.3f);
                    holding.drop(dropTo);
                    holding = null;
                    touch = null;
                }
            }
        }
    }

    class TouchControlOld{
        private PointF touch = null;
        private final ArrayList<PointF> magnets = new ArrayList<>();
        private final Paint paint = new Paint();

        public TouchControlOld() {
            paint.setColor(Color.WHITE);
            paint.setAlpha(5);
        }

        void draw(Canvas canvas){
            if(touch != null) {
                int radius = Tools.dp(70);
                circle(canvas, touch, radius);

                for (int i=0; i<magnets.size(); i++)
                    area(canvas, magnets.get(i), radius / 3);
            }
        }
        void circle(Canvas canvas, PointF center, int radius){
            int minStroke = Tools.dp(1);
            int maxStroke = Tools.dp(40);
            int strokeSteps = 10;
            int strokeStep = (maxStroke - minStroke)/(strokeSteps);
            paint.setStyle(Paint.Style.STROKE);
            int stroke = maxStroke;
            for(int i=0; i<strokeSteps; i++){
                paint.setStrokeWidth(stroke);
                canvas.drawCircle(center.x, center.y, radius, paint);
                stroke -= strokeStep;
            }
        }
        void area(Canvas canvas, PointF center, int radius){
            int minRadius = radius/5;
            int maxRaduis = radius;
            int steps = 10;
            int step = (maxRaduis - minRadius)/(steps);
            paint.setStyle(Paint.Style.FILL);
            int r = maxRaduis;
            for(int i=0; i<steps; i++){
                canvas.drawCircle(center.x, center.y, r, paint);
                r -= step;
            }
        }


        void touchEvent(MotionEvent motionEvent){
            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                touch = new PointF(motionEvent.getX(), motionEvent.getY());

                magnets.clear();
                int radius = Tools.dp(70);
                int totalFlyers = flyers.size();
                int maxDegree = 360;
                int degreeStep = maxDegree / (totalFlyers);
                int degree = 0;
                for(int i=0; i<totalFlyers; i++){
                    PointF point = getPointAroundCenter(touch, radius, degree);
                    magnets.add(point);
                    flyers.get(i).magnet(point);
                    degree += degreeStep;
                }
            }
            else if(motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                touch = new PointF(motionEvent.getX(), motionEvent.getY());

                magnets.clear();
                int radius = Tools.dp(70);
                int totalFlyers = flyers.size();
                int maxDegree = 360;
                int degreeStep = maxDegree / (totalFlyers);
                int degree = 0;
                for(int i=0; i<totalFlyers; i++){
                    PointF point = getPointAroundCenter(touch, radius, degree);
                    magnets.add(point);
                    flyers.get(i).magnet(point);
                    degree += degreeStep;
                }
            }
            else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                touch = null;
                magnets.clear();
                int totalFlyers = flyers.size();
                for(int i=0; i<totalFlyers; i++){
                    flyers.get(i).magnet(null);
                }
            }
        }
    }

    class DebugCollisions {
        Paint paint = new Paint();

        public DebugCollisions() {
        }

        public void draw(Canvas canvas){
            //debug
            //реагировать между парой только один раз.
            //в данном случае цикл будет обрабатывать только те, что после него в массиве
            //все что до него будет пропускать
            boolean skip = true;
            for(int i=0; i<flyers.size()-1; i++) {
                for (int j = i + 1; j < flyers.size(); j++) {
                    String text = "";
                    Flyer f1 = flyers.get(i);
                    Flyer f2 = flyers.get(j);
                    circle(canvas, f1.pos, f1.r);
                    circle(canvas, f2.pos, f2.r);
                    point(canvas, f1.pos);
                    point(canvas, f2.pos);
                    line(canvas, f1.pos, f2.pos);
                    line(canvas, f1.pos, sum(f1.pos, f1.v));
                    line(canvas, f2.pos, sum(f2.pos, f2.v));

                    float sumEnergy = len(f1.v) + len(f2.v);
                    text += "En=" + sumEnergy + "   ";

                    float d = d(f1.pos, f2.pos);
                    text += "d=" + d + "   ";


                    {
                        //для 2 шара
                        //расичтать угол
                        PointF x1 = f1.pos;
                        PointF x2 = f2.pos;
                        PointF v1 = f1.v;
                        PointF v2 = f2.v;
                        float k = k(x1, x2);
                        text += "k1=" + k + "   ";

                        //расчитать траекторию воздействия энергии при разбрасывании
                        PointF x3_abs = X3(k, x2, sumEnergy/2);
                        PointF x3 = subst(x3_abs, x2);
                        point(canvas, sum(x2, x3));

                        //Расчитать траекторию отброса с учётом скорости шара
                        PointF dropV = sum(x3, v2);
                        line(canvas, x2, sum(x2, dropV));
                    }
                    {
                        //для 1 шара
                        //расичтать угол
                        PointF x1 = f2.pos;
                        PointF x2 = f1.pos;
                        PointF v1 = f2.v;
                        PointF v2 = f1.v;
                        float k = k(x1, x2);
                        text += "k2=" + k + "   ";

                        //расчитать траекторию воздействия энергии при разбрасывании
                        PointF x3_abs = X3(k, x2, sumEnergy/2);
                        PointF x3 = subst(x3_abs, x2);
                        point(canvas, sum(x2, x3));

                        //Расчитать траекторию отброса с учётом скорости шара
                        PointF dropV = sum(x3, v2);
                        line(canvas, x2, sum(x2, dropV));
                    }

                    text(canvas, center(f1.pos, f2.pos), text);
                }
            }
        }

        private void line(Canvas canvas, PointF point1, PointF point2){
            paint.setStrokeWidth(1);
            paint.setColor(Color.WHITE);
            canvas.drawLine(point1.x, point1.y, point2.x, point2.y, paint);
        }
        private void point(Canvas canvas, PointF point){
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(point.x - 2, point.y - 2, 5, paint);
        }
        private void circle(Canvas canvas, PointF point, float radius){
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(point.x, point.y, radius, paint);
        }
        private void text(Canvas canvas, PointF point, String text){
            paint.setTextSize(20);
            paint.setColor(Color.WHITE);
            float w = paint.measureText(text);
            canvas.drawText(text, point.x - w/2, point.y - 50f, paint);
        }
    }

    class sDrawText{
        private Paint paint;
        private final String textToShow = "FP sDraw "+ getContext().getString(R.string.version);

        void draw(Canvas canvas){
            if(paint == null){
                paint = new Paint();
                paint.setTextSize(80);
                do{
                    paint.setTextSize(paint.getTextSize()-2);
                }while(paint.measureText(textToShow) > getWidth()/2);
                paint.setAntiAlias(true);
                paint.setColor(Color.rgb(40,40,40));
            }

            canvas.drawText(textToShow, getWidth()/2 - paint.measureText(textToShow)/2, getHeight()/2, paint);
        }
    }

    class Clock{
        private Paint clockPaint = null;
        private String clockString = "00:00:00";
        private Timer clockTimer = null;

        public Clock() {
            clockTimer = new Timer();
            clockTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Calendar calendar = Calendar.getInstance();
                    int hours = calendar.get(Calendar.HOUR_OF_DAY);
                    int minutes = calendar.get(Calendar.MINUTE);
                    int seconds = calendar.get(Calendar.SECOND);
                    clockString = (hours < 10 ? "0" + hours : hours) + ":" +
                            (minutes < 10 ? "0" + minutes : minutes) + ":" +
                            (seconds < 10 ? "0" + seconds : seconds);
                }
            }, 500, 500);
        }
        public void draw(Canvas canvas){
            if(clockPaint == null)
            {
                clockPaint = new Paint();
                clockPaint.setTextSize(getHeight()/3);
                clockPaint.setColor(Color.rgb(20,20,20));
                clockPaint.setAntiAlias(true);
                do{
                    clockPaint.setTextSize(clockPaint.getTextSize()-2);
                }while(clockPaint.measureText(clockString) > (float)getWidth()*0.9f);
            }
            canvas.drawText(clockString, getWidth()/2 - clockPaint.measureText(clockString)/2, getHeight()/4, clockPaint);
        }
        public void stop(){
            if(clockTimer != null)
                clockTimer.cancel();
        }
    }
}
