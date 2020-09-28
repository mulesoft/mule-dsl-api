/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xni.parser;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.dsl.internal.util.SchemaMappingsUtils.CUSTOM_SCHEMA_MAPPINGS_LOCATION;
import static org.mule.runtime.dsl.internal.util.SchemaMappingsUtils.getSchemaMappings;
import static org.mule.runtime.dsl.internal.util.SchemaMappingsUtils.resolveSystemId;
import static org.slf4j.LoggerFactory.getLogger;
import static java.util.Optional.of;
import static java.util.Optional.empty;

import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Custom {@link XMLEntityResolver} that resolve entities over mule schemas.
 *
 * @since 1.4.0
 */
public class DefaultXmlEntityResolver implements XMLEntityResolver {

  private static final Logger LOGGER = getLogger(DefaultXmlEntityResolver.class);
  private static Boolean IS_RUNNING_TESTS;

  private final Map<String, String> schemas;
  private Optional<ClassLoader> classLoader = empty();
  private Map<String, String> appPluginsSchemaMappings = new HashMap<>();

  public DefaultXmlEntityResolver() {
    this.schemas = getSchemaMappings(CUSTOM_SCHEMA_MAPPINGS_LOCATION, DefaultXmlEntityResolver.class::getClassLoader);
    if (isRunningTests()) {
      this.classLoader = of(currentThread().getContextClassLoader());
      this.appPluginsSchemaMappings = getSchemaMappings(CUSTOM_SCHEMA_MAPPINGS_LOCATION, () -> classLoader.get());
    }
  }

  @Override
  public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws XNIException, IOException {
    String publicId = resourceIdentifier.getPublicId();
    String systemId = resourceIdentifier.getExpandedSystemId();
    if (publicId == null && systemId == null)
      return null;
    systemId =
        resolveSystemId(publicId, systemId, isRunningTests(),
                        (pId, sId) -> schemas.containsKey(sId) || appPluginsSchemaMappings.containsKey(sId));
    XMLInputSource xis = resolveEntity(schemas, publicId, systemId, DefaultXmlEntityResolver.class.getClassLoader());
    if (xis == null && isRunningTests()) {
      xis = resolveEntity(appPluginsSchemaMappings, publicId, systemId, classLoader.orElse(null));
    }
    return xis;
  }

  private XMLInputSource resolveEntity(Map<String, String> schemas, String publicId, String systemId, ClassLoader classLoader) {
    String resourceLocation = schemas.get(systemId);
    if (classLoader != null && resourceLocation != null) {
      InputStream is = classLoader.getResourceAsStream(resourceLocation);
      if (is == null) {
        LOGGER.debug("Couldn't find XML schema [" + systemId + "]: " + resourceLocation);
        return null;
      }
      XMLInputSource source = new XMLInputSource(publicId, systemId, null);
      source.setByteStream(is);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Found XML schema [" + systemId + "] in classpath: " + resourceLocation);
      }
      return source;
    } else {
      return null;
    }
  }

  private boolean isRunningTests() {
    if (IS_RUNNING_TESTS != null) {
      return IS_RUNNING_TESTS;
    }
    for (StackTraceElement element : new Throwable().getStackTrace()) {
      if (element.getClassName().startsWith("org.junit.runners.")) {
        IS_RUNNING_TESTS = true;
        return true;
      }
    }
    IS_RUNNING_TESTS = false;
    return false;
  }
}
