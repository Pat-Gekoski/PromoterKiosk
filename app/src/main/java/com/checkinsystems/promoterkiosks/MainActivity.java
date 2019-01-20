package com.checkinsystems.promoterkiosks;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import java.io.File;

public class MainActivity extends AppCompatActivity {

    ImageView mMainImage;
    Context mContext;
    Uri mDownloadedImage;
    String mPic;
    int currentApiVersion;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        mMainImage = findViewById(R.id.iv_main_screen);

        File file = new File("data/data/com.checkinsystems.promoterkiosks/shared_prefs/preferences.xml");
        if(!file.exists()){
            Intent intent = new Intent(this, AdminActivity.class);
            startActivity(intent);
            finish();
        } else {
            imageTouch(mMainImage);
        }

        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String[] listOfPictures = pictures.list();

        boolean picIsFound = false;

        if (listOfPictures != null) {
            for (String pic : listOfPictures) {
                if (pic.contains("PromoterKiosk Main Image")) {
                    picIsFound = true;
                    mPic = pic;
                    break;
                }
            }
        }
        if (picIsFound) {

            mDownloadedImage = Uri.parse("file://" + pictures.toString() + "/" + mPic);

            if (mDownloadedImage != null) {
                mMainImage.setImageURI(mDownloadedImage);
            }
            else {
                mMainImage.setImageResource(R.drawable.main);
            }
        } else {
            mMainImage.setImageResource(R.drawable.main);
        }

        // hide the navigatoin bar
        currentApiVersion = android.os.Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        // This work only for android 4.4+
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {

            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

                        @Override
                        public void onSystemUiVisibilityChange(int visibility) {
                            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                                decorView.setSystemUiVisibility(flags);
                            }
                        }
                    });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void imageTouch(ImageView view) {
        View.OnTouchListener listener = new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int result = event.getAction();

                switch (result) {
                    case MotionEvent.ACTION_DOWN:
                        Intent intent = new Intent(mContext, CustomerInfoActivity.class);
                        startActivity(intent);
                        finish();
                        return true;
                }
                return false;
            }
        };

        view.setOnTouchListener(listener);

    }

    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onBackPressed(){
        // do nothing
    }

    @Override
    public void onPause(){
        super.onPause();
        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager != null) {
            activityManager.moveTaskToFront(getTaskId(), 0);
        }
    }
}
