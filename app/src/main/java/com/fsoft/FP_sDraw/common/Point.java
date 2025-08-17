package com.fsoft.FP_sDraw.common;

import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

/**
 * Created with IntelliJ IDEA.
 * User: Dr. Failov
 * Date: 20.01.13
 * Time: 23:26
 */
public class Point {
    public float x=-1;
    public float y=-1;
    public int helper=0;

    public Point() {

    }
    public Point(float nx, float ny) {
        x=nx;
        y=ny;
    }
    public Point(MotionEvent motionEvent) {
        x=motionEvent.getX();
        y=motionEvent.getY();
    }
    public Point(int nx, int ny) {
        x=nx;
        y=ny;
    }
    public boolean isEmpty(){
        return x == -1 && y == -1;
    }
    public void empty(){
        x = -1;
        y = -1;
    }
    public void set(float nx, float ny) {
        x=nx;
        y=ny;
    }
    public Point(Point np) {
        x=np.x;
        y=np.y;
        helper=np.helper;
    }
    public Point(PointF np) {
        x=np.x;
        y=np.y;
    }
    public void set(Point np) {
        x=np.x;
        y=np.y;
        helper=np.helper;
    }
    public void set(PointF np) {
        x=np.x;
        y=np.y;
    }
    public float d(Point to){
        return distanceTo(to);
    }
    public float distanceTo(Point to){
        Point from = this;
        float dx = Math.abs(to.x - from.x);
        float dy = Math.abs(to.y - from.y);
        return (float)Math.sqrt(dx*dx+dy*dy);
    }

    public float degreeWith(Point point){
        //https://ru.onlinemschool.com/math/library/vector/angl/
        float v1x = x; //вектор 1
        float v1y = y;
        float v2x = point.x; //вектор 2
        float v2y = point.y;
        double sm = v1x * v2x + v1y * v2y; //скалярное произведение векторов
        double v1m = Math.sqrt(v1x * v1x + v1y * v1y); //модуль (длина) вектора 1
        double v2m = Math.sqrt(v2x * v2x + v2y * v2y); //модуль (длина) вектора 2
        double cos = sm / (v1m * v2m); //косинуус угла между векторами
        double rad = Math.acos(cos);
        double degree = Math.toDegrees(rad);
        return (float)degree;
    }
    public float k(Point point2){
        //output is radians
        //float tgk = (point1.x - point2.x)/(point1.y - point2.y);
        return (float)Math.atan2((y - point2.y), (x - point2.x));
    }
    public float distanceTo(MotionEvent motionEvent){
        Point from = this;
        float dx = Math.abs(motionEvent.getX() - from.x);
        float dy = Math.abs(motionEvent.getY() - from.y);
        return (float)Math.sqrt(dx*dx+dy*dy);
    }
    public Point plus(Point p){
        return new Point(x+p.x, y+p.y);
    }
    public Point plus(float px, float py){
        return new Point(x+px, y+py);
    }
    public Point multiply(float px){
        return new Point(x*px, y*px);
    }
    public Point rotate(float rad){
        double rx = x * Math.cos(rad) - y * Math.sin(rad);
        double ry = x * Math.sin(rad) + y * Math.cos(rad);
        return new Point((float)rx, (float)ry);
    }
    public Point minus(Point p){
        return new Point(x-p.x, y-p.y);
    }
    public Point minus(float px, float py){
        return new Point(x-px, y-py);
    }
    public Point centerWith(Point p){
        return new Point((x+p.x)/2, (y+p.y)/2);
    }
    public Point copy(){
        return new Point(this);
    }
    /*Set minimum values. If values too low, it will be set to minimum*/
    public void limitMin(float minX, float minY){
        if(x < minX)
            x = minX;
        if(y < minY)
            y = minY;
    }
    /*Set maximum values. If values too high, it will be set to maximum*/
    public void limitMax(float maxX, float maxY){
        if(x > maxX)
            x = maxX;
        if(y > maxY)
            y = maxY;
    }

    @NonNull
    public @Override String toString() {
        String result="(";
        result+=String.valueOf(x);
        result+=", ";
        result+=String.valueOf(y);
        result+=")";
        return result;
    }
    public void set(MotionEvent motionEvent) {
        x=motionEvent.getX();
        y=motionEvent.getY();
    }
    public boolean equals(float x, float y){
        return x == this.x && y == this.y;
    }
}
