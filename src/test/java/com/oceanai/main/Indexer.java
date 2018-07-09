package com.oceanai.main;

import com.oceanai.kafka.KafkaConsumer;

public class Indexer {
    public static void main(String[] args) {
        String zookeeper = "192.168.1.5:2181/kafka";
        String groupId = "group1";
        String topic = "test";
        int threadNum = 1;
        KafkaConsumer kafkaConsumer = new KafkaConsumer(topic, threadNum, zookeeper, groupId);
        new Thread(kafkaConsumer).start();
    }
}
