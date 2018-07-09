package com.oceanai.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

/**
 * HttpTool Tester.
 *
 * @author Xiong Raorao <xiongraorao@gmail.com>
 * @version 1.0
 * @since <pre>05/02/2018</pre>
 */
public class HttpToolTest {

  @Before
  public void before() throws Exception {
  }

  @After
  public void after() throws Exception {
  }

  /**
   * Method: readRequestData(HttpServletRequest request)
   */
  @Test
  public void testReadRequestData() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: base64ToBuff(String strHttpImg)
   */
  @Test
  public void testBase64ToBuff() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: getDays(String start, String end)
   */
  @Test
  public void testGetDays() throws Exception {
    List<String> result = new ArrayList<>();
    String start = "20170524";
    String end = "20170527";
    result = HttpTool.getDays(start, end);
    Iterator<String> it = result.iterator();
    while (it.hasNext()) {
      System.out.println(it.next());
    }
    System.out.println(result.size());
//TODO: Test goes here... 
  }

  /**
   * Method: getInterval(String start, String end)
   */
  @Test
  public void testGetInterval() throws Exception {
//TODO: Test goes here... 
  }


  /**
   * Method: nextDay(Date date)
   */
  @Test
  public void testNextDay() throws Exception {
//TODO: Test goes here... 
/* 
try { 
   Method method = HttpTool.getClass().getMethod("nextDay", Date.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/
  }

} 
