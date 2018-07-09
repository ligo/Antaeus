package com.oceanai.main;

/**
 * 服务入口
 *
 * @author WangRupeng
 * @since 2018-05-02-10:24
 */
public class AntaeusServer {
    public static void main(String[] args) throws Exception {
        StartMain startMain = new StartMain();
        startMain.init(args);
        startMain.initFeature();
        startMain.startKafka();
        startMain.startMonitorSchedule();
        startMain.startServer();
    }
}
