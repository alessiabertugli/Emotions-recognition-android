package com.example.alessiabertugli.emotionapp;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.UiThread;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class CameraWindow {

    private static final String TAG = "CameraWindow";
    private Context mContext;
    private WindowManager.LayoutParams mWindowParam;
    private WindowManager mWindowManager;
    private FloatCamView mRootView;
    private Handler mUIHandler;

    private int mWindowWidth;
    private int mWindowHeight;

    private int mScreenMaxWidth;
    private int mScreenMaxHeight;

    private float mScaleWidthRatio = 1.0f;
    private float mScaleHeightRatio = 1.0f;


    //ProgressBars
    public ProgressBar mAngryEm;
    public ProgressBar mDisgustEm;
    public ProgressBar mHappyEm;
    public ProgressBar mFearEm;
    public ProgressBar mSurprisedEm;
    public ProgressBar mSadEm;
    public ProgressBar mNeutralEm;

    //Percentages
    public TextView mDisPerc;
    public TextView mAngryPerc;
    public TextView mFearPerc;
    public TextView mSurpPerc;
    public TextView mSadPerc;
    public TextView mHappyPerc;
    public TextView mNeutPerc;

    public CameraWindow(Context context) {
        mContext = context;
        mUIHandler = new Handler(Looper.getMainLooper());

        // Get screen max size
        Point size = new Point();
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            display.getSize(size);
            mScreenMaxWidth = size.x;
            mScreenMaxHeight = size.y;
        } else {
            mScreenMaxWidth = display.getWidth();
            mScreenMaxHeight = display.getHeight();
        }
        // Default window size
        mWindowWidth = mScreenMaxWidth;  // / 2;
        mWindowHeight = mScreenMaxHeight;  // / 2;

        mWindowWidth = mWindowWidth > 0 && mWindowWidth < mScreenMaxWidth ? mWindowWidth : mScreenMaxWidth;
        mWindowHeight = mWindowHeight > 0 && mWindowHeight < mScreenMaxHeight ? mWindowHeight : mScreenMaxHeight;
    }


    private void init() {
        mUIHandler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                if (mWindowManager == null || mRootView == null) {
                    mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                    mRootView = new FloatCamView(CameraWindow.this);
                    mWindowManager.addView(mRootView, initWindowParameter());
                }
            }
        });
    }

    public void release() {
        mUIHandler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                if (mWindowManager != null) {
                    mWindowManager.removeViewImmediate(mRootView);
                    mRootView = null;
                }
                mUIHandler.removeCallbacksAndMessages(null);
            }
        });
    }

    private WindowManager.LayoutParams initWindowParameter() {
        mWindowParam = new WindowManager.LayoutParams();

        mWindowParam.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mWindowParam.format = 1;
        mWindowParam.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowParam.flags = mWindowParam.flags | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        mWindowParam.flags = mWindowParam.flags | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        mWindowParam.alpha = 1.0f;

        mWindowParam.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        mWindowParam.x = 0;
        mWindowParam.y = 0;
        mWindowParam.width = mWindowWidth;
        mWindowParam.height = mWindowHeight;
        return mWindowParam;
    }

    public void setRGBBitmap(final Bitmap rgb) {
        checkInit();
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                mRootView.setRGBImageView(rgb);
            }
        });
    }

    private void checkInit() {
        if (mRootView == null) {
            init();
        }
    }

    public void setText(final TextView text,final String value){
        checkInit();
        mUIHandler.post( new Runnable() {
            @Override
            public void run() {
                text.setText(value);

            }
        });
    }

    public void setProgressBar(final ProgressBar progBar, final double num){
        checkInit();
        mUIHandler.post( new Runnable() {
            @Override
            public void run() {
                progBar.setMax(100);
                progBar.setProgress((int)(num));
            }
        });
    }

    @UiThread
    private final class FloatCamView extends FrameLayout {
        private WeakReference<CameraWindow> mWeakRef;
        private LayoutInflater mLayoutInflater;
        private ImageView mColorView;

        public FloatCamView(CameraWindow window) {
            super(window.mContext);
            mWeakRef = new WeakReference<CameraWindow>(window);
            mLayoutInflater = (LayoutInflater) window.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            FrameLayout body = this;
            View floatView = mLayoutInflater.inflate(R.layout.cam_window_view, body, true);
            mColorView = findViewById(R.id.imageView_c);

            int colorMaxWidth = (int) (mWindowWidth * window.mScaleWidthRatio);
            int colorMaxHeight = (int) (mWindowHeight * window.mScaleHeightRatio);

            mColorView.getLayoutParams().width = colorMaxWidth;
            mColorView.getLayoutParams().height = colorMaxHeight;

            mHappyEm = findViewById(R.id.progressBar5);
            mAngryEm = findViewById(R.id.progressBar6);
            mDisgustEm = findViewById(R.id.progressBar7);
            mSadEm = findViewById(R.id.progressBar8);
            mSurprisedEm = findViewById(R.id.progressBar9);
            mNeutralEm = findViewById(R.id.progressBar10);
            mFearEm = findViewById(R.id.progressBar11);

            mHappyPerc = findViewById(R.id.textView2);
            mAngryPerc = findViewById(R.id.textView3);
            mDisPerc = findViewById(R.id.textView4);
            mSadPerc = findViewById(R.id.textView5);
            mSurpPerc = findViewById(R.id.textView6);
            mNeutPerc = findViewById(R.id.textView7);
            mFearPerc = findViewById(R.id.textView8);

        }



        public void setRGBImageView(Bitmap rgb) {
            if (rgb != null && !rgb.isRecycled()) {
                mColorView.setImageBitmap(rgb);
            }
        }
    }

}
