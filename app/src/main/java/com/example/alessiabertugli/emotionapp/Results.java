package com.example.alessiabertugli.emotionapp;

public class Results {

    private final int mNumber;
    private final float[] mProbability;

    public Results(float[] result) {
        mNumber = argmax(result);
        mProbability = result;
    }

    public int getNumber() {
        return mNumber;
    }

    public float[] getProbability() { return mProbability; }

    private static int argmax(float[] probs) {
        int maxIdx = -1;
        float maxProb = 0.0f;
        for (int i = 0; i < probs.length; i++) {
            if (probs[i] > maxProb) {
                maxProb = probs[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }
}
