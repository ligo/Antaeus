# 创建 Extractor

在我们自定义Extractor之前需要了解两个官方接口Extractor和LireFeature。Extractor只有一个函数extractor(),只有一个参数
BufferedImage对象。

## LIRe-Extractor.java
从BufferedImage中提取图像的feature。
```java
public interface Extractor {

    /**
     * 从BufferedImage对象中提取feature。
     * @param image the source image
     */
    public void extract(BufferedImage image);
}
```

## LIRe-LireFeature.java

```java
public interface LireFeature extends FeatureVector {
    /**
     * Gives a descriptive name of the feature, i.e. a name to show up in benchmarks, menus, UIs, etc.
     * @return the name of the feature.
     */
    public String getFeatureName();

    /**
     * Returns the preferred field name for indexing.
     * @return the field name preferred for indexing in a Lucene index.
     */
    public String getFieldName();

    /**
     * Returns a compact byte[] based representation of the feature vector.
     * 获取index中由feature double数组转化的byte数组
     * @return a compact byte[] array containing the feature vector.
     * @see LireFeature#setByteArrayRepresentation(byte[])
     */
    public byte[] getByteArrayRepresentation();

    /**
     * Sets the feature vector values based on the byte[] data. Use
     * {@link LireFeature#getByteArrayRepresentation()}
     * to generate a compatible byte[] array.
     * feature存储到index文件中之前，需要将double数组压缩为byte数组，可以节省空间。
     * @param featureData the byte[] data.
     * @see LireFeature#getByteArrayRepresentation()
     */
    public void setByteArrayRepresentation(byte[] featureData);

    /**
     * Sets the feature vector values based on the byte[] data.
     * Use {@link LireFeature#getByteArrayRepresentation()}
     * to generate a compatible byte[] array.
     * @param featureData the byte[] array containing the data.
     * @param offset the offset, i.e. where the feature vector starts.
     * @param length the length of the data representing the feature vector.
     * @see LireFeature#getByteArrayRepresentation()
     */
    public void setByteArrayRepresentation(byte[] featureData, int offset, int length);

    /**
     * The distance function for this type of feature
     * 计算距离
     * @param feature the feature vector to compare the current instance to.
     * @return the distance (or dissimilarity) between the instance and the parameter.
     */
    double getDistance(LireFeature feature);
}

```

## LIRe-GlobalFeature.java

```java
public interface GlobalFeature extends LireFeature, Extractor {

}
```

## [OceanFeature.java](src/main/java/com/oceanai/imageanalysis/feature/global/OceanFeature.java)

由我们自己算法构造的Extractor介绍
```java
/**
 * OceanFeature 为魅瞳科技有限公司研发的通过深度学期提取精密特征的算法。
 *
 * @author: WangRupeng, wangrupeng@live.cn
 */

public class OceanFeature implements GlobalFeature {
    public boolean Compact = false;
    protected double[] histogram = new double[512];
    private FaceTool faceTool;
    private BASE64Encoder encoder;
    int tmp;

    // Constructor
    public OceanFeature() {
        faceTool = FaceTool.getInstance();
        encoder = new BASE64Encoder();
    }

    /**
     * 通过调用Verifier的API接口检测人脸并且从BufferedImage对象中提取feature。
     * @param image
     * @return
     */
    public double[] ApplyFeature(BufferedImage image) {
       //详见代码
    }

    /**
     * 从BufferedImage对象中提取feature，主要有ApplyFeature()函数实现。
     * @param bimg
     */
    @Override
    public void extract(BufferedImage bimg) {
        bimg = ImageUtils.get8BitRGBImage(bimg);
        //histogram = Apply(bimg);
        histogram = ApplyFeature(bimg);
    }

    /**
     * Creates a small byte array from an OceanFeature descriptor.
     *
     * @return
     */
    @Override
    public byte[] getByteArrayRepresentation() {
       //详见代码
    }

    /**
     * Reads descriptor from a byte array. Much faster than the String based method.
     *
     * @param in byte array from corresponding method
     * @see CEDD#getByteArrayRepresentation
     */
    @Override
    public void setByteArrayRepresentation(byte[] in) {
        setByteArrayRepresentation(in, 0, in.length);
    }

    @Override
    public void setByteArrayRepresentation(byte[] in, int offset, int length) {
        //详见代码
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

        double result = cosineSimilarity(ch.histogram, histogram);

        result = (1 - result) * 100;
        return result;
    }

    //计算cos距离
    static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        //详见代码
    }

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
        return DocumentBuilder.FIELD_NAME_FCTH;
    }
}

```
