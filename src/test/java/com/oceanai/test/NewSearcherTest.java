package com.oceanai.test;

import com.oceanai.feature.OceanFeature;
import com.oceanai.monitor.IndexMonitor;
import com.oceanai.model.SearchBean;
import com.oceanai.model.SearchResult;
import com.oceanai.searcher.GenericOceanaiImageSearcher;
import com.oceanai.utils.DateUtil;
import com.oceanai.utils.HttpTool;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class NewSearcherTest {
    public static void main(String[] args) {
        try {
            ArrayList<String> pastDays = DateUtil.getDates(3);
            IndexMonitor indexMonitor = new IndexMonitor(3);
            if (pastDays != null) {
                for (String date : pastDays) {
                    String indexPath = "index" + File.separator + date;
                    System.out.println("Scanning " + indexPath);
                    File indexFile = new File(indexPath);
                    if (!indexFile.isDirectory()) {
                        System.out.println(indexPath + " path is not exist!");
                    } else if (indexFile.listFiles().length == 0){
                        System.out.println(indexPath + " path is auto generate by lucene!");
                        //判断文件夹是否为空
                    } else {
                        try {
                            IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
                            indexMonitor.cachFeature(ir, date);
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
            System.out.println(GenericOceanaiImageSearcher.features.size());

            SearchBean searchBean = new SearchBean();
            BufferedImage image = ImageIO.read(new File("C:\\Users\\wangr\\Desktop\\test\\1.jpg"));
            OceanFeature oceanFeature = new OceanFeature();
            double[] feature = oceanFeature.ApplyNewFeature(net.semanticmetadata.lire.utils.ImageUtils.get8BitRGBImage(image));
            if (feature == null) {
                System.out.println("Detect no face");
                return;
            }
            List<String> indexList = HttpTool.getDays(searchBean.startTime, searchBean.endTime);
            GenericOceanaiImageSearcher searcher = new GenericOceanaiImageSearcher(searchBean.top, OceanFeature.class);
            if (indexList != null) {
                for (String index : indexList) {
                    String indexPath = "index" + File.separator + index;
                    System.out.println("Scanning " + indexPath);
                    File indexFile = new File(indexPath);
                    if (!indexFile.isDirectory()) {
                        System.out.println(indexPath + " path is not exist!");
                    } else if (indexFile.listFiles().length == 0) {
                        //判断文件夹是否为空
                        System.out.println(indexPath + " path is auto generate by lucene!");
                    } else if (GenericOceanaiImageSearcher.features.containsKey(index)) {
                        searcher.searchFeatureCache(feature, index);
                    } else {
                        IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
                        searcher.searchLocalDocument(feature, ir);
                    }
                }
                TreeSet<SearchResult> results = searcher.getResults();
                for (SearchResult searchResult: results) {
                    System.out.println(searchResult.score + "  " + searchResult.indexKey);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
