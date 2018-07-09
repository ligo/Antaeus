
package net.semanticmetadata.lire.lire;

import net.semanticmetadata.lire.builders.GlobalDocumentBuilder;
import com.oceanai.feature.OceanFeature;
import net.semanticmetadata.lire.utils.FileUtils;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Simple class showing the process of indexing
 * @author WangRupeng, wangrupeng@live.cn
 */
public class Indexer {
    public static void main(String[] args) throws IOException {
        // Checking if arg[0] is there and if it is a directory.
        boolean passed = false;
        if (args.length > 0) {
            File f = new File(args[0]);
            System.out.println("Indexing images in " + args[0]);
            if (f.exists() && f.isDirectory()) passed = true;
        }
        if (!passed) {
            System.out.println("No directory given as first argument.");
            System.out.println("Run \"Indexer <directory>\" to index files of a directory.");
            System.exit(1);
        }
        // Getting all images from a directory and its sub directories.
        ArrayList<String> images = FileUtils.getAllImages(new File(args[0]), true);

        GlobalDocumentBuilder globalDocumentBuilder = new GlobalDocumentBuilder(false, false);
        //BitSampling.generateHashFunctions("D:\\OceanAI\\gitlab\\Antaeus\\out\\LshBitSampling.obj");
        //LocalitySensitiveHashing.readHashFunctions();
        //GlobalDocumentBuilder globalDocumentBuilder = new GlobalDocumentBuilder(true, GlobalDocumentBuilder.HashingMode.LSH, false);
        /*
            If you want to use DocValues, which makes linear search much faster, then use.
            However, you then === to use a specific searcher!
         */
        // GlobalDocumentBuilder globalDocumentBuilder = new GlobalDocumentBuilder(false, true);

        /*
            Then add those features we want to extract in a single run:
         */
        globalDocumentBuilder.addExtractor(OceanFeature.class);
        //globalDocumentBuilder.addExtractor(FCTH.class);
        //globalDocumentBuilder.addExtractor(AutoColorCorrelogram.class);

        // Creating an Lucene IndexWriter
        IndexWriterConfig conf = new IndexWriterConfig(new WhitespaceAnalyzer());
        IndexWriter iw = new IndexWriter(FSDirectory.open(Paths.get("index/index_oceanai")), conf);
        // Iterating through images building the low level features
        for (Iterator<String> it = images.iterator(); it.hasNext();) {
            String imageFilePath = it.next();
            System.out.println("Indexing " + imageFilePath);
            try {
                BufferedImage img = ImageIO.read(new FileInputStream(imageFilePath));
                if (img == null) {
                    continue;
                }
                Document document = globalDocumentBuilder.createDocument(img, imageFilePath);
                iw.addDocument(document);
            } catch (Exception e) {
                System.err.println("Error reading image or indexing it.");
                e.printStackTrace();
            }
        }
        // closing the IndexWriter
        iw.close();
        System.out.println("Finished indexing " + images.size() + " images.");
    }
}
