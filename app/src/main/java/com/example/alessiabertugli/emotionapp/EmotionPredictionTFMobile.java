package com.example.alessiabertugli.emotionapp;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.Arrays;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class EmotionPredictionTFMobile {

    private static final String LOG_TAG = EmotionPredictionTFMobile.class.getSimpleName();

    private static final int DIM_BATCH_SIZE = 1;
    public static final int DIM_IMG_SIZE_HEIGHT = 48;
    public static final int DIM_IMG_SIZE_WIDTH = 48;
    private static final int DIM_PIXEL_SIZE = 1;
    private static final int CATEGORY_COUNT = 7;

    private static final String INPUT_NAME = "input_1";
    private static final String OUTPUT_NAME = "output_node0";
    private static final String[] OUTPUT_NAMES = { OUTPUT_NAME };

    private final int[] mImagePixels = new int[DIM_IMG_SIZE_HEIGHT * DIM_IMG_SIZE_WIDTH];
    private final float[] mImageData = new float[DIM_IMG_SIZE_HEIGHT * DIM_IMG_SIZE_WIDTH];
    private final float[] mResult = new float[CATEGORY_COUNT];

    private TensorFlowInferenceInterface mInferenceInterface;

    public EmotionPredictionTFMobile(Context mContext, String model_path) {
        mInferenceInterface = new TensorFlowInferenceInterface(mContext.getAssets(), model_path);
    }


    public Results classify(Bitmap bitmap) {
        convertBitmap(bitmap);

        mInferenceInterface.feed(INPUT_NAME, mImageData, DIM_BATCH_SIZE, DIM_IMG_SIZE_HEIGHT,
                DIM_IMG_SIZE_WIDTH, DIM_PIXEL_SIZE);
        mInferenceInterface.run(OUTPUT_NAMES);
        mInferenceInterface.fetch(OUTPUT_NAME, mResult);


        Log.v(LOG_TAG, "classify(): result = " + Arrays.toString(mResult));
        return new Results(mResult);
    }

    public void close() {
        mInferenceInterface.close();
    }

    private void convertBitmap(Bitmap bitmap) {
        bitmap.getPixels(mImagePixels, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < DIM_IMG_SIZE_HEIGHT * DIM_IMG_SIZE_WIDTH; i++) {
            mImageData[i] = convertToGreyScale(mImagePixels[i]);
        }
    }

    private float convertToGreyScale(int color) {
        return (((color >> 16) & 0xFF) + ((color >> 8) & 0xFF) + (color & 0xFF)) / 3.0f / 255.0f;
    }
}
