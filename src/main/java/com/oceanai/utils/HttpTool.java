package com.oceanai.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * restful api server tool.
 *
 * @author Xiong Raorao
 * @since 2018-05-02-10:40
 */
public class HttpTool {

  private static Logger logger = LoggerFactory.getLogger(HttpTool.class);
  static {
    System.setProperty("java.awt.headless", "true");
  }

  public static String readRequestData(HttpServletRequest request) throws IOException {
    BufferedReader br = null;
    StringBuilder sb = null;
    try {
      sb = new StringBuilder();
      br = request.getReader();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        br.close();
      }
    }
    return sb.toString();
  }

  // data:image/png;base64,
  public static BufferedImage base64ToBuff(String strHttpImg) {

    byte[] byImg = null;
    try {
      if (strHttpImg == null) {
        return null;
      } else {
        //BASE64Decoder decoder = new BASE64Decoder();
        Base64.Decoder decoder = Base64.getDecoder();
        //Base64解码
        //byImg = decoder.decodeBuffer(strHttpImg);
        byImg = decoder.decode(strHttpImg);

        /**
         * add by raorao, 测试png图片编码问题.
         * 测试通过，支持gif，png，bmp等多种格式问题.
         */
        ByteArrayInputStream bais;
        BufferedImage bi;
        //ByteArrayOutputStream baos;
        bais = new ByteArrayInputStream(byImg);
        //baos = new ByteArrayOutputStream();
        bi = ImageIO.read(bais);
        BufferedImage newBufferedImage = new BufferedImage(bi.getWidth(),
            bi.getHeight(), bi.TYPE_INT_RGB);
        newBufferedImage.createGraphics().drawImage(bi, 0, 0, Color.WHITE, null);
        return newBufferedImage;
        //ImageIO.write(newBufferedImage, "jpg", baos);
        //ImageIO.write(newBufferedImage, "jpg", new File("/home/hadoop/cloud/test.jpg"));
        //return baos.toByteArray();
      }

    } catch (Exception e) {
      logger.error("Error to decode Base64:" + e.getMessage());
      return null;
    }

  }

  public static List<String> getDays(String start, String end) {
    if (!start.matches("\\d{8}") || !end.matches("\\d{8}")) {
      return null;
    }
    String dateFormat = "yyyyMMdd";
    int interval = 0;
    try {
      interval = getInterval(start, end);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    logger.info("Date interval: " + interval);
    List<String> result = new ArrayList<>();
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    Date startDate = null;
    try {
      startDate = sdf.parse(start);
    } catch (ParseException e) {
      e.printStackTrace();
    }

    if (interval < 0) {
      return null;
    } else {
      for (int i = 0; i < interval; i++) {
        result.add(sdf.format(startDate));
        startDate = nextDay(startDate);
      }
      result.add(sdf.format(startDate));
    }
    return result;

  }

  private static Date nextDay(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    date = calendar.getTime();
    return date;
  }

  // 20180502, 20180504==> return 2
  private static int getInterval(String start, String end) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    Date date1 = sdf.parse(start);
    Date date2 = sdf.parse(end);
    Calendar cal = Calendar.getInstance();
    cal.setTime(date1);
    Date time1 = cal.getTime();
    cal.setTime(date2);
    Date time2 = cal.getTime();
    long between_days = (time2.getTime() - time1.getTime()) / (1000 * 3600 * 24);
    return Integer.parseInt(String.valueOf(between_days));
  }

}
