package com.oceanai.kafka;

import com.google.gson.Gson;
import com.oceanai.builders.OceanDocumentBuilder;
import com.oceanai.model.IndexMessage;
import net.semanticmetadata.lire.utils.LuceneUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class KafkaProcessorMultiThread implements Runnable {

    private List<IndexMessage> indexMessageList = new ArrayList<>();
    private List<IndexMessage> videoIndexMessageList = new ArrayList<>();
    private Map<String, List<IndexMessage>> videoIndexMessagemap = new ConcurrentHashMap<>();
    private Gson gson;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
    private boolean create = false;
    private LinkedBlockingQueue<String> imageFeatures;
    private boolean running = true;
    private int queueSize = 50;
    private int videoQueueSize = 10;
    private long timeDelay = 2000;
    private long start = 0;
    private long timeCount = 0;
    private long videoStart = 0;
    private long videoTimeCount = 0;
    private long mapClearTimer = 0;
    private Logger logger = LoggerFactory.getLogger(KafkaProcessorMultiThread.class);
    //private GlobalDocumentBuilder globalDocumentBuilder;
    public KafkaProcessorMultiThread(LinkedBlockingQueue<String> imageFeatures) {
        gson = new Gson();
        this.imageFeatures = imageFeatures;
    }

    /**
     * 处理kafka接收到的message
     * @param message
     */
    public void process(String message) {
        if (start == 0) {
            start = System.currentTimeMillis();
        }
        if (videoStart == 0) {
            videoStart = System.currentTimeMillis();
        }
        //logger.info("Receive one kafka message.");
        if (message.startsWith("[{") && message.endsWith("}]")) {
            IndexMessage[] indexMessages = gson.fromJson(message, IndexMessage[].class);
            for (IndexMessage indexMessage : indexMessages) {
                if (indexMessage.getVideo_name() == null) {
                    indexMessageList.add(indexMessage);
                } else {
                    String videoName = indexMessage.getVideo_name();
                    if (videoIndexMessagemap.containsKey(videoName)) {
                        videoIndexMessagemap.get(videoName).add(indexMessage);
                    } else {
                        List<IndexMessage> indexMessageList = new ArrayList<>();
                        indexMessageList.add(indexMessage);
                        videoIndexMessagemap.put(videoName, indexMessageList);
                    }
                }
            }
            //System.out.println("Receive array list size is " + indexMessages.length);
            logger.info("Receive array list size is " + indexMessages.length);
        } else {
            IndexMessage indexMessage = gson.fromJson(message, IndexMessage.class);
            if (indexMessage.getVideo_name() == null) {
                indexMessageList.add(indexMessage);
                logger.info("Receive one image");
            } else {
                String videoName = indexMessage.getVideo_name();
                if (videoIndexMessagemap.containsKey(videoName)) {
                    videoIndexMessagemap.get(videoName).add(indexMessage);
                } else {
                    List<IndexMessage> indexMessageList = new ArrayList<>();
                    indexMessageList.add(indexMessage);
                    videoIndexMessagemap.put(videoName, indexMessageList);
                }
                //videoIndexMessageList.add(indexMessage);
                logger.info("Receive one video frame");
            }
            //System.out.println(indexMessage.getId());

        }

        timeCount += (System.currentTimeMillis() - start);
        videoTimeCount += (System.currentTimeMillis() - videoStart);

        if (timeCount > timeDelay || indexMessageList.size() >= queueSize) {
            long time = System.currentTimeMillis();
            String indexPath = "index" + File.separator + getDate();
            processIndexMessageList(indexMessageList, indexPath);
            logger.info("Finish indexing " + indexMessageList.size() + " of features, remaining " + imageFeatures.size() + " to be processed, time used " + (System.currentTimeMillis() - time));
            //System.out.println("Finish indexing " + queueSize + " of features, remaining " + imageFeatures.size() + " to be processed, time used " + (System.currentTimeMillis() - time));

            indexMessageList.clear();
            mapClearTimer += timeCount;
            timeCount = 0;
            start = 0;
        }

        if (!videoIndexMessagemap.isEmpty()) {
            long time = System.currentTimeMillis();
            int count = 0;
            for (String key : videoIndexMessagemap.keySet()) {
                List<IndexMessage> indexMessageList = videoIndexMessagemap.get(key);
                if (videoTimeCount > timeDelay || indexMessageList.size() >= videoQueueSize) {
                String indexPath = "index" + File.separator + key;
                processIndexMessageList(indexMessageList, indexPath);
                count += indexMessageList.size();
                logger.info("Finish indexing " + indexMessageList.size() + " of features from video " + key);
                indexMessageList.clear();
                }
            }
            if (mapClearTimer > 3600000) {
                videoIndexMessagemap.clear();
            }
            if (videoTimeCount > timeDelay) {
                videoTimeCount = 0;
                videoStart = 0;
            }
            //logger.info("Finish indexing " + count + " features from " + videoIndexMessagemap.size() + " videos" + " time used " + (System.currentTimeMillis() - time));
        }
    }

    private void processIndexMessageList(List<IndexMessage> indexMessageList, String indexPath){
        try {
            File file = new File(indexPath);
            if (file.exists()) {
                create = false;
                logger.debug(indexPath + " exists, will add feature in the exist index");
            } else {
                create = true;
                logger.info(indexPath + " did not exist, will create");
            }
            IndexWriter iw = LuceneUtils.createIndexWriter(indexPath, create, LuceneUtils.AnalyzerType.WhitespaceAnalyzer);
            OceanDocumentBuilder builder = new OceanDocumentBuilder();
            for (int i = 0; i < indexMessageList.size(); i++) {
                Document document = builder.createDocument(indexMessageList.get(i).getFeature(), indexMessageList.get(i).getId());
                iw.addDocument(document);
            }
            LuceneUtils.commitWriter(iw);
            LuceneUtils.optimizeWriter(iw);
            LuceneUtils.closeWriter(iw);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动线程
     */
    public void setRunning() {
        running = true;
    }

    /**
     * 停止线程
     */
    public void setStop() {
        running = false;
    }

    @Override
    public void run() {
        //System.out.println("The kafka processor thread running is " + running);
        logger.info("The kafka processor thread running is " + running);
        while (running) {
            try {
                String message = imageFeatures.take();
                //System.out.println(message);
                process(message);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取今天的日期
     * @return 今天的日期
     */
    private String getDate() {
        return simpleDateFormat.format(new Date());
    }
}
