package com.eastmoney.tesseract;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PICK_PHOTO = 1;
    private TessBaseAPI tessBaseAPI;
    private static final String lang = "eus";//识别库
    //private static final String lang = "chi_sim";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tesseract/";
    private static final String TESSDATA = "tessdata";
    String result = "empty";
    private TextView text;
    private ProgressBar progressBar;
    private ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        text = (TextView) findViewById(R.id.text);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        imageView = (ImageView) findViewById(R.id.imageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.dao_ru) {
            pickPhoto();
        }

        return super.onOptionsItemSelected(item);
    }

    private void pickPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_PHOTO);
    }

    //试试这，注意，我在toHeiBai黑白处理图片的方法截图了图片宽度的三分之一，所以使用下面的方法需修改获取原宽度
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode,resultCode,data);
//        if (requestCode == REQUEST_PICK_PHOTO && resultCode == RESULT_OK) {
//            Uri uri = data.getData();
//            if (uri != null) {
//                Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped"));
//                UCrop.of(uri, destination)
//                        .withAspectRatio(3, 8)
//                        .withMaxResultSize(200, 800)
//                        .start(MainActivity.this);
//            }
//        }else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP){
//            prepareTesseract();
//            final Uri imageuri = UCrop.getOutput(data);
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inSampleSize = 1;
//            Bitmap bitmap = BitmapFactory.decodeFile(getRealImageFilePath(this,imageuri));
//            new MyAsyckTask().execute( toHeibai( bitmap));
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == REQUEST_PICK_PHOTO && resultCode == RESULT_OK) {
            prepareTesseract();
            Uri uri = data.getData();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            Bitmap bitmap = BitmapFactory.decodeFile(getRealImageFilePath(this,uri));
            bitmap = toHeibai(bitmap);
            new MyAsyckTask().execute( bitmap);
        }
    }


    public static String getRealImageFilePath( Context context,Uri uri) {
        if( uri == null ) {
            return null;
        }
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, filePathColumn, null, null, null);
        if (cursor!=null){
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String yourRealPath = cursor.getString(columnIndex);

                return yourRealPath;
            }
        cursor.close();
        }
        return uri.getPath();
    }



    private void prepareDirectory(String path) {

        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "ERROR: Creation of directory " + path + " failed, check does Android Manifest have permission to write to external storage.");
            }
        } else {
            Log.i(TAG, "Created directory " + path);
        }
    }

    private void prepareTesseract() {
        try {
            prepareDirectory(DATA_PATH + TESSDATA);
        } catch (Exception e) {
            e.printStackTrace();
        }
        copyTessDataFiles(TESSDATA);
    }
    //拷贝识别库到手机
    private void copyTessDataFiles(String path) {
        try {
            String fileList[] = getAssets().list(path);

            for (String fileName : fileList) {

                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                String pathToDataFile = DATA_PATH + path + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {

                    InputStream in = getAssets().open(path + "/" + fileName);

                    OutputStream out = new FileOutputStream(pathToDataFile);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();

                    Log.d(TAG, "Copied " + fileName + "to tessdata");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy files to tessdata " + e.toString());
        }
    }

    //真正从图片提取内容的方法
    private String extractText(Bitmap bitmap) {
        try {
            tessBaseAPI = new TessBaseAPI();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            if (tessBaseAPI == null) {
                Log.e(TAG, "TessBaseAPI is null. TessFactory not returning tess object.");
            }
        }

        tessBaseAPI.init(DATA_PATH, lang);

//       //EXTRA SETTINGS 提取设置
//        //For example if we only want to detect numbers    白名单
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_");
        //tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
//
//        //blackList Example   黑名单
//        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-qwertyuiop[]}{POIU" +
//                "YTRWQasdASDfghFGHjklJKLl;L:'\"\\|~`xcvXCVbnmBNM,./<>?");

        Log.d(TAG, "Training file loaded");
        tessBaseAPI.setImage(bitmap);

        String extractedText = "empty result";
        try {
            extractedText = tessBaseAPI.getUTF8Text();
        } catch (Exception e) {
            Log.e(TAG, "Error in recognizing text.");
        }
        tessBaseAPI.end();
        return extractedText;
    }

    //提取图片内容采用异步执行
    private class MyAsyckTask extends AsyncTask<Bitmap,Void,String>{

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(final Bitmap... params) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageBitmap(params[0]);
                }
            });
            return extractText(params[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            progressBar.setVisibility(View.GONE);
//            String pattern = "\\d{5,6}\\b|\\b[A-Z_]+\\b";//正则表达式过滤
            String pattern = "\\d{5,6}\\b";//正则表达式过滤
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(s);
            StringBuilder formatStringBuilder = new StringBuilder();
            while (m.find()) {
                formatStringBuilder.append(m.group()).append("\n");
//                Log.i(TAG,"formatStringBuilder---------"+formatStringBuilder.toString());
            }

            text.setText(formatStringBuilder);
        }
    }

    //转换成黑白照片，更利于识别图片
    public static Bitmap toHeibai(Bitmap mBitmap) {
        int mBitmapWidth = 0;
        int mBitmapHeight = 0;
        //截取图片宽度的3分之一
        mBitmapWidth = mBitmap.getWidth() / 3;
        mBitmapHeight = mBitmap.getHeight();
        Bitmap bmpReturn = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight,
                Bitmap.Config.ARGB_8888);
        Bitmap resizeBmp;
        int iPixel = 0;
        int wTime = 0;//用于判断是白色背景的图片
        int bTime = 0;//用于判断是黑色背景的图片
        for (int i = 0; i < mBitmapWidth; i++) {
            for (int j = 0; j < mBitmapHeight; j++) {
                int curr_color = mBitmap.getPixel(i, j);
                int avg = (Color.red(curr_color) + Color.green(curr_color) + Color
                        .blue(curr_color)) / 3;
                if (avg >= 190)//修改这个值会影响字体颜色的深浅，这个项目的截图的股票代码字体比较暗，设置成190有利于识别，
                {
                    iPixel = 255;
                    wTime++;
                } else if (avg < 190 && avg > 100) {
                    if (wTime > bTime) {//当为白色的背景图片时
                        iPixel = 0;
                    } else {
                        iPixel = 255;
                    }
                } else {
                    iPixel = 0;
                    bTime++;
                }
                int modif_color = Color.argb(255, iPixel, iPixel, iPixel);

                bmpReturn.setPixel(i, j, modif_color);
            }
        }
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        resizeBmp = ThumbnailUtils.extractThumbnail(bmpReturn, mBitmapWidth, mBitmapHeight);
        return resizeBmp;
    }
}
