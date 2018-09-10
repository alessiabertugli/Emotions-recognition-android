package com.example.alessiabertugli.emotionapp;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.Trace;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import weka.classifiers.Classifier;

public class OnGetImageListener implements OnImageAvailableListener{

    private static final int INPUT_SIZE = 976;
    private static final String TAG = "OnGetImageListener";
    private static final String TF_TAG = "TFTest";
    private int mScreenRotation = 90;
    private List<VisionDetRet> results;
    private int mPreviewWdith = 0;
    private int mPreviewHeight = 0;
    private byte[][] mYUVBytes;
    private int[] mRGBBytes = null;
    private Bitmap mRGBframeBitmap = null;
    private Bitmap mCroppedBitmap = null;
    private Bitmap mResizedBitmap = null;
    private Bitmap mInversedBipmap = null;
    private Bitmap mTest = null;
    private boolean mIsComputing = false;
    private Handler mInferenceHandler;
    private Context mContext;
    private FaceDet mFaceDet;
    private CameraWindow mWindow;
    private Paint mFaceLandmardkPaint;
    private AssetManager mAsset;
    private int mframeNum = 0;
    private String modelName;
    private EmotionPredictionTFMobile mClassifier;
    private EmotionsList cs = new EmotionsList();
    private static final String tfModel = "fer2013_mini_XCEPTION.102-0.66.pb";


