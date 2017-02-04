package net.gcolin.server.maven.plugin.search;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.maven.model.Model;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Resolver {

  private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{(.*)\\}");

  public static String resolve(String value, Properties props, Model model) {
    if(value == null) {
      return null;
    }
    Matcher matcher = VAR_PATTERN.matcher(value);
    StringBuffer result = new StringBuffer();
    while(matcher.find()) {
      String expr = matcher.group(1);
      if(props.containsKey(expr)) {
        matcher.appendReplacement(result, props.getProperty(expr));
      } else {
        if(expr.startsWith("project.")) {
          expr = expr.substring(8);
        }
        try {
          matcher.appendReplacement(result, String.valueOf(BeanUtils.getProperty(model, expr)));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
          throw new IllegalArgumentException(e);
        }
      }
    }
    matcher.appendTail(result);
    return result.toString();
  }

}
