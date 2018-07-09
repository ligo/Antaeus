package com.oceanai.feature;

import com.oceanai.model.Face;
import com.oceanai.model.Features;
import com.oceanai.model.LandMark;
import com.oceanai.model.SearchFeature;
import com.oceanai.utils.FaceTool;
import com.oceanai.utils.HttpClientUtil;
import com.oceanai.utils.ImageUtils;
import net.semanticmetadata.lire.imageanalysis.features.Extractor;
import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.imageanalysis.features.LireFeature;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

//import sun.misc.BASE64Encoder;

/**
 * OceanFeature 为魅瞳科技有限公司研发的通过深度学期提取精密特征的算法。
 *
 * @author: WangRupeng, wangrupeng@live.cn
 */

public class OceanFeature implements GlobalFeature, Extractor {
    public boolean Compact = false;
    public static String FIELD_NAME_OCENAI_FEATURE = "OceanaiFeature";
    protected double[] histogram = new double[256];
    private FaceTool faceTool;
    //private BASE64Encoder encoder;
    private Base64.Encoder encoder;


    // Constructor
    public OceanFeature() {
        faceTool = FaceTool.getInstance();
        //encoder = new BASE64Encoder();
        encoder = Base64.getEncoder();
    }

    /**
     * 通过调用Verifier的API接口检测人脸并且从BufferedImage对象中提取feature。
     * @param image
     * @return
     */
    public double[] ApplyNewFeature(BufferedImage image) {
        try {
            double[] featureDouble = new double[256];
            byte[] bytes =  com.oceanai.utils.ImageUtils.imageToBytes(image, "jpg");
            //String base64 = encoder.encode(bytes);
            String base64 = encoder.encodeToString(bytes);
            HttpClientUtil.setFunction("detect");
            List<SearchFeature> searchFeatureList = faceTool.detectFace(base64);

            if (searchFeatureList.size() == 0) {
                System.out.println("Detect no face");
                return null;
            }
            SearchFeature searchFeature = searchFeatureList.get(0);
            image = getSubImage(image, searchFeature.bbox);
            //com.oceanai.utils.ImageUtils.saveToFile(image, "C:\\Users\\wangr\\Desktop\\faces\\", UUID.randomUUID() + "", "jpg");
            //base64 = encoder.encode(com.oceanai.utils.ImageUtils.imageToBytes(image, "jpg"));
            base64 = encoder.encodeToString(com.oceanai.utils.ImageUtils.imageToBytes(image, "jpg"));
            //LandMark landMark = processLandMark(searchFeature.landMark, searchFeature.bbox.left_top.x, searchFeature.bbox.left_top.y);
            List<Face> imagesBase64 = new ArrayList<>();
            imagesBase64.add(new Face(base64, searchFeature.landMark));
            HttpClientUtil.setFunction("feature");
            Features features = faceTool.getNewFeatures(imagesBase64);
            if (features.getError_message().equals("701")) {
                featureDouble = features.getFeature()[0];
            }
            return featureDouble;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public double[] ApplyFeature(BufferedImage image) {
        try {
            byte[] bytes =  ImageUtils.imageToBytes(image, "jpg");
            //String base64 = encoder.encode(bytes);
            String base64 = encoder.encodeToString(bytes);
            HttpClientUtil.setFunction("detect");
            List<SearchFeature> searchFeatureList = faceTool.detectFace(base64);
            double[] featureDouble = new double[512];
            if (searchFeatureList.size() == 0) {
                return featureDouble;
            }
            SearchFeature searchFeature = searchFeatureList.get(0);
            image = getSubImage(image, searchFeature.bbox);
            //com.oceanai.utils.ImageUtils.saveToFile(image, "C:\\Users\\wangr\\Desktop\\faces\\", UUID.randomUUID() + "", "jpg");
            //base64 = encoder.encode(com.oceanai.utils.ImageUtils.imageToBytes(image, "jpg"));
            base64 = encoder.encodeToString(ImageUtils.imageToBytes(image, "jpg"));
            List<String> imagesBase64 = new ArrayList<>();
            imagesBase64.add(base64);
            HttpClientUtil.setFunction("feature");
            Features features = faceTool.getFeatures(imagesBase64);
            if (features.getError_message().equals("701")) {
                featureDouble = features.getFeature()[0];
            }
            return featureDouble;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private LandMark processLandMark(LandMark landMark, int x, int y) {
        int[] originX = landMark.getX();
        int[] originY = landMark.getY();
        int[] X = new int[originX.length];
        int[] Y = new int[originY.length];
        for (int i = 0;i< originX.length;i++) {
            X[i] = originX[i] - x;
        }
        for (int i = 0;i< originY.length;i++) {
            Y[i] = originY[i] - y;
        }

        return new LandMark(X, Y);
    }

    private BufferedImage getSubImage(BufferedImage image, SearchFeature.BBox box) {
        int width = box.right_down.x - box.left_top.x;
        int height = box.right_down.y - box.left_top.y;
        BufferedImage temp = image.getSubimage(box.left_top.x, box.left_top.y, width, height);
        return temp;
    }

    public boolean ApplyFeature(double[] feature) {
        if (feature.length != 256) {
            return false;
        }

        for (int i = 0;i < 256;i++) {
            histogram[i] = feature[i];
        }

        return true;
    }
    /**
     * 从BufferedImage对象中提取feature，主要有ApplyFeature()函数实现。
     * @param bimg
     */
    @Override
    public void extract(BufferedImage bimg) {
        bimg = ImageUtils.get8BitRGBImage(bimg);
        //histogram = Apply(bimg);
        histogram = ApplyNewFeature(bimg);
        if (histogram == null) {
            histogram = new double[256];
        }
    }

    /**
     * Creates a small byte array from an OceanFeature descriptor.
     *
     * @return
     */
    @Override
    public byte[] getByteArrayRepresentation() {
        int times = Double.SIZE / Byte.SIZE;
        byte[] bytes = new byte[histogram.length * times];
        for(int i=0;i<histogram.length;i++){
            ByteBuffer.wrap(bytes, i*times, times).putDouble(histogram[i]);
        }
        //bytes = gZip(bytes);
        try {
            //bytes = Snappy.compress(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public void setHistogram(double[] feature) {
        this.histogram = feature;
    }

    /**
     * Reads descriptor from a byte array. Much faster than the String based method.
     *
     * @param in byte array from corresponding method
     */
    @Override
    public void setByteArrayRepresentation(byte[] in) {
        //in = unGZip(in);
        try {
            //in = Snappy.uncompress(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setByteArrayRepresentation(in, 0, in.length);
    }

    @Override
    public void setByteArrayRepresentation(byte[] in, int offset, int length) {
        int times = Double.SIZE / Byte.SIZE;
        double[] doubles = new double[length / times];
        if (doubles.length != histogram.length) {
            return;
        }
        for(int i=0;i<doubles.length;i++){
            doubles[i] = ByteBuffer.wrap(in, i*times, times).getDouble();
            histogram[i] = doubles[i];
        }
    }

    @Override
    public double[] getFeatureVector() {
        return histogram;
    }

    @Override
    public double getDistance(LireFeature vd) {
        if (!(vd instanceof OceanFeature))// Check if instance of the right class ...
            throw new UnsupportedOperationException("Wrong descriptor.");

        // casting ...
        OceanFeature ch = (OceanFeature) vd;

        // check if parameters are fitting ...
        if ((ch.histogram.length != histogram.length))
            throw new UnsupportedOperationException("Histogram lengths or color spaces do not match");

        //double result = cosineSimilarity(ch.histogram, histogram);

        double result = FaceTool.cosineSimilarity(ch.histogram, histogram);
        //result = (1 - result) * 100;
        result = result * 100;
        return result;
    }

    //计算cos距离
/*    static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0d;
        double normA = 0.0d;
        double normB = 0.0d;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        double result = 0d;
        if (normB != 0 && normA != 0) {
             result = (0.5 + 0.5 * (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))));
             //result = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
             //System.out.println();
        } else if (normB == 0) {
            System.out.println("The image to search cannot be extracted feature!");
        }

        return result;
    }*/

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(histogram.length * 2 + 25);
        for (double aData : histogram) {
            sb.append((int) aData);
            sb.append(' ');
        }
        return "OceanFeature{" + sb.toString().trim() + "}";
    }

    @Override
    public String getFeatureName() {
        return "OceanFeature";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME_OCENAI_FEATURE;
    }
}

