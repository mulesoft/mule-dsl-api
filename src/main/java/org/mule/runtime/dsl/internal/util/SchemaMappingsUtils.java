/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.util;

import static org.mule.runtime.api.util.classloader.MuleImplementationLoaderUtils.isResolveMuleImplementationLoadersDynamically;
import static org.mule.runtime.dsl.internal.util.CollectionUtils.mergePropertiesIntoMap;
import static org.mule.runtime.dsl.internal.util.ResourceUtils.useCachesIfNecessary;
import static org.mule.runtime.dsl.internal.util.SchemasConstants.CORE_XSD;
import static org.mule.runtime.dsl.internal.util.SchemasConstants.CORE_CURRENT_XSD;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.classloader.MuleImplementationLoaderUtils;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * A helper class for loading mule schema mappings.
 *
 * @since 1.4.0
 */
public final class SchemaMappingsUtils {

  public static final String CUSTOM_SCHEMA_MAPPINGS_LOCATION = "META-INF/mule.schemas";
  public static final String CUSTOM_SPRING_SCHEMA_MAPPINGS_LOCATION = "META-INF/spring.schemas";

  private static final Logger LOGGER = getLogger(SchemaMappingsUtils.class);
  private static final LazyValue<Map<String, String>> MULE_SCHEMAS_MAPPINGS =
      new LazyValue<>(() -> getSchemaMappings(CUSTOM_SCHEMA_MAPPINGS_LOCATION,
                                              MuleImplementationLoaderUtils::getMuleImplementationsLoader));
  private static final LazyValue<Map<String, String>> SPRING_SCHEMAS_MAPPINGS =
      new LazyValue<>(() -> getSchemaMappings(CUSTOM_SPRING_SCHEMA_MAPPINGS_LOCATION,
                                              MuleImplementationLoaderUtils::getMuleImplementationsLoader));

  private SchemaMappingsUtils() {}

  public static String resolveSystemId(String systemId) {
    if (systemId.equals(CORE_XSD)) {
      return CORE_CURRENT_XSD;
    } else if (systemId.contains("spring")) {
      // [MULE-16572] This is to support importing Spring xsd's from custom xsd's. Compatibility module does such thing.
      return systemId.replace("-current.xsd", ".xsd");
    } else {
      return systemId;
    }
  }

  /**
   * @return schemas mappings located at {@code CUSTOM_SCHEMA_MAPPINGS_LOCATION} location
   */
  public static Map<String, String> getMuleSchemasMappings() {
    if (isResolveMuleImplementationLoadersDynamically()) {
      return getSchemaMappings(CUSTOM_SCHEMA_MAPPINGS_LOCATION, MuleImplementationLoaderUtils::getMuleImplementationsLoader);
    } else {
      return MULE_SCHEMAS_MAPPINGS.get();
    }
  }

  /**
   * @return schemas mappings located at {@code CUSTOM_SPRING_SCHEMA_MAPPINGS_LOCATION} location
   */
  public static Map<String, String> getSpringSchemasMappings() {
    if (isResolveMuleImplementationLoadersDynamically()) {
      return getSchemaMappings(CUSTOM_SPRING_SCHEMA_MAPPINGS_LOCATION,
                               MuleImplementationLoaderUtils::getMuleImplementationsLoader);
    } else {
      return SPRING_SCHEMAS_MAPPINGS.get();
    }
  }

  /**
   * Load schemas mappings for a given {@code schemaMappingsLocation} location
   *
   * @param schemaMappingsLocation schema mappings location to load
   * @param classLoader            {@link Supplier} the ClassLoader to use for loading schemas
   * @return a {@link Map} schemas mappings
   */
  public static Map<String, String> getSchemaMappings(String schemaMappingsLocation, Supplier<ClassLoader> classLoader) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Loading schema mappings from [" + schemaMappingsLocation + "]");
    }
    try {
      Properties appPluginsMappings = loadAllProperties(schemaMappingsLocation, classLoader);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Loaded schema mappings: " + appPluginsMappings);
      }
      Map<String, String> schemaMappings = new HashMap<>(appPluginsMappings.size());
      mergePropertiesIntoMap(appPluginsMappings, schemaMappings);
      return schemaMappings;
    } catch (IOException ex) {
      throw new IllegalStateException(
                                      "Unable to load schema mappings from location [" + schemaMappingsLocation + "]",
                                      ex);
    }
  }

  /**
   * Load all properties from the specified class path resource (in ISO-8859-1 encoding), using the given class loader.
   * <p>
   * Merges properties if more than one resource of the same name found in the class path.
   * 
   * @param resourceName the name of the class path resource
   * @param classLoader  {@link Supplier} the ClassLoader to use for loading (or {@code null} to use the default class loader)
   * @return the populated Properties instance
   * @throws IOException if loading failed
   */
  private static Properties loadAllProperties(String resourceName, Supplier<ClassLoader> classLoader) throws IOException {
    ClassLoader classLoaderToUse = classLoader.get();
    Enumeration<URL> urls =
        (classLoaderToUse != null ? classLoaderToUse.getResources(resourceName) : ClassLoader.getSystemResources(resourceName));
    Properties props = new Properties();
    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();
      URLConnection con = url.openConnection();
      useCachesIfNecessary(con);
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

}
