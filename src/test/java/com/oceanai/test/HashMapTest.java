package com.oceanai.test;

import java.util.concurrent.ConcurrentHashMap;

public class HashMapTest {
    public static void main(String[] args) {
        ConcurrentHashMap<String, Integer> concurrentHashMap = new ConcurrentHashMap<>();
        concurrentHashMap.put("1", 1);
        concurrentHashMap.put("2", 2);
        concurrentHashMap.put("3", 3);
        System.out.println(concurrentHashMap.size());
        concurrentHashMap.remove("1");
        System.out.println(concurrentHashMap.size());
    }
}
