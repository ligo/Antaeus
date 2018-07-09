package com.oceanai.test;

import com.google.gson.Gson;
import com.oceanai.model.IndexMessage;

public class StringArrayTest {
    public static void main(String[] args) {
        IndexMessage indexMessage = new IndexMessage();
        indexMessage.setId("1");
        double[] test = new double[512];
        indexMessage.setFeature(test);
        Gson gson = new Gson();
        //System.out.println(gson.toJson(indexMessage));
        indexMessage = gson.fromJson(gson.toJson(indexMessage), IndexMessage.class);
        if (indexMessage.getVideo_name()  == null) {
            System.out.println(true);
        } else {
            System.out.println(false);
        }
    }
}