    public void initialize(
            final Context context,
            final AssetManager assetManager,
            final Handler handler, String model) throws IOException {
        this.mContext = context;
        this.mInferenceHandler = handler;
        this.mAsset = assetManager;
        this.modelName = model;

        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        mWindow = new CameraWindow(mContext);

        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.parseColor("#30DAC5"));
        mFaceLandmardkPaint.setStrokeWidth(2);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);

        mAsset = context.getAssets();
    }

    // Load Weka models
    public Classifier loadModel(){

        Classifier cls = null;
        Log.d(TAG, "Debug message camera " + modelName);
             try {
                 cls = (Classifier) weka.core.SerializationHelper.read(mAsset.open(this.modelName));

             } catch (IOException e) {
                 e.printStackTrace();
             } catch (Exception e) {
                 e.printStackTrace();
             }

        return cls;
    }

    public void deInitialize() {
        synchronized (OnGetImageListener.this) {
            if (mFaceDet != null) {
                mFaceDet.release();
            }
            if (mWindow != null) {
                mWindow.release();
            }
        }
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {

        Display getOrient = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        Point point = new Point();
        getOrient.getSize(point);
        int screen_width = point.x;
        int screen_height = point.y;
        Log.d(TAG, String.format("screen size (%d,%d)", screen_width, screen_height));
        if (screen_width < screen_height) {
            orientation = Configuration.ORIENTATION_PORTRAIT;
            mScreenRotation = -90;
        } else {
            orientation = Configuration.ORIENTATION_LANDSCAPE;
            mScreenRotation = 0;
        }

        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);

        // Rotate around the center if necessary.
        if (mScreenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(mScreenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }

    public Bitmap imageSideInversion(Bitmap src){
        Matrix sideInversion = new Matrix();
        sideInversion.setScale(-1, 1);
        Bitmap inversedImage = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), sideInversion, false);
        return inversedImage;
    }

    private double ToDecimalFormat(double x){
        DecimalFormat df = new DecimalFormat("#.###");
        String dx=df.format(x);
        x=Double.valueOf(dx);
        return x;
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            // No mutex needed as this method is not reentrant.
            if (mIsComputing) {
                image.close();
                return;
            }
            mIsComputing = true;

            Trace.beginSection("imageAvailable");

            final Plane[] planes = image.getPlanes();

            // Initialize the storage bitmaps once when the resolution is known.
            if (mPreviewWdith != image.getWidth() || mPreviewHeight != image.getHeight()) {
                mPreviewWdith = image.getWidth();
                mPreviewHeight = image.getHeight();

                //Log.d(TAG, String.format("Initializing at size %dx%d", mPreviewWdith, mPreviewHeight));
                mRGBBytes = new int[mPreviewWdith * mPreviewHeight];
                mRGBframeBitmap = Bitmap.createBitmap(mPreviewWdith, mPreviewHeight, Config.ARGB_8888);
                mCroppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

                mYUVBytes = new byte[planes.length][];
                for (int i = 0; i < planes.length; ++i) {
                    mYUVBytes[i] = new byte[planes[i].getBuffer().capacity()];
                }
            }

            for (int i = 0; i < planes.length; ++i) {
                planes[i].getBuffer().get(mYUVBytes[i]);
            }

            final int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            ImageUtils.convertYUV420ToARGB8888(
                    mYUVBytes[0],
                    mYUVBytes[1],
                    mYUVBytes[2],
                    mPreviewWdith,
                    mPreviewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    mRGBBytes);

            image.close();
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            //Log.e(TAG, "Exception!", e);
            Trace.endSection();
            return;
        }

        mRGBframeBitmap.setPixels(mRGBBytes, 0, mPreviewWdith, 0, 0, mPreviewWdith, mPreviewHeight);
        drawResizedBitmap(mRGBframeBitmap, mCroppedBitmap);

        mInversedBipmap = imageSideInversion(mCroppedBitmap);
        mResizedBitmap = Bitmap.createScaledBitmap(mInversedBipmap, (int)(INPUT_SIZE/4.5), (int)(INPUT_SIZE/4.5), true);

        mInferenceHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        String class_predicted = new String();
                        double[] class_percentage = new double[7];
                        Canvas canvas = new Canvas(mInversedBipmap);

                        if (!new File(Constants.getFaceShapeModelPath()).exists()) {
                            FileUtils.copyFileFromRawToOthers(mContext, R.raw.shape_predictor_68_face_landmarks, Constants.getFaceShapeModelPath());
                        }

                        if (mframeNum % 3 == 0) {
                            synchronized (OnGetImageListener.this) {
                                results = mFaceDet.detect(mResizedBitmap);
                            }
                        }
                        if (results.size() != 0) {
                            if (!modelName.equals(tfModel)) {

                                // Draw on bitmap

                                for (final VisionDetRet ret : results) {
                                    float resizeRatio = 4.5f;

                                    // Draw landmark
                                    int cont = 0;
                                    ArrayList<Point> landmarks = ret.getFaceLandmarks();
                                    ArrayList<org.opencv.core.Point> points_coord = new ArrayList<>();

                                    for (Point point : landmarks) {
                                        cont++;
                                        int pointX = (int) (point.x * resizeRatio);
                                        int pointY = (int) (point.y * resizeRatio);
                                        double[] p = new double[2];
                                        p[0] = (int) (point.x / resizeRatio);
                                        p[1] = (int) (point.y / resizeRatio);

                                        org.opencv.core.Point po = new org.opencv.core.Point();
                                        po.set(p);
                                        points_coord.add(po);

                                        canvas.drawText(String.valueOf(cont), pointX, pointY, mFaceLandmardkPaint);

                                    }

                                    EmotionPredictionWeka emotAct = new EmotionPredictionWeka();

                                    double[] current_features = emotAct.extractFeatures(points_coord);
                                    class_percentage = emotAct.predictNewEmotion(current_features, loadModel());

                                    double max = 0.0;
                                    int index_class = 0;

                                    for (int i = 0; i < class_percentage.length; i++) {
                                        if (class_percentage[i] > max) {
                                            max = class_percentage[i];
                                            index_class = i;
                                        }
                                    }

                                    class_predicted = cs.GetClassLabel(index_class);
                                    DisplayPercentages(mWindow, class_percentage);
                                }

                            }
                            else if (modelName.equals(tfModel)) {

                                TFinit();
                                mTest = Bitmap.createScaledBitmap(mInversedBipmap, EmotionPredictionTFMobile.DIM_IMG_SIZE_WIDTH, EmotionPredictionTFMobile.DIM_IMG_SIZE_WIDTH, true);
                                Results result = mClassifier.classify(mTest);
                                class_predicted = cs.GetClassLabel(result.getNumber());

                                for (int i = 0; i < class_percentage.length; i++)
                                    class_percentage[i] = (double) (result.getProbability()[i]);

                                DisplayPercentages(mWindow, class_percentage);
                            }
                        }

                      canvas.drawText(class_predicted, 60, 60, mFaceLandmardkPaint);

                        mframeNum++;
                        mWindow.setRGBBitmap(mInversedBipmap);
                        mIsComputing = false;
                    }
                });
        Trace.endSection();
    }

    public void DisplayPercentages(CameraWindow cm, double[] class_percentage){

            cm.setText(cm.mAngryPerc, String.valueOf(ToDecimalFormat(class_percentage[0] * 100)));
            cm.setText(cm.mDisPerc, String.valueOf(ToDecimalFormat(class_percentage[1] * 100)));
            cm.setText(cm.mFearPerc, String.valueOf(ToDecimalFormat(class_percentage[2] * 100)));
            cm.setText(cm.mHappyPerc, String.valueOf(ToDecimalFormat(class_percentage[3] * 100)));
            cm.setText(cm.mSadPerc, String.valueOf(ToDecimalFormat(class_percentage[4] * 100)));
            cm.setText(cm.mSurpPerc, String.valueOf(ToDecimalFormat(class_percentage[5] * 100)));
            cm.setText(cm.mNeutPerc, String.valueOf(ToDecimalFormat(class_percentage[6] * 100)));

            //Set progress bars
            cm.setProgressBar(cm.mAngryEm, class_percentage[0] * 100);
            cm.setProgressBar(cm.mDisgustEm, class_percentage[1] * 100);
            cm.setProgressBar(cm.mFearEm, class_percentage[2] * 100);
            cm.setProgressBar(cm.mHappyEm, class_percentage[3] * 100);
            cm.setProgressBar(cm.mSadEm, class_percentage[4] * 100);
            cm.setProgressBar(cm.mSurprisedEm, class_percentage[5] * 100);
            cm.setProgressBar(cm.mNeutralEm, class_percentage[6] * 100);

    }


    private void TFinit() {
        try {
            mClassifier = new EmotionPredictionTFMobile(mContext, tfModel);
        } catch (RuntimeException e) {
            Log.e(TF_TAG, "Failed to create classifier.", e);
        }
    }

}
