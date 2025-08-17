package com.fsoft.FP_sDraw;

/*
 *
 * Фрагмент для отображения сетки из рисунков на экране открытия файла
 * Created by Dr. Failov on 03.07.2014. *
 */


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.DialogLoading;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * отображает изображения, содержащиеся в принятой в конструкторе папке в видеде прокручиваемой сетки
 * Created by Dr. Failov on 08.01.14.

 * upd (09.01.2016) С днём рождения!)
 */
public class ImageGridFragment extends Fragment {
    public interface OnSelect{
        void select(File path);
    }


    private File path = null;
    private File pathAux = null;
    private File[] files = null;
    private GridView gridView = null;
    private OnSelect onSelect = null;
    private FileGridAdapter adapter = null;
    private final Handler handler = new Handler();
    DialogLoading dialogLoading = null;
    //не выполняется ли сейчас какой-то длительной операции
    private boolean ready = false;

    public ImageGridFragment() {

    }

    //inPath - main folder
    //auxPath - alternative path, from here will be used only these files that is not exists in main path
    //_onS - what to do when file selected
    public ImageGridFragment(File in_path, File aux_path, OnSelect _onS){
        super();
        path = in_path;
        pathAux = aux_path;
        onSelect = _onS;
    }

    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        if(bundle != null)
            path = new File(bundle.getString("path"));
        refreshFiles();
    }
    private void refreshFiles() {
        ArrayList<File> filesList = new ArrayList<>();
        //scanFiles in main folder
        Logger.log("Loading files from folder " + path);
        if (path != null && path.isDirectory()) {
            File[] mainFiles = path.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.toLowerCase().endsWith(".png");
                }
            });
            if(mainFiles != null) {
                Logger.log("Loaded " + mainFiles.length + " files from folder " + path);
                filesList.addAll(Arrays.asList(mainFiles));
            }
        }
        //scanFiles in aux folder
        Logger.log("Loading files from folder " + pathAux);
        if (pathAux != null && pathAux.isDirectory()) {
            File[] auxFiles = pathAux.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.toLowerCase().endsWith(".png");
                }
            });
            if(auxFiles != null){
                Logger.log("Loaded " + auxFiles.length + " files from folder " + pathAux);
                int counter = 0;
                //add aux files to list IF it is not exist in main folder
                for(int i=0; i<auxFiles.length; i++){
                    boolean existsInMain = false;
                    String auxPart = auxFiles[i].getName().split("\\.")[0];
                    Logger.log("auxPart: " + auxPart);
                    for(int j=0; j<filesList.size(); j++) {
                        String mainName = filesList.get(j).getName();
                        if (mainName.contains(auxPart)){
                            existsInMain = true;
                            break;
                        }
                    }
                    if(!existsInMain){
                        counter ++;
                        filesList.add(auxFiles[i]);
                    }
                }
                Logger.log("Added " + counter + " files from folder " + pathAux);
            }
        }
        files = new File[filesList.size()];
        files = filesList.toArray(files);

        //SORT
        for (int i = 0; i < files.length; i++) {              //sort
            for (int j = 1; j < files.length; j++) {
                if (files[j].getName().compareToIgnoreCase(files[j - 1].getName()) > 0) {
                    File tmp = files[j - 1];
                    files[j - 1] = files[j];
                    files[j] = tmp;
                }
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle outState){
        outState.putString("path", path.getAbsolutePath());
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        try{
            Logger.log("ImageGridFragment", "Loading ImageGridFragment for " + path, false);
            if(path == null)
                return getBigMessage(getActivity().getString(R.string.openWindowErrorNoPath));
            if(files == null)
                return getBigMessage(getActivity().getString(R.string.openWindowErrorNoFolder));
            if(files.length == 0)
                return getBigMessage(getActivity().getString(R.string.openWindowErrorNoFiles));

            gridView = new GridView(getActivity());
            Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            double displayWidthPixels = display.getWidth();
            double dpi = Data.store().DPI;
            double displaySizeInches = displayWidthPixels / dpi;
            int numColumns = (int)(displaySizeInches / 0.7);

            int size = (int)(displayWidthPixels/ numColumns);
            Logger.log("ImageGridFragment", "Columns: " + numColumns, false);
            gridView.setNumColumns(numColumns);
            adapter = new FileGridAdapter(size);
            gridView.setAdapter(adapter);

            Logger.log("ImageGridFragment", "Adapter set.", false);

            ready = true;
            return gridView;
        }catch (Throwable e) {
            Logger.log("ImageGridFragment("+path+").onCreateView", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            return getBigMessage(e.getMessage());
        }
    }

    View getBigMessage(String in_text) {
        RelativeLayout relativeLayout = new RelativeLayout(getActivity());
        relativeLayout.setGravity(Gravity.CENTER);
        TextView textView = new TextView(getActivity());
        textView.setText(in_text);
        textView.setPadding(50, 50, 50, 50);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(25);
        relativeLayout.addView(textView);
        return relativeLayout;
    }

    View.OnClickListener getOnClickListener(final File path){
        return view -> select(path);
    }

    private void select(final File path){
        try{
            Logger.log("Select: " + path);
            if(ready){
                ready = false;
                //run
                dialogLoading = new DialogLoading(getActivity(), null, null, getString(R.string.opening));
                dialogLoading.show();
                new Thread(() -> {
                    try {
                        Tools.sleep(100);
                        onSelect.select(path);
                        Tools.sleep(500);
                        handler.post(() -> {
                            try {
                                if (dialogLoading != null)
                                    dialogLoading.cancel();
                                getActivity().finish();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        });
                    }
                    catch (Throwable e){
                        Logger.log("Error in select(...): " + Tools.getStackTrace(e));
                        e.printStackTrace();
                    }
                }).start();
            }
        }catch (Throwable e) {
            Logger.log("ImageGridFragment("+path+").OnClickListener", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }

    class FileGridAdapter extends BaseAdapter {
        private int size = Tools.dp(150);
        private Handler handler = null;

        public FileGridAdapter(int size) {
            super();
            this.size = size;
            handler = new Handler();
        }

        @Override
        public int getCount() {
            return files.length;
        }

        @Override
        public Object getItem(int position) {
            return files[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            try {
                View view = null;
                if (convertView != null && position != 0)
                    view = convertView;
                else {
                    view = LayoutInflater.from(getActivity()).inflate(R.layout.open_image_item_layout, null, false);
                    view.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, size));
                }
                //view.setEnabled(true);

                TextView textView = view.findViewById(R.id.open_image_item_textview);
                ImageView imageView = view.findViewById(R.id.open_image_item_imageview);

                //здесь определяется подпись плитки
                //"sDraw_2021-04-12_12-59-59.-56556.png"  ->  "sDraw_2021-04-12_12-59-59"  ->  "_2021-04-12_12-59-59"  ->  " 2021-04-12 12-59-59"  ->  "2021-04-12 12-59-59"
                textView.setText(files[position].getName().split("\\.")[0].replaceAll("[^-_0123456789]", "").replace("_", " ").trim());

                //здесь определяется картинка плитки
                applyImageDelayed(imageView, files[position].getPath());

                View.OnClickListener onClickListener = getOnClickListener(files[position]);
                view.setOnClickListener(onClickListener);
                textView.setOnClickListener(onClickListener);
                imageView.setOnClickListener(onClickListener);


                View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        new DeleteConfirmation(getActivity(), files[position].getPath(), v).show();
                        return true;
                    }
                };
                view.setOnLongClickListener(onLongClickListener);
                textView.setOnLongClickListener(onLongClickListener);
                imageView.setOnLongClickListener(onLongClickListener);

                return view;
            }
            catch (Throwable e){
                Logger.log(e);
                return new View(getActivity());
            }
        }


        //загрузка картинки начинается только через 300 мс чтобы при быстрой прокрутке не тормозило
        private void applyImageDelayed(final ImageView imageView, final String path){
            imageView.setImageBitmap(null);
            imageView.setTag(path);

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (imageView.getTag().equals(path)) {
                            applyImage(imageView, path);
                        }
                    }
                    catch (Throwable e){
                        e.printStackTrace();
                    }
                }
            }, 300);
        }
        private void applyImage(final ImageView imageView, final String path){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int background = Tools.extractBackgroundColorFromFileName(path);
                        Bitmap bitmap = Tools.decodeFile(path, size / 2, size / 2);
                        if(bitmap != null) {
                            bitmap = Tools.addBitmapBackground(bitmap, background);
                            if (imageView.getTag().equals(path)) {
                                applyImage(imageView, bitmap);
                            } else {
                                if (bitmap != null)
                                    bitmap.recycle();
                            }
                        }
                        else{
                            Logger.log("File " + path + " decoded as NULL!");
                        }
                    }
                    catch (Throwable e){
                        Logger.log(e);
                    }
                }
            }).start();
        }
        private void applyImage(final ImageView imageView, final Bitmap bitmap){
            handler.post(() -> {
                try {
                    imageView.setImageBitmap(bitmap);
                }
                catch (Throwable e){
                    e.printStackTrace();
                }
            });
        }

    }

    class DeleteConfirmation extends AlertDialog {
        String file;
        View view;
        DeleteConfirmation(Context context, String in_file, View in_view){
            super(context);
            file = in_file;
            view = in_view;
        }

        @Override
        public void show() {
            setButton(Data.tools.getResource(R.string.FileOpenDeleteconfirmationConfirm), getAccceptListener());
            setButton2(Data.tools.getResource(R.string.FileOpenDeleteconfirmationCancel), getNullListener());
            setTitle(Data.tools.getResource(R.string.FileOpenDeleteconfirmationHeader));
            setMessage(Data.tools.getResource(R.string.FileOpenDeleteconfirmationMessage) + " \n\"" + file + "\"?");
            super.show();
        }

        DialogInterface.OnClickListener getAccceptListener(){
            return new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try{
                        //TEST PATHS

                        String transparentFileName = new File(file).getName();
                        String transparentFilePath = new File(file).getParent();
                        String exportedFilePath = new File(transparentFilePath).getParent();
                        String transparentFileString = transparentFilePath + File.separator + transparentFileName;
                        String exportedFileString = exportedFilePath + File.separator + transparentFileName;
                        File transparentFile = new File(transparentFileString);
                        File exportedFile = new File(exportedFileString);

                        Logger.log("transparentFileName = " + transparentFileName);
                        Logger.log("transparentFilePath = " + transparentFilePath);
                        Logger.log("exportedFilePath = " + exportedFilePath);
                        Logger.log("transparentFile = " + transparentFile);
                        Logger.log("exportedFile = " + exportedFile);

                        //try to delete transparent file
                        if(transparentFile.exists()){
                            if(transparentFile.delete())
                                Logger.log("Success deleted: " + transparentFile);
                            else
                                Logger.log("Failed to delete: " + transparentFile);
                        }
                        else
                            Logger.log("File doesn't exist: " + transparentFile);

                        //try to delete exported file
                        if(exportedFile.exists()){
                            if(exportedFile.delete())
                                Logger.log("Success deleted: " + exportedFile);
                            else
                                Logger.log("Failed to delete: " + exportedFile);
                        }
                        else
                            Logger.log("File doesn't exist: " + exportedFile);


                        refreshFiles();
                        adapter.notifyDataSetInvalidated();

                        //unregister from gallery
                        callBroadCast();
                    }catch (Throwable e){
                        Logger.log("ImageGridFragment.DeleteConfirmation.DialogInterface.OnClickListener", "File: " + file + ",\n Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                    }
                }
            };
        }

        public void callBroadCast() {
            if (Build.VERSION.SDK_INT >= 14) {
                //Log.e("-->", " >= 14");
                MediaScannerConnection.scanFile(getContext(), new String[]{Environment.getExternalStorageDirectory().toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                    /*
                     *   (non-Javadoc)
                     * @see android.media.MediaScannerConnection.OnScanCompletedListener#onScanCompleted(java.lang.String, android.net.Uri)
                     */
                    public void onScanCompleted(String path, Uri uri) {
                        //Log.e("ExternalStorage", "Scanned " + path + ":");
                        //Log.e("ExternalStorage", "-> uri=" + uri);
                    }
                });
            } else {
                //Log.e("-->", " < 14");
                getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                        Uri.parse("file://" + Environment.getExternalStorageDirectory())));
            }
        }

        DialogInterface.OnClickListener getNullListener(){
            return (dialogInterface, i) -> {
            };
        }
    }
}
