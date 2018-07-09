package com.oceanai.kafka;

import kafka.consumer.*;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by WangRupeng on 2017/10/7.
 */
public class KafkaConsumer implements Runnable {
    /**
     * Kafka数据消费对象
     */
    private ConsumerConnector consumer;

    /**
     * Kafka Topic名称
     */
    private String topic;

    /**
     * 线程数量，一般就是Topic的分区数量
     */
    private int numThreads;

    /**
     * 线程池
     */
    private ExecutorService executorPool;
    //private ExecutorService processorPool;

    private KafkaProcessorMultiThread processorMultiThread;
    private LinkedBlockingQueue<String> imageFeatures = new LinkedBlockingQueue<>();

    private ConsumerKafkaStreamProcesser consumerKafkaStreamProcesser;

    /**
     * 构造函数
     *
     * @param topic      Kafka消息Topic主题
     * @param numThreads 处理数据的线程数/可以理解为Topic的分区数
     * @param zookeeper  Kafka的Zookeeper连接字符串
     * @param groupId    该消费者所属group ID的值
     */
    public KafkaConsumer(String topic, int numThreads, String zookeeper, String groupId) {
        // 1. 创建Kafka连接器
        this.consumer = Consumer.createJavaConsumerConnector(createConsumerConfig(zookeeper, groupId));
        // 2. 数据赋值
        this.topic = topic;
        this.numThreads = numThreads;
    }

    @Override
    public void run() {
        // 1. 指定Topic
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(this.topic, this.numThreads);

        // 2. 指定数据的解码器
        StringDecoder keyDecoder = new StringDecoder(new VerifiableProperties());
        StringDecoder valueDecoder = new StringDecoder(new VerifiableProperties());

        // 3. 获取连接数据的迭代器对象集合
        /**
         * Key: Topic主题
         * Value: 对应Topic的数据流读取器，大小是topicCountMap中指定的topic大小
         */
        Map<String, List<KafkaStream<String, String>>> consumerMap = this.consumer.createMessageStreams(topicCountMap, keyDecoder, valueDecoder);

        // 4. 从返回结果中获取对应topic的数据流处理器
        List<KafkaStream<String, String>> streams = consumerMap.get(this.topic);

        // 5. 创建线程池
        this.executorPool = Executors.newFixedThreadPool(this.numThreads);
        //this.processorPool = Executors.newFixedThreadPool(1);
        //this.processorPool
        processorMultiThread = new KafkaProcessorMultiThread(imageFeatures);
        processorMultiThread.setRunning();
        new Thread(processorMultiThread).start();
        //this.processorPool.submit(this.processorMultiThread);
        //processorMultiThread.start();

        // 6. 构建数据输出对象
        int threadNumber = 0;
        for (final KafkaStream<String, String> stream : streams) {
            this.consumerKafkaStreamProcesser = new ConsumerKafkaStreamProcesser(stream, threadNumber);
            this.executorPool.submit(this.consumerKafkaStreamProcesser);
            threadNumber++;
        }
    }

    public void shutdown() {
        // 1. 关闭和Kafka的连接，这样会导致stream.hashNext返回false
        if (this.consumer != null) {
            this.consumer.shutdown();
        }

        // 2. 关闭线程池，会等待线程的执行完成
        if (this.executorPool != null) {
            // 2.1 关闭线程池
            this.executorPool.shutdown();

            // 2.2. 等待关闭完成, 等待五秒
            try {
                if (!this.executorPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.out.println("Timed out waiting for consumer threads to shut down, exiting uncleanly!!");
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted during shutdown, exiting uncleanly!!");
            }
        }

    }

    /**
     * 根据传入的zk的连接信息和groupID的值创建对应的ConsumerConfig对象
     *
     * @param zookeeper zk的连接信息，类似于：<br/>
     *                  hadoop-senior01.ibeifeng.com:2181,hadoop-senior02.ibeifeng.com:2181/kafka
     * @param groupId   该kafka consumer所属的group id的值， group id值一样的kafka consumer会进行负载均衡
     * @return Kafka连接信息
     */
    private ConsumerConfig createConsumerConfig(String zookeeper, String groupId) {
        // 1. 构建属性对象
        Properties prop = new Properties();
        // 2. 添加相关属性
        prop.put("group.id", groupId); // 指定分组id
        prop.put("zookeeper.connect", zookeeper); // 指定zk的连接url
        prop.put("zookeeper.session.timeout.ms", "10000"); //
        prop.put("zookeeper.sync.time.ms", "5000");
        prop.put("auto.commit.interval.ms", "5000");
        // 3. 构建ConsumerConfig对象
        return new ConsumerConfig(prop);
    }

    /**
     * Kafka消费者数据处理线程
     */
    public class ConsumerKafkaStreamProcesser implements Runnable {
        private String TAG = "ConsumerKafkaStreamProcesser";
        // Kafka数据流
        private KafkaStream<String, String> stream;
        // 线程ID编号
        private int threadNumber;

        /**
         * kafka 消息（ip*streamLocation）
         */
        private String kafkaMessage = "";

        private KafkaProcessor processor;

        public ConsumerKafkaStreamProcesser(KafkaStream<String, String> stream, int threadNumber) {
            this.stream = stream;
            this.threadNumber = threadNumber;

            //processor = new KafkaProcessor();
        }

        @Override
        public void run() {
            // 1. 获取数据迭代器
            ConsumerIterator<String, String> iter = this.stream.iterator();


            // 2. 迭代输出数据
            while (iter.hasNext()) {
                try {
                    // 2.1 获取数据值
                    MessageAndMetadata value = iter.next();

                    //multi thread
                    kafkaMessage = (String)value.message();
                    imageFeatures.put((String) value.message());

                    //single thread
                    //processor.process(kafkaMessage);
                    //System.out.println(kafkaMessage);

                } catch (Exception e) {
                    System.out.println("Receive kafka message error!");
                }
            }

            // 3. 表示当前线程执行完成
            System.out.println("Shutdown Thread:" + this.threadNumber);
            //mLogger.log(TAG+ "@" + this.threadNumber, "Shutdown Thread:" + this.threadNumber);
        }

    }
}
