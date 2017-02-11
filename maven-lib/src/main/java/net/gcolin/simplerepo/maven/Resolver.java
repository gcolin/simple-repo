/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package net.gcolin.simplerepo.maven;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.maven.model.Model;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve Maven properties.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
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
