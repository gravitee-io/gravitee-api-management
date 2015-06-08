/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.common.utils;

import com.google.common.base.Strings;

import java.util.Properties;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public final class PropertiesUtils {

  private PropertiesUtils() {
  }

  public static String getProperty(final Properties properties, final String propertyName) {
    return (String) getProperty(properties, propertyName, false);
  }

  public static Integer getPropertyAsInteger(final Properties properties, final String propertyName) {
    return (Integer) getProperty(properties, propertyName, true);
  }

  public static Object getProperty(final Properties properties, final String propertyName, final boolean isNumber) {
    final String propertyValue = properties.getProperty(propertyName);
    if (Strings.isNullOrEmpty(propertyValue)) {
      throw new IllegalStateException("Missing property configuration:" + propertyName);
    }
    try {
      if (isNumber) {
        return Integer.parseInt(propertyValue);
      }
      return propertyValue;
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException("The property must be a valid number:" + propertyName, e);
    }
  }
}
