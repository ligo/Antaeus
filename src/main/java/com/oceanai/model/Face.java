package com.oceanai.model;

public class Face {
    private String image_base64 = "";
    private LandMark landmark ;

    public Face(String base64, LandMark landMark) {
        this.image_base64 = base64;
        this.landmark = landMark;
    }

    public String getBase64() {
        return image_base64;
    }

    public void setBase64(String base64) {
        this.image_base64 = base64;
    }

    public LandMark getLandMark() {
        return landmark;
    }

    public void setLandMark(LandMark landMark) {
        this.landmark = landMark;
    }
}
