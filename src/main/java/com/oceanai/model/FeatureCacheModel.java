package com.oceanai.model;

public class FeatureCacheModel {
    private byte[] bytes;
    private String key;

    public FeatureCacheModel(String key, byte[] bytes) {
        this.key = key;
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
