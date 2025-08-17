package com.fsoft.FP_sDraw.common;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import java.util.ArrayList;

/**
 *
 * Created by Dr. Failov on 03.01.14.

 * класс призван выступить в роли адаптера для заполнения ListView.
 * Но не простого адапрета, а такого, что хранит View-шки, а не текст и ссылки на ресурсы.
 */
public class ViewAdapter implements ListAdapter {
    private int size ;
    private ArrayList<View> cache = null;
    Context context;

    public ViewAdapter(Context c){
        cache = new ArrayList<>();
        size = 0;
        context = c;
    }

    public void addView(View v){
        try{
            cache.add(v);
            size ++;
        }catch (Exception e){
            Logger.log("Где-то в ViewAdapter.addView произошла ошибка ", e +"\n", true);
        }catch (OutOfMemoryError e) {
            Logger.log("Где-то в ViewAdapter.addView Недостаточно памяти: ", e.toString(), true);
        }
    }

    private View getView(int pos){
            if(pos < size)
                return cache.get(pos);
            else if(context != null)
                return new View(context);
            else
                return null;
    }

    public void clear(){
        cache.clear();
        size = 0;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public int getCount() {
        return size;
    }

    @Override
    public Object getItem(int position) {
        return getView(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getView(position);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return size == 0 ? 1 : size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }
}