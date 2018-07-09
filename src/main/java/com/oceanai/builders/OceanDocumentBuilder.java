package com.oceanai.builders;

import com.oceanai.feature.OceanFeature;
import com.oceanai.searcher.GenericOceanaiImageSearcher;
import net.semanticmetadata.lire.builders.DocumentBuilder;
import org.apache.lucene.document.*;
import org.apache.lucene.util.BytesRef;

public class OceanDocumentBuilder {

    private boolean docsCreated = false;
    private boolean useDocValues = false;
    private OceanFeature oceanFeature = new OceanFeature();

    /**
     * 创建index document的描述域
     * @return
     */
    public Field createDescriptorFields() {
        docsCreated = true;
        Field field = null;

        if (!useDocValues) {
            // TODO: Stored field is compressed and upon search decompression takes a lot of time (> 50% with a small index with 50k images). Find something else ...
            field = new StoredField(OceanFeature.FIELD_NAME_OCENAI_FEATURE, new BytesRef(oceanFeature.getByteArrayRepresentation()));
        } else {
            // Alternative: The DocValues field. It's extremely fast to read, but it's all in RAM most likely.
            field = new BinaryDocValuesField(OceanFeature.FIELD_NAME_OCENAI_FEATURE, new BytesRef(oceanFeature.getByteArrayRepresentation()));
        }
        return field;
    }

    /**
     * 创建document
     * @param feature 图片的特征数组
     * @param identifier 描述
     * @return 生成的document
     */
    public Document createDocument(double[] feature, String identifier) {
        oceanFeature.ApplyFeature(feature);
        Document doc = new Document();
        if (identifier != null) {
            GenericOceanaiImageSearcher.todayFeatures.put(identifier, oceanFeature.getByteArrayRepresentation());
            doc.add(new StringField(DocumentBuilder.FIELD_NAME_IDENTIFIER, identifier, Field.Store.YES));
        }
        Field field = createDescriptorFields();
        doc.add(field);
        return doc;
    }
}
