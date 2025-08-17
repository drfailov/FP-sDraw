package com.fsoft.FP_sDraw.common;

/**
 * Created with IntelliJ IDEA.
 * User: Dr. Failov
 * Date: 20.01.13
 * Time: 23:26
 */
public class IntPoint {
    public int x=-1;
    public int y=-1;
    public IntPoint(int nx, int ny) {
        x=nx;
        y=ny;
    }
    public void set(int nx, int ny) {
        x=nx;
        y=ny;
    }
    public @Override String toString() {
        return "(" + x + ", " + y + ")";
    }
}
