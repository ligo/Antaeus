package com.oceanai.model;

/**
 * search 接口的model.
 *
 * @author Xiong Raorao
 * @since 2018-05-02-10:35
 */
public class SearchBean {
  public double[] feature;
  public int top = 30;
  public double threshold = 0.0;
  public String startTime = "20180527";
  public String endTime = "20180528";
  public String[] video_name;
}
