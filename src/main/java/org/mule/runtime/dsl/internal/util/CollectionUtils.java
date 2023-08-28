/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.util;

import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * Miscellaneous collection utility methods.
 *
 * @since 1.4.0
 */
public class CollectionUtils {

  private CollectionUtils() {}

  /**
   * Merge the given Properties instance into the given Map, copying all properties (key-value pairs) over.
   * <p>
   * Uses {@code Properties.propertyNames()} to even catch default properties linked into the original Properties instance.
   * 
   * @param props the Properties instance to merge (may be {@code null})
   * @param map   the target Map to merge the properties into
   */
  public static <K, V> void mergePropertiesIntoMap(Properties props, Map<K, V> map) {
    if (props != null) {
      for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements();) {
        String key = (String) en.nextElement();
        Object value = props.get(key);
        if (value == null) {
          // Allow for defaults fallback or potentially overridden accessor...
          value = props.getProperty(key);
        }
        map.put((K) key, (V) value);
      }
    }
  }
}
