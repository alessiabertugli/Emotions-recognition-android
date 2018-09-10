package com.example.alessiabertugli.emotionapp;

import org.opencv.core.Point;
import java.util.ArrayList;
import java.util.List;
import hugo.weaving.DebugLog;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class EmotionPredictionWeka {

    /** This class make prediction with Weka Model
    */

    @DebugLog
    public double[] extractFeatures(ArrayList<Point> points_coord) {

        ArrayList<Double> features = new ArrayList<>();
        double[] array_features = new double[14];

        double left_eye_brow_angle = findAngle(points_coord.get(17), points_coord.get(21), points_coord.get(19));
        double right_eye_brow_angle = findAngle(points_coord.get(22), points_coord.get(26), points_coord.get(24));
        double left_eye_width = sqrt(pow(points_coord.get(36).x - points_coord.get(39).x, 2) + pow(points_coord.get(36).y - points_coord.get(39).y, 2));
        double right_eye_width = sqrt(pow(points_coord.get(42).x - points_coord.get(45).x, 2) + pow(points_coord.get(42).y - points_coord.get(45).y, 2));
        double left_eye_height = sqrt(pow(points_coord.get(37).x - points_coord.get(41).x, 2) + pow(points_coord.get(37).y - points_coord.get(41).y, 2));
        double right_eye_height = sqrt(pow(points_coord.get(44).x - points_coord.get(46).x, 2) + pow(points_coord.get(44).y - points_coord.get(46).y, 2));
        double left_eye_upper_angle = findAngle(points_coord.get(36), points_coord.get(39), points_coord.get(37));
        double left_eye_low_angle = findAngle(points_coord.get(36), points_coord.get(39), points_coord.get(41));
        double right_eye_upper_angle = findAngle(points_coord.get(42), points_coord.get(45), points_coord.get(44));
        double right_eye_low_angle = findAngle(points_coord.get(42), points_coord.get(45), points_coord.get(46));
        double mouth_upper_angle = findAngle(points_coord.get(48), points_coord.get(54), points_coord.get(51));
        double mouth_low_angle = findAngle(points_coord.get(48), points_coord.get(54), points_coord.get(57));
        double mouth_width = sqrt(pow(points_coord.get(48).x - points_coord.get(54).x, 2) + pow(points_coord.get(48).y - points_coord.get(54).y, 2));
        double mouth_height = sqrt(pow(points_coord.get(51).x - points_coord.get(57).x, 2) + pow(points_coord.get(51).y - points_coord.get(57).y, 2));

        features.add(left_eye_brow_angle);
        features.add(right_eye_brow_angle);
        features.add(left_eye_width);
        features.add(right_eye_width);
        features.add(left_eye_height);
        features.add(right_eye_height);
        features.add(left_eye_upper_angle);
        features.add(left_eye_low_angle);
        features.add(right_eye_upper_angle);
        features.add(right_eye_low_angle);
        features.add(mouth_upper_angle);
        features.add(mouth_low_angle);
        features.add(mouth_width);
        features.add(mouth_height);

        for (int i = 0; i < features.size(); i++)
            array_features[i] = features.get(i);

        return array_features;
    }

    private double findAngle(org.opencv.core.Point p0, org.opencv.core.Point p1, org.opencv.core.Point c) {
        double p0c = Math.sqrt(Math.pow(c.x - p0.x, 2) + Math.pow(c.y - p0.y, 2)); // p0->c (b)
        double p1c = Math.sqrt(Math.pow(c.x - p1.x, 2) + Math.pow(c.y - p1.y, 2)); // p1->c (a)
        double p0p1 = Math.sqrt(Math.pow(p1.x - p0.x, 2) + Math.pow(p1.y - p0.y, 2)); // p0->p1 (c)
        //Carnot's theorem
        double cos_angle = 0.0;

        if (p0c > 0 && p1c > 0) {
            cos_angle = (p1c * p1c + p0c * p0c - p0p1 * p0p1) / (2 * p1c * p0c);
        }
        double eps = 0.05;

        if (cos_angle > 1.0) {
            cos_angle = 1.0 - eps;
        }
        else if (cos_angle < -1.0){
            cos_angle = -1.0 + eps;
        }

        double angle = Math.acos(cos_angle);
        return angle;
    }


    public double[] predictNewEmotion(final double[] features, Classifier cls) {

        // we need those for creating new instances later
        // order of attributes/classes needs to be exactly equal to those used for training

        //Attributes names
        final Attribute left_eye_brow_angle = new Attribute("1");
        final Attribute right_eye_brow_angle = new Attribute("2");
        final Attribute left_eye_width = new Attribute("3");
        final Attribute right_eye_width = new Attribute("4");
        final Attribute left_eye_height = new Attribute("5");
        final Attribute right_eye_height = new Attribute("6");
        final Attribute left_eye_upper_angle = new Attribute("7");
        final Attribute left_eye_low_angle = new Attribute("8");
        final Attribute right_eye_upper_angle = new Attribute("9");
        final Attribute right_eye_low_angle = new Attribute("10");
        final Attribute mouth_upper_angle = new Attribute("11");
        final Attribute mouth_low_angle = new Attribute("12");
        final Attribute mouth_width = new Attribute("13");
        final Attribute mouth_height = new Attribute("14");

        final List<String> classes = new ArrayList<String>() {
            {
                add("0"); //angry
                add("1"); //disgust
                add("2"); //fear
                add("3"); //happy
                add("4"); //sad
                add("5"); //surprised
                add("6"); //neutral
            }
        };

        // Instances(...) requires ArrayList<> instead of List<>...
        ArrayList<Attribute> attributeList = new ArrayList<Attribute>(15) {
            {
                add(left_eye_brow_angle);
                add(right_eye_brow_angle);
                add(left_eye_width);
                add(right_eye_width);
                add(left_eye_height);
                add(right_eye_height);
                add(left_eye_upper_angle);
                add(left_eye_low_angle);
                add(right_eye_upper_angle);
                add(right_eye_low_angle);
                add(mouth_upper_angle);
                add(mouth_low_angle);
                add(mouth_width);
                add(mouth_height);
                Attribute attributeClass = new Attribute("class", classes);
                add(attributeClass);
            }
        };

        // unpredicted data sets (reference to sample structure for new instances)
        Instances dataUnpredicted = new Instances("TestInstances", attributeList, 1);
        // last feature is target variable
        dataUnpredicted.setClassIndex(dataUnpredicted.numAttributes() - 1);

        DenseInstance newInstance = new DenseInstance(dataUnpredicted.numAttributes()) {
            {
                setValue(left_eye_brow_angle, features[0]);
                setValue(right_eye_brow_angle, features[1]);
                setValue(left_eye_width, features[2]);
                setValue(right_eye_width, features[3]);
                setValue(left_eye_height, features[4]);
                setValue(right_eye_height, features[5]);
                setValue(left_eye_upper_angle, features[6]);
                setValue(left_eye_low_angle, features[7]);
                setValue(right_eye_upper_angle, features[8]);
                setValue(right_eye_low_angle, features[9]);
                setValue(mouth_upper_angle, features[10]);
                setValue(mouth_low_angle, features[11]);
                setValue(mouth_width, features[12]);
                setValue(mouth_height, features[13]);
            }
        };

        // Reference to dataset
        newInstance.setDataset(dataUnpredicted);

        double result;

        String c = null;
        double[] percentage = new double[7];
        // predict new sample
        try {
            result = cls.classifyInstance(newInstance);

            //get the prediction percentage or distribution
            percentage = cls.distributionForInstance(newInstance);

            c = classes.get(new Double(result).intValue());
            System.out.println("Index of predicted class label: " + result + ", which corresponds to class: " + classes.get(new Double(result).intValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return percentage;
    }



}