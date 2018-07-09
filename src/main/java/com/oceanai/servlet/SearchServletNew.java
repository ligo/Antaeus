package com.oceanai.servlet;

import com.google.gson.Gson;
import com.oceanai.feature.OceanFeature;
import com.oceanai.model.SearchBean;
import com.oceanai.model.SearchResult;
import com.oceanai.searcher.GenericOceanaiImageSearcher;
import com.oceanai.utils.DateUtil;
import com.oceanai.utils.HttpTool;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchServletNew extends HttpServlet {
    private Logger logger = LoggerFactory.getLogger(SearchServletNew.class);
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        logger.info("Get one request");
        String raw = HttpTool.readRequestData(req);
        resp.setContentType("application/json;charset=UTF-8");
        String result = process(raw);
        resp.getWriter().write(result);
    }

    /**
     * 处理请求
     * @param raw 请求参数
     * @return 处理结果json
     * @throws IOException
     */
    private String process(String raw) throws IOException{
        long start = System.currentTimeMillis();
        Gson gson = new Gson();
        SearchBean searchBean = gson.fromJson(raw, SearchBean.class);
        List<SearchResult> candidates = new ArrayList<>();
        Map<String, Object> resultMap = new HashMap<>();
        /*BufferedImage image = HttpTool.base64ToBuff(searchBean.base64);
        if (image == null) {
            logger.error("Cannot decode base64, please check out base64 value or parameter.");
            long latency = System.currentTimeMillis() - start;
            resultMap.put("code", 903);
            resultMap.put("time_used", latency);
            return gson.toJson(resultMap);
        }
        OceanFeature oceanFeature = new OceanFeature();
        double[] feature = oceanFeature.ApplyNewFeature(ImageUtils.get8BitRGBImage(image));*/
        double[] feature = searchBean.feature;
        if (feature == null) {
            //System.out.println("Detect no face");
            logger.error("Request json doesn't contains feature, please checkout.");
            long latency = System.currentTimeMillis() - start;
            resultMap.put("code", 902);
            resultMap.put("time_used", latency);
            return gson.toJson(resultMap);
        }
        GenericOceanaiImageSearcher searcher = new GenericOceanaiImageSearcher(searchBean.top, searchBean.threshold, OceanFeature.class);
        if (searchBean.video_name != null) {
            for (String video_name : searchBean.video_name) {
                searchVideopath(video_name, searcher, feature);
            }
            for (SearchResult searchResult : searcher.getResults()) {
                candidates.add(searchResult);
            }
            long latency = System.currentTimeMillis() - start;
            //System.out.println("Time used : " + latency);
            logger.info("Time used : " + latency);
            resultMap.put("code", 101);
            resultMap.put("time_used", latency);
            resultMap.put("result", candidates);
            //resp.getWriter().write(gson.toJson(resultMap));

            for (SearchResult searchResult : candidates) {
                System.out.println(searchResult.score + "  " + searchResult.indexKey);
            }
        } else {
            List<String> indexList = HttpTool.getDays(searchBean.startTime, searchBean.endTime);
            //System.out.println("Get one request.");
            if (indexList == null) {
                // 日期填写错误
                logger.error("Wrong date.");
                long latency = System.currentTimeMillis() - start;
                resultMap.put("code", 901);
                resultMap.put("time_used", latency);
            } else {
                for (String date : indexList) {
                    searchPath(date, searcher, feature);
                }
                for (SearchResult searchResult : searcher.getResults()) {
                    candidates.add(searchResult);
                }
                long latency = System.currentTimeMillis() - start;
                //System.out.println("Time used : " + latency);
                logger.info("Time used : " + latency);
                resultMap.put("code", 101);
                resultMap.put("time_used", latency);
                resultMap.put("result", candidates);
                //resp.getWriter().write(gson.toJson(resultMap));

                for (SearchResult searchResult : candidates) {
                    System.out.println(searchResult.score + "  " + searchResult.indexKey);
                }
            }
        }
        return gson.toJson(resultMap);
    }

    /**
     * 查找指定目录的相似人脸
     * @param date 待检索日期
     * @param searcher 查找器
     * @param feature 待检索图片的特征数组
     * @throws IOException
     */
    private void searchPath(String date, GenericOceanaiImageSearcher searcher, double[] feature) throws IOException {
        String indexPath = "index" + File.separator + date;
        //System.out.println("Scanning " + indexPath);
        logger.info("Scanning " + indexPath);
        File indexFile = new File(indexPath);
        if (!indexFile.isDirectory()) {
            //System.out.println(indexPath + " path is not exist!");
            logger.warn(indexPath + " is not exist!");
        } else if (indexFile.listFiles().length == 0) {
            //判断文件夹是否为空
            //System.out.println(indexPath + " path is auto generate by lucene!");
            logger.warn(indexPath + " is empty!");
        } else if (GenericOceanaiImageSearcher.features.containsKey(date)){
            //read from cached index
            logger.info("The date of " + date + " faces number is " + GenericOceanaiImageSearcher.features.get(date).size());
            searcher.searchFeatureCache(feature, date);
        } else if (date.equals(DateUtil.getDate())){ //read from "today's" cached index
            //LinkedHashMap<String, byte[]> tmp = GenericOceanaiImageSearcher.todayFeatures;
            logger.info("Today face number is " + GenericOceanaiImageSearcher.todayFeatures.size());
            searcher.searchByDay(feature, GenericOceanaiImageSearcher.todayFeatures);
        } else {
            IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
            logger.info("The date of " + date + " faces number is " + ir.numDocs());
            searcher.searchLocalDocument(feature, ir);
        }
    }

    private void searchVideopath(String video_name, GenericOceanaiImageSearcher searcher, double[] feature) throws IOException {
        String indexPath = "index" + File.separator + video_name;
        //System.out.println("Scanning " + indexPath);
        logger.info("Scanning " + indexPath);
        File indexFile = new File(indexPath);
        if (!indexFile.isDirectory()) {
            //System.out.println(indexPath + " path is not exist!");
            logger.warn(indexPath + " is not exist!");
        } else if (indexFile.listFiles().length == 0) {
            //判断文件夹是否为空
            //System.out.println(indexPath + " path is auto generate by lucene!");
            logger.warn(indexPath + " is empty!");
        }else {
            IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
            logger.info("The video " + video_name + " faces number is " + ir.numDocs());
            searcher.searchLocalDocument(feature, ir);
        }
    }
}
