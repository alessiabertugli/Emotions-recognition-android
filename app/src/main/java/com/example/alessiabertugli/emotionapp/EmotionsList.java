package com.example.alessiabertugli.emotionapp;

public class EmotionsList {

    public String GetClassLabel(int index_class){

        String class_predicted = new String();

        if(index_class == 0){
            class_predicted = "ANGRY";
        }
        else if(index_class == 1){
            class_predicted = "DISGUST";
        }
        else if(index_class == 2){
            class_predicted = "FEAR";
        }
        else if(index_class == 3){
            class_predicted = "HAPPY";
        }
        else if(index_class == 4){
            class_predicted = "SAD";
        }
        else if(index_class == 5){
            class_predicted = "SURPRISED";
        }
        else if(index_class == 6){
            class_predicted = "NEUTRAL";
        }

        return class_predicted;
    }
}
