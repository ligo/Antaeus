package com.oceanai.monitor;

import com.oceanai.feature.OceanFeature;
import com.oceanai.searcher.GenericOceanaiImageSearcher;
import com.oceanai.utils.DateUtil;
import net.semanticmetadata.lire.builders.DocumentBuilder;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class IndexMonitor implements Runnable {

    private int intervals = 3;
    private String fieldName = OceanFeature.FIELD_NAME_OCENAI_FEATURE;
    private Logger logger = LoggerFactory.getLogger(IndexMonitor.class);

    public IndexMonitor (int intervals) {
        this.intervals = intervals;
    }

    @Override
    public void run() {
        ArrayList<String> pastDays = DateUtil.getDates(intervals);
        if (pastDays != null) {
            for (String key : GenericOceanaiImageSearcher.features.keySet()) {
                if (!pastDays.contains(key)) {
                    GenericOceanaiImageSearcher.features.remove(key);
                    GenericOceanaiImageSearcher.todayFeatures.clear();
                }
            }
            for (String date : pastDays) {
                if (GenericOceanaiImageSearcher.features.containsKey(date)) {
                    continue;
                }
                String indexPath = "index" + File.separator + date;
                //System.out.println("Scanning " + indexPath);
                logger.info("Scanning " + indexPath);
                File indexFile = new File(indexPath);
                if (!indexFile.isDirectory()) {
                    //System.out.println(indexPath + " path is not exist!");
                    logger.warn(indexPath + " path is not exist!");
                } else if (indexFile.listFiles().length == 0) {
                    //判断文件夹是否为空
                    //System.out.println(indexPath + " path is auto generate by lucene!");
                    logger.warn(indexPath + " is empty!");
                } else {
                    try {
                        IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
                        cachFeature(ir, date);
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
            //System.out.println("Monitor thread cache finished!");
            logger.info("Monitor thread cache finished!");
        }
    }

    /**
     * 缓存当前日期的index
     * @param reader
     * @param date
     * @throws IOException
     */
    public void cachFeature(IndexReader reader, String date) throws IOException {
        if (reader != null) {
            Bits liveDocs = MultiFields.getLiveDocs(reader);
            int docs = reader.numDocs();
            LinkedHashMap<String, byte[]> featureCache = new LinkedHashMap<>(docs);
            Document document;
            for (int i = 0; i < docs; i++) {
                if (!(reader.hasDeletions() && !liveDocs.get(i))) {
                    document = reader.document(i);
                    if (document.getField(fieldName) != null) {
                        featureCache.put(document.getField(DocumentBuilder.FIELD_NAME_IDENTIFIER).stringValue(), document.getField(fieldName).binaryValue().bytes);
                    }
                }
            }
            GenericOceanaiImageSearcher.features.put(date, featureCache);
        }
    }
}
