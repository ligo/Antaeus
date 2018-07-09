package com.oceanai.utils;

import com.google.gson.Gson;
import com.oceanai.model.*;

import java.util.ArrayList;
import java.util.List;

public  class FaceTool {
    private static FaceTool instance = null;
    private static float threshold = 0.75f;
    private Gson gson = new Gson();

    public static FaceTool getInstance(){
        if (instance == null)
            return new FaceTool();
        else
            return instance;
    }

    public List<SearchFeature> detectFace(String base64) {
        String raw = HttpClientUtil.detectFace(base64);
        System.out.println(raw);
        ExtractResult er = gson.fromJson(raw, ExtractResult.class);
        //System.out.println(er.getTime_used());
        List<SearchFeature> searchFeatures = new ArrayList<>();
        for (int i = 0;i<er.getFace_nums();i++) {
            FaceFeature faceFeature = er.getResult()[i];
            int x1 = faceFeature.getLeft();
            int y1 = faceFeature.getTop();
            int x2 = x1 + faceFeature.getWidth();
            int y2 = y1 + faceFeature.getHeight();
            SearchFeature searchFeature = new SearchFeature(x1, y1, x2, y2, 0, faceFeature.getBlur(), faceFeature.getLandmark());
            searchFeatures.add(searchFeature);
        }
        return searchFeatures;
    }

    public Features getFeatures(List<String> faces) {
        String raw = HttpClientUtil.getFeature(faces);
        Features features = gson.fromJson(raw, Features.class);

        return features;
    }

    public Features getNewFeatures(List<Face> faces) {
        String raw = HttpClientUtil.getNewFeature(faces);
        Features features = gson.fromJson(raw, Features.class);

        return features;
    }


    public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0d;
        double normA = 0.0d;
        double normB = 0.0d;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        double result = 0.0d;
        if (normB != 0 && normA != 0) {
            result = (0.5 + 0.5 * (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))));
        }
        return result;
    }

    public static boolean isTheSamePerson(double[] feature1, double[] feature2) {
        if (feature1 == null || feature2 == null
                || feature1.length == 0 || feature2.length == 0)
            return false;

        return cosineSimilarity(feature1, feature2) > threshold;
    }

    public static float getThreshold() {
        return threshold;
    }

    public static void setThreshold(float threshold) {
        FaceTool.threshold = threshold;
    }
}
