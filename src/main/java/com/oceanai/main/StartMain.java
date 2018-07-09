package com.oceanai.main;

import com.google.gson.Gson;
import com.oceanai.feature.OceanFeature;
import com.oceanai.kafka.KafkaConsumer;
import com.oceanai.model.IndexConfig;
import com.oceanai.monitor.IndexMonitor;
import com.oceanai.quartz.schedule.IndexSchedule;
import com.oceanai.searcher.GenericOceanaiImageSearcher;
import com.oceanai.servlet.SearchServletNew;
import com.oceanai.utils.DateUtil;
import net.semanticmetadata.lire.builders.DocumentBuilder;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;

public class StartMain {
    private Logger logger = LoggerFactory.getLogger(StartMain.class);
    private String zookeeper;
    private String kafka_topic;
    private int kafka_consumer_thread_number;
    private int monitor_schedule = 1;

    /**
     * 读取配置文件，初始化kafka地址，verifier 地址
     * @param args
     */
    public void init(String[] args) {
        boolean passed = false;
        IndexConfig config = null;
        if (args.length > 0) {
            //System.out.println("Indexer config file path is " + args[0]);
            String configJson = readString(args[0]);
            System.out.println(configJson);
            Gson gson = new Gson();
            config = gson.fromJson(configJson, IndexConfig.class);
            kafka_topic = config.kafka_topic;
            kafka_consumer_thread_number = config.kafka_consumer_thread_number;
            monitor_schedule = config.monitor_schedule;
            passed = true;
        }
        if (!passed) {
            //System.out.println("No directory given as first argument.");
            logger.info("No config file given as first argument.");
            System.exit(1);
        }

        zookeeper = System.getenv("ZOOKEEPER_CONNECT");
        if (zookeeper == null || zookeeper.equals("")) {
            zookeeper = config.zookeeper_address;
        }
    }

    /**
     * 读取文件中的文本信息
     * @param filePath 文件路径
     * @return
     */
    private String readString(String filePath) {
        String text = "";
        File file = new File(filePath);
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            char[] buffer = new char[512];
            int count;
            StringBuilder builder = new StringBuilder();
            while ((count = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, count);
            }
            text = builder.toString();
            reader.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            file = null;
        }
        return text;
    }

    /**
     * 开启kafka consumer线程
     */
    public void startKafka() {
        //System.out.println("Zookeeper address is " + zookeeper);
        logger.info("Zookeeper address is " + zookeeper);
        logger.info("Starting kafka......");
        KafkaConsumer kafkaConsumer = new KafkaConsumer(kafka_topic, kafka_consumer_thread_number, zookeeper, "group1");
        new Thread(kafkaConsumer).start();
        //System.out.println("Kafka started.");
        logger.info("Kafka started.");
    }

    /**
     * 缓存今天已经记录在磁盘的index，如果没有则跳过此步骤
     */
    public void initFeature() {
        logger.info("Start to init today's index........");
        if (readTodayFeature()) {
            logger.info("Finished init today's index. ");
        } else {
            logger.info("Init today's index failure!");
        }
    }

    /**
     * 读取今天的缓存
     * @return
     */
    private boolean readTodayFeature() {
        String today = DateUtil.getDate();
        String indexPath = "index" + File.separator + today;
        File file = new File(indexPath);
        try {
            if (!file.isDirectory() || file.listFiles().length == 0) {
                return false;
            }
            IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
            Document document;
            String fileName = DocumentBuilder.FIELD_NAME_IDENTIFIER;
            String identifier = OceanFeature.FIELD_NAME_OCENAI_FEATURE;
            for (int i = 0; i < ir.numDocs(); i++) {
                document = ir.document(i);
                String key = document.getField(fileName).stringValue();
                byte[] featureBytes = document.getField(identifier).binaryValue().bytes;
                GenericOceanaiImageSearcher.todayFeatures.put(key, featureBytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 缓存磁盘上今天之前intervals天数的index
     */
    public void startMonitorSchedule() {
        //监控线程
        //System.out.println("Starting monitor thread......");
        logger.info("Starting monitor thread......");
        IndexMonitor indexMonitor = new IndexMonitor(monitor_schedule);
        new Thread(indexMonitor).start();
        IndexSchedule indexSchedule = new IndexSchedule();
        indexSchedule.start(monitor_schedule);
    }

    /**
     * 开启API服务
     * @throws Exception
     */
    public void startServer() throws Exception{
        //System.out.println("Starting server......");
        logger.info("Starting server......");
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(28888);
        server.setConnectors(new Connector[]{connector});

        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");
        contextHandler.addServlet(SearchServletNew.class, "/search");

        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(new Handler[]{contextHandler, new DefaultHandler()});
        server.setHandler(handlerCollection);
        server.start();
        server.join();
        //System.out.println("Server api started.");
        logger.info("Server api started.");
    }

}
