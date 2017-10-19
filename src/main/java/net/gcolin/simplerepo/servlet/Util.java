package net.gcolin.simplerepo.servlet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Util {

  public static String encode(String value) {
    try {
      return URLEncoder.encode(value, "utf-8");
    } catch (UnsupportedEncodingException ex) {
      return "";
    }
  }

}
