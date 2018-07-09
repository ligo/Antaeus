package com.oceanai.model;

/**
 * .
 *
 * @author Xiong Raorao
 * @since 2018-05-02-17:56
 */
public class SearchResult implements Comparable<SearchResult> {
  public double score;
  public String indexKey;

  public SearchResult(double score, String indexKey) {
    this.score = score;
    this.indexKey = indexKey;
  }

  @Override
  public int compareTo(SearchResult o) {
    return Double.compare( o.score,this.score);
  }
}
