package com.oceanai.searcher;

import com.oceanai.feature.OceanFeature;
import com.oceanai.model.DocumentModel;
import com.oceanai.model.FeatureCacheModel;
import com.oceanai.model.SearchResult;
import com.oceanai.utils.FaceTool;
import net.semanticmetadata.lire.builders.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.indexers.parallel.ExtractorItem;
import net.semanticmetadata.lire.searchers.SimpleResult;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.Bits;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class GenericOceanaiImageSearcher{
    protected Logger logger = Logger.getLogger(getClass().getName());
    protected String fieldName;
    protected LireFeature cachedInstance = null;
    protected ExtractorItem extractorItem;
    public static ConcurrentHashMap<String, LinkedHashMap<String, byte[]>> features  = new ConcurrentHashMap<>();
    public static LinkedHashMap<String, byte[]> todayFeatures = new LinkedHashMap<>();
    protected IndexReader reader = null;

    protected int maxHits = 50;
    protected double threshold = 0.0;
    protected TreeSet<SearchResult> results = new TreeSet<>();
    protected volatile double maxDistance;
    private double minDistance;


    protected LinkedBlockingQueue<Map.Entry<Integer, byte[]>> queue = new LinkedBlockingQueue<Map.Entry<Integer, byte[]>>(100);
    protected int numThreads = DocumentBuilder.NUM_OF_THREADS;

    private OceanFeature oceanFeature = null;
    protected LinkedBlockingQueue<DocumentModel> linkedBlockingQueue = new LinkedBlockingQueue<DocumentModel>();
    protected LinkedBlockingQueue<FeatureCacheModel> featureCacheModels = new LinkedBlockingQueue<FeatureCacheModel>();
    protected ConcurrentSkipListSet<SimpleResult> concurrentSkipListSet = new ConcurrentSkipListSet<SimpleResult>();
    protected ConcurrentSkipListSet<SearchResult> searchResults = new ConcurrentSkipListSet<SearchResult>();
    protected int numThreadsQueue = DocumentBuilder.NUM_OF_THREADS;
    protected int numThreadProducer = 16;
    protected int producer_flag = 0;

    public GenericOceanaiImageSearcher(int maxHits, double threshold, Class<? extends GlobalFeature> globalFeature) {
        this(maxHits, globalFeature);
        this.threshold = threshold;
    }
    public GenericOceanaiImageSearcher(int maxHits, Class<? extends GlobalFeature> globalFeature) {
        this.maxHits = maxHits;
        this.extractorItem = new ExtractorItem(globalFeature);
        this.fieldName = extractorItem.getFieldName();
        try {
            this.cachedInstance = (GlobalFeature)extractorItem.getExtractorInstance().getClass().newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param reader
     * @param lireFeature
     * @return the maximum distance found for normalizing.
     * @throws IOException
     */
    protected void findSimilar(IndexReader reader, LireFeature lireFeature) throws IOException {

        // Needed for check whether the document is deleted.
        Bits liveDocs = MultiFields.getLiveDocs(reader);
        Document d;
        double tmpDistance;
        int docs = reader.numDocs();
        // we read each and every document from the index and then we compare it to the query.
        for (int i = 0; i < docs; i++) {
            if (reader.hasDeletions() && !liveDocs.get(i)) continue; // if it is deleted, just ignore it.
            d = reader.document(i);
            tmpDistance = getDistance(d, lireFeature);
            assert (tmpDistance >= 0);
            if (tmpDistance < this.threshold) {
                continue;
            }
            // if the array is not full yet:
            if (this.results.size() < maxHits) {
                this.results.add(new SearchResult(tmpDistance, d.getField(DocumentBuilder.FIELD_NAME_IDENTIFIER).stringValue()));
                //if (tmpDistance > maxDistance) maxDistance = tmpDistance;
            } else if (tmpDistance > results.last().score) {
                // if it is nearer to the sample than at least on of the current set:
                // remove the last one ...
                this.results.remove(this.results.last());
                // add the new one ...
                this.results.add(new SearchResult(tmpDistance, d.getField(DocumentBuilder.FIELD_NAME_IDENTIFIER).stringValue()));
                // and set our new distance border ...
            }
        }
        return;
    }

    protected double findMultiThreadSimilar(IndexReader reader, LireFeature lireFeature) throws IOException {
        maxDistance = -1d;
        // clear result set ...
        concurrentSkipListSet.clear();
        oceanFeature = (OceanFeature) lireFeature;
        // Needed for check whether the document is deleted.
        LinkedList<DocumentConsumer> tasks = new LinkedList<DocumentConsumer>();
        LinkedList<Thread> threads = new LinkedList<Thread>();
        DocumentConsumer consumer;
        int docs = reader.numDocs();
        Thread thread;

        int range = docs / numThreadProducer;
        for (int i = 0;i< numThreadProducer ;i++) {
            if (i == numThreadProducer - 1) {
                Thread p = new Thread(new DocumentProducer(i, reader, range * i, docs));
                p.start();
            } else {
                Thread p = new Thread(new DocumentProducer(i, reader, range * i, range * (i + 1)));
                p.start();
            }
        }

        System.out.println();
        /*Thread p1 = new Thread(new DocumentProducer(1, reader, 0, 25000));
        p1.start();
        Thread p2 = new Thread(new DocumentProducer(2, reader, 25001, 50000));
        p2.start();
        Thread p3 = new Thread(new DocumentProducer(3, reader, 80001, docs));
        p3.start();
        Thread p4 = new Thread(new DocumentProducer(4, reader, 50001, 80000));
        p4.start();*/
        /*try {
            p1.join();
            p2.join();
            p3.join();
            p4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        for (int i = 0; i < numThreadsQueue; i++) {
            consumer = new DocumentConsumer("consumer-" + i );
            thread = new Thread(consumer);
            thread.start();
            tasks.add(consumer);
            threads.add(thread);
        }
        for (Thread next : threads) {
            try {
                next.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return maxDistance;
    }

    class DocumentProducer implements Runnable{
        String threadName;
        IndexReader reader = null;
        int startIndex = 0;
        int endIndex = 0;
        private DocumentProducer(IndexReader reader) {
            this.reader = reader;
        }
        private DocumentProducer(int id, IndexReader reader, int startIndex, int endIndex) {
            this.threadName = "producer-" + id;
            this.reader = reader;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
        @Override
        public void run() {
            if (reader == null) {
                return;
            }
            //long start = System.currentTimeMillis();
            for (int i = startIndex;i < endIndex;i++) {
                try {
                    linkedBlockingQueue.add(new DocumentModel(i, reader.document(i)));
                    //System.out.println("Add one document, queue size is " + linkedBlockingQueue.size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /**/

            producer_flag++;
            if (producer_flag == numThreadProducer) {
                for (int i = 1;i< numThreadsQueue * 3;i++) {
                    linkedBlockingQueue.add(new DocumentModel(-i, new Document()));
                    //System.out.println("Add one empty document, queue size is " + linkedBlockingQueue.size());
                }
            }
            //System.out.println(threadName + " thead time used " + (System.currentTimeMillis() - start) + " flag is " + producer_flag);
        }
    }

    class DocumentConsumer implements Runnable {
        private boolean consumeFinish = false;
        private String threadName = "";
        public DocumentConsumer(String threadName) {
            this.threadName = threadName;
        }
        @Override
        public void run() {
            Document tmpDocument;
            int tmpId;
            DocumentModel tempDocumentModel;
            double tmpDistance;
            double tmpMaxDistance;
            while (!consumeFinish) {
                try {
                    tempDocumentModel = linkedBlockingQueue.take();
                    //System.out.println("Consume one document, queue size is " + linkedBlockingQueue.size());
                    tmpDocument = tempDocumentModel.getDocument();
                    tmpId = tempDocumentModel.getId();
                    if (tmpId >= 0) {
                        long start = System.currentTimeMillis();
                        tmpDistance = getDistance(tmpDocument, oceanFeature);
                        assert (tmpDistance >= 0d);
                        if (concurrentSkipListSet.size() < maxHits) {
                            concurrentSkipListSet.add(new SimpleResult(tmpDistance, tmpId));
                            if (tmpDistance > maxDistance) maxDistance = tmpDistance;
                        } else if (tmpDistance < concurrentSkipListSet.last().getDistance()) {
                            // if it is nearer to the sample than at least on of the current set:
                            // remove the last one ...
                            concurrentSkipListSet.remove(concurrentSkipListSet.last());
                            // add the new one ...
                            concurrentSkipListSet.add(new SimpleResult(tmpDistance, tmpId));
                            // and set our new distance border ...
                            //maxDistance = concurrentSkipListSet.last().getDistance();
                        }
                        //System.out.println(this.threadName + " add one face consume time is " + (System.currentTimeMillis() - start) + ", id is " + tmpId);
                    }else {
                        consumeFinish = true;
                    }

                } catch (InterruptedException e) {
                    e.getMessage();
                }
            }
        }
    }

    /**
     * 获取查找结果
     * @return
     */
    public TreeSet<SearchResult> getResults() {
        return this.results;
    }

    public ConcurrentSkipListSet<SearchResult> getMultiThreadResults() {
        return this.searchResults;
    }
    /**
     * Main similarity method called for each and every document in the index.
     *
     * @param document
     * @param lireFeature
     * @return the distance between the given feature and the feature stored in the document.
     */
    protected double getDistance(Document document, LireFeature lireFeature) {
        if (document.getField(fieldName).binaryValue() != null && document.getField(fieldName).binaryValue().length > 0) {
            //cachedInstance.setByteArrayRepresentation(document.getField(fieldName).binaryValue().bytes, document.getField(fieldName).binaryValue().offset, document.getField(fieldName).binaryValue().length);
            cachedInstance.setByteArrayRepresentation(document.getField(fieldName).binaryValue().bytes);
            return lireFeature.getDistance(cachedInstance);
        } else {
            logger.warning("No feature stored in this document! (" + extractorItem.getExtractorClass().getName() + ")");
        }
        return 0d;
    }

    /**
     * index中的二进制转化为double数组
     * @param bytes
     * @return
     */
    protected double[] bytes2doubleArray(byte[] bytes) {
        //bytes = unGZip(bytes);
        double[] histogram = new double[256];
        int times = Double.SIZE / Byte.SIZE;
        double[] doubles = new double[bytes.length / times];
        if (doubles.length != histogram.length) {
            return null;
        }
        for(int i=0;i<doubles.length;i++){
            doubles[i] = ByteBuffer.wrap(bytes, i*times, times).getDouble();
            histogram[i] = doubles[i];
        }
        return histogram;
    }

    /**
     * 计算两个特征数组的距离（相似度）
     * @param featureSearch 待检索图片的特征数组
     * @param featureIndex index中存储的特征数组
     * @return 相似度
     */
    protected double getDistance(double[] featureSearch, double[] featureIndex) {
        double result = FaceTool.cosineSimilarity(featureSearch, featureIndex);
        return result * 100;
    }

    /**
     * 从本地index文件中查找
     * @param feature
     * @param reader
     */
    public void searchLocalDocument(double[] feature, IndexReader reader) {
        try {
            OceanFeature oceanFeature = new OceanFeature();
            oceanFeature.setHistogram(feature);
            findSimilar(reader, oceanFeature);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * 从缓存中查找
     * @param feature
     * @param date
     * @throws IOException
     */
    public void searchFeatureCache(double[] feature, String date) throws IOException {
        LinkedHashMap<String, byte[]> dayFeatures = GenericOceanaiImageSearcher.features.get(date);
        searchByDay(feature, dayFeatures);
        //searchByDayMultiThread(feature, dayFeatures);
    }

    class FeatureCacheProducer implements Runnable{
        String threadName = "cache-producer";
        LinkedHashMap<String, byte[]> dayFeatures = null;
        int startIndex = 0;
        int endIndex = 0;
        private FeatureCacheProducer(LinkedHashMap<String, byte[]> dayFeatures) {
            this.dayFeatures = dayFeatures;
        }
        private FeatureCacheProducer(int id, LinkedHashMap<String, byte[]> dayFeatures, int startIndex, int endIndex) {
            this.threadName = "cache-producer-" + id;
            this.dayFeatures = dayFeatures;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
        @Override
        public void run() {
            if (dayFeatures == null || dayFeatures.isEmpty()) {
                return;
            }
            //long start = System.currentTimeMillis();
            for (String key : dayFeatures.keySet()) {
                featureCacheModels.add(new FeatureCacheModel(key, dayFeatures.get(key)));
                //System.out.println("Add one document, queue size is " + linkedBlockingQueue.size());
            }
            for (int i = 1; i < numThreadsQueue * 3; i++) {
                featureCacheModels.add(new FeatureCacheModel("", null));
                //System.out.println("Add one empty document, queue size is " + linkedBlockingQueue.size());
            }
            //producer_flag = 0;

            //System.out.println(threadName + " thead time used " + (System.currentTimeMillis() - start) + " flag is " + producer_flag);
        }
    }

    class FeatureCacheConsumer implements Runnable {
        private boolean consumeFinish = false;
        private String threadName = "";
        private double[] feature;
        public FeatureCacheConsumer(String threadName, double[] feature) {
            this.threadName = threadName;
            this.feature = feature;
        }
        @Override
        public void run() {
            byte[] tmpBytes;
            String tmpKey;
            FeatureCacheModel featureCacheModel;
            double tmpDistance;
            while (!consumeFinish) {
                try {
                    featureCacheModel = featureCacheModels.take();
                    //System.out.println(threadName + " consume one document, queue size is " + linkedBlockingQueue.size());
                    tmpBytes = featureCacheModel.getBytes();
                    //tmpId = featureCacheModel.getId();
                    tmpKey = featureCacheModel.getKey();
                    if (!tmpKey.equals("")) {
                        long start = System.currentTimeMillis();
                        tmpDistance = getDistance(feature, bytes2doubleArray(tmpBytes));
                        if (searchResults.size() < maxHits) {
                            searchResults.add(new SearchResult(tmpDistance, tmpKey));
                            if (tmpDistance > maxDistance) maxDistance = tmpDistance;
                        } else if (tmpDistance > searchResults.last().score) {
                            // if it is nearer to the sample than at least on of the current set:
                            // remove the last one ...
                            searchResults.remove(searchResults.last());
                            // add the new one ...
                            searchResults.add(new SearchResult(tmpDistance, tmpKey));
                            // and set our new distance border ...
                        }
                        //System.out.println(this.threadName + " add one face consume time is " + (System.currentTimeMillis() - start) + ", id is " + tmpId);
                    }else {
                        consumeFinish = true;
                    }

                } catch (InterruptedException e) {
                    e.getMessage();
                }
            }
        }
    }

    public void searchByDayMultiThread(double[] feature, LinkedHashMap<String, byte[]> dayFeatures) {
        LinkedList<FeatureCacheConsumer> tasks = new LinkedList<>();
        LinkedList<Thread> threads = new LinkedList<>();
        FeatureCacheConsumer featureCacheConsumer;
        Thread thread;
        Thread p = new Thread(new FeatureCacheProducer(dayFeatures));
        p.start();
        for (int i = 0; i < numThreads; i++) {
            featureCacheConsumer = new FeatureCacheConsumer("consumer-" + i, feature);
            thread = new Thread(featureCacheConsumer);
            thread.start();
            tasks.add(featureCacheConsumer);
            threads.add(thread);
        }

        for (Thread next : threads) {
            try {
                next.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void searchByDay(double[] feature, LinkedHashMap<String, byte[]> dayFeatures) {
        double tmpDistance;

        for (String key : dayFeatures.keySet()) {
            tmpDistance = getDistance(feature, bytes2doubleArray(dayFeatures.get(key)));
            assert (tmpDistance >= 0);
            if (tmpDistance < threshold) {
                continue;
            }
            // if the array is not full yet:
            if (results.size() < this.maxHits) {
                results.add(new SearchResult(tmpDistance, key));
                //if (tmpDistance < minDistance) minDistance = tmpDistance;
            } else if (tmpDistance > results.last().score) {
                // if it is nearer to the sample than at least on of the current set:
                // remove the last one ...
                results.remove(results.last());
                // add the new one ...
                results.add(new SearchResult(tmpDistance, key));
            }

        }
    }

    public String toString() {
        return "GenericSearcher using " + extractorItem.getExtractorClass().getName();
    }
}
