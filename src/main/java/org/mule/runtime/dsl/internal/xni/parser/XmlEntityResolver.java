/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xni.parser;

import static org.mule.runtime.dsl.internal.util.EntityResolverUtils.overrideSystemIdForCompatibility;
import static org.mule.runtime.dsl.internal.xml.parser.MuleSchemaMappingsLoader.getSchemaMappings;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


/**
 * Custom {@link XMLEntityResolver} that resolve entities over mule schemas.
 *
 * @since 1.4.0
 */
public class XmlEntityResolver implements XMLEntityResolver {

  private static final Logger LOGGER = getLogger(XmlEntityResolver.class);

  private static final String CUSTOM_SCHEMA_MAPPINGS_LOCATION = "META-INF/mule.schemas";

  private final Map<String, String> schemaMappings;

  public XmlEntityResolver() {
    this.schemaMappings = getSchemaMappings(CUSTOM_SCHEMA_MAPPINGS_LOCATION, XmlEntityResolver.class::getClassLoader);
  }

  @Override
  public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws XNIException, IOException {
    String publicId = resourceIdentifier.getPublicId();
    String systemId = resourceIdentifier.getExpandedSystemId();
    if (publicId == null && systemId == null)
      return null;
    systemId = overrideSystemIdForCompatibility(publicId, systemId, (pId, sId) -> schemaMappings.containsKey(pId));
    return resolveEntity(publicId, systemId);
  }

  private XMLInputSource resolveEntity(String publicId, String systemId) {
    String resourceLocation = schemaMappings.get(systemId);
    if (resourceLocation != null) {
      InputStream is = XmlEntityResolver.class.getClassLoader().getResourceAsStream(resourceLocation);
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
}
