/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xni.parser;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.mule.runtime.dsl.api.xni.parser.SchemaProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class DefaultSchemaProvider implements SchemaProvider {

  private static final Logger LOGGER = getLogger(DefaultSchemaProvider.class);
  private static final String CUSTOM_SCHEMA_MAPPINGS_LOCATION = "META-INF/mule.schemas";

  private final Map<String, String> muleSchemaMappings;

  public DefaultSchemaProvider() {
    this.muleSchemaMappings = getMuleSchemaMappings();
  }

  /**
   * Load the specified schema mappings.
   */
  private Map<String, String> getMuleSchemaMappings() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Loading schema mappings from [" + CUSTOM_SCHEMA_MAPPINGS_LOCATION + "]");
    }
    try {
      Properties muleMappings =
          loadAllProperties(CUSTOM_SCHEMA_MAPPINGS_LOCATION, DefaultSchemaProvider.class.getClassLoader());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Loaded Mule schema mappings: " + muleMappings);
      }
      Map<String, String> schemaMappings = new HashMap<>(muleMappings.size());

      mergePropertiesIntoMap(muleMappings, schemaMappings);
      return schemaMappings;
    } catch (IOException ex) {
      throw new IllegalStateException(
                                      "Unable to load schema mappings from location [" + CUSTOM_SCHEMA_MAPPINGS_LOCATION + "]",
                                      ex);
    }
  }

  private <K, V> void mergePropertiesIntoMap(Properties props, Map<K, V> map) {
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

  private Properties loadAllProperties(String resourceName, ClassLoader classLoader) throws IOException {
    ClassLoader classLoaderToUse = classLoader;
    // TODO: ML Se necesita???
    /*
    if (classLoaderToUse == null) {
        classLoaderToUse = ClassUtils.getDefaultClassLoader();
    }
     */
    Enumeration<URL> urls =
        (classLoaderToUse != null ? classLoaderToUse.getResources(resourceName) : ClassLoader.getSystemResources(resourceName));
    Properties props = new Properties();
    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();
      URLConnection con = url.openConnection();
      // TODO: ML Se necesita ???
      // ResourceUtils.useCachesIfNecessary(con);
      InputStream is = con.getInputStream();
      try {
        if (resourceName != null && resourceName.endsWith(".xml")) {
          props.loadFromXML(is);
        } else {
          props.load(is);
        }
      } finally {
        is.close();
      }
    }
    return props;
  }

  @Override
  public List<XMLInputSource> getSchemas() {
    return muleSchemaMappings.entrySet().stream()
        .map(entry -> {
          String systemId = entry.getKey();
          String resourceLocation = entry.getValue();
          XMLInputSource xis = null;
          InputStream is = DefaultSchemaProvider.class.getClassLoader().getResourceAsStream(resourceLocation);
          if (is == null) {
            LOGGER.debug("Couldn't find XML schema [" + systemId + "]: " + resourceLocation);
          } else {
            xis = new XMLInputSource(null, entry.getKey(), null);
            xis.setByteStream(is);
          }
          return ofNullable(xis);
        })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }
}
