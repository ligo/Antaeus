package com.oceanai.model;

public class Features {
    private String request_id;
    private int time_used;
    private String error_message;
    private double[][] feature;
    private int dimension;

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public int getTime_used() {
        return time_used;
    }

    public void setTime_used(int time_used) {
        this.time_used = time_used;
    }

    public String getError_message() {
        return error_message;
    }

    public void setError_message(String error_message) {
        this.error_message = error_message;
    }

    public double[][] getFeature() {
        return feature;
    }

    public void setFeature(double[][] feature) {
        this.feature = feature;
    }

    public int getDimention() {
        return dimension;
    }

    public void setDimention(int dimention) {
        this.dimension = dimention;
    }
}
