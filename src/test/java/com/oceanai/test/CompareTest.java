package com.oceanai.test;

import com.oceanai.model.Face;
import com.oceanai.model.Features;
import com.oceanai.model.LandMark;
import com.oceanai.model.SearchFeature;
import com.oceanai.utils.FaceTool;
import com.oceanai.utils.HttpClientUtil;
import org.codehaus.jettison.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CompareTest {
    //private static BASE64Encoder encoder = new BASE64Encoder();
    private static Base64.Encoder encoder = Base64.getEncoder();
    public static void main(String[] args) {
        try {
            BufferedImage bufferedImage1 = ImageIO.read(new File("C:\\Users\\wangr\\Desktop\\test\\dakui.jpg"));
            BufferedImage bufferedImage2 = ImageIO.read(new File("C:\\Users\\wangr\\Desktop\\test\\dakui_origin.jpg"));

            SearchFeature searchFeature1 = getBase64(bufferedImage1);
            SearchFeature searchFeature2 = getBase64(bufferedImage2);

            BufferedImage image1 = getSubImage(bufferedImage1, searchFeature1.bbox);
            BufferedImage image2 = getSubImage(bufferedImage2, searchFeature2.bbox);

            String base1 =  encoder.encodeToString(com.oceanai.utils.ImageUtils.imageToBytes(image1, "jpg"));
            String base2 = encoder.encodeToString(com.oceanai.utils.ImageUtils.imageToBytes(image2, "jpg"));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("api_key", "");
            jsonObject.put("image1_base64", base1);
            jsonObject.put("image2_base64", base2);
            HttpClientUtil.setFunction("compare");
            String result = HttpClientUtil.post(jsonObject);
            System.out.println(result);

            double[] feature1 = getFeature(searchFeature1, bufferedImage1);
            double[] feature2 = getFeature(searchFeature2, bufferedImage2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SearchFeature getBase64(BufferedImage image) {
        try {
            byte[] bytes = com.oceanai.utils.ImageUtils.imageToBytes(image, "jpg");
            //BASE64Encoder encoder = new BASE64Encoder();
            Base64.Encoder encoder = Base64.getEncoder();
            FaceTool faceTool = FaceTool.getInstance();
            String base64 = encoder.encodeToString(bytes);
            HttpClientUtil.setFunction("detect");
            List<SearchFeature> searchFeatureList = faceTool.detectFace(base64);
            double[] featureDouble = new double[256];
            if (searchFeatureList.size() == 0) {
                System.out.println("Detect no face");
            }
            SearchFeature searchFeature = searchFeatureList.get(0);
            return searchFeature;
            /*image = getSubImage(image, searchFeature.bbox);
            //com.oceanai.utils.ImageUtils.saveToFile(image, "C:\\Users\\wangr\\Desktop\\faces\\", UUID.randomUUID() + "", "jpg");
            base64 = encoder.encode(com.oceanai.utils.ImageUtils.imageToBytes(image, "jpg"));
            return base64;*/
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static double[] getFeature(SearchFeature searchFeature, BufferedImage image) throws IOException {
        image = getSubImage(image, searchFeature.bbox);
        //com.oceanai.utils.ImageUtils.saveToFile(image, "C:\\Users\\wangr\\Desktop\\faces\\", UUID.randomUUID() + "", "jpg");
        String base64 = encoder.encodeToString(com.oceanai.utils.ImageUtils.imageToBytes(image, "jpg"));
        LandMark landMark = processLandMark(searchFeature.landMark, searchFeature.bbox.left_top.x, searchFeature.bbox.left_top.y);
        List<Face> imagesBase64 = new ArrayList<>();
        imagesBase64.add(new Face(base64, landMark));
        HttpClientUtil.setFunction("feature");
        FaceTool faceTool = new FaceTool();
        Features features = faceTool.getNewFeatures(imagesBase64);
        double[] featureDouble = null;
        if (features.getError_message().equals("701")) {
            featureDouble = features.getFeature()[0];
        }
        return featureDouble;
    }

    private static LandMark processLandMark(LandMark landMark, int x, int y) {
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

    private static BufferedImage getSubImage(BufferedImage image, SearchFeature.BBox box) {
        int width = box.right_down.x - box.left_top.x;
        int height = box.right_down.y - box.left_top.y;
        BufferedImage temp = image.getSubimage(box.left_top.x, box.left_top.y, width, height);
        return temp;
    }
}
