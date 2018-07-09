package com.oceanai.kafka;

import com.google.gson.Gson;
import com.oceanai.builders.OceanDocumentBuilder;
import com.oceanai.model.IndexMessage;
import net.semanticmetadata.lire.utils.LuceneUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class KafkaProcessor {

    private List<IndexMessage> indexMessageList = new ArrayList<>(500);
    private Gson gson;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
    private boolean create = false;
    //private GlobalDocumentBuilder globalDocumentBuilder;
    public KafkaProcessor() {
        gson = new Gson();
        //globalDocumentBuilder = new GlobalDocumentBuilder(true,GlobalDocumentBuilder.HashingMode.LSH, false);
        //globalDocumentBuilder.addExtractor(OceanFeature.class);
    }
    public void process(String message) {
        //System.out.println(message);
        if (message.startsWith("[{") && message.endsWith("}]")) {
            //List<IndexMessage> indexMessageListTemp = gson.fromJson(message, )
            IndexMessage[] indexMessages = gson.fromJson(message, IndexMessage[].class);
            for (IndexMessage indexMessage : indexMessages) {
                indexMessageList.add(indexMessage);
            }
        } else {
            IndexMessage indexMessage = gson.fromJson(message, IndexMessage.class);
            System.out.println(indexMessage.getId());
            indexMessageList.add(indexMessage);
        }
        if (indexMessageList.size() >= 50) {
            try {
                System.out.println("AntaeusServer indexing " + indexMessageList.size() + " of features.");
                // Creating an Lucene IndexWriter
                String nowDate = getDate();
                String indexPath = "index" + File.separator + nowDate;
                //System.out.println("Index path is " + indexPath);
                File file = new File(indexPath);
                if (file.exists()) {
                    create = false;
                } else {
                    create = true;
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
                indexMessageList.clear();
                System.out.println("Finish indexing " + 50 + " of features.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

        }
    }

    private String getDate() {
        return simpleDateFormat.format(new Date());
    }
}
