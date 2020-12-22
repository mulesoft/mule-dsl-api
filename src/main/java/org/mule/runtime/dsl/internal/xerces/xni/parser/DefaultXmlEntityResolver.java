/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import static org.mule.runtime.dsl.internal.util.SchemaMappingsUtils.getMuleSchemasMappings;
import static org.mule.runtime.dsl.internal.util.SchemaMappingsUtils.resolveSystemId;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.util.XMLResourceIdentifierImpl;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * Custom {@link XMLEntityResolver} that resolve entities over mule schemas.
 *
 * @since 1.4.0
 */
public class DefaultXmlEntityResolver implements XMLEntityResolver {

  private static final Logger LOGGER = getLogger(DefaultXmlEntityResolver.class);

  private final Map<String, String> schemas;

  public DefaultXmlEntityResolver() {
    this.schemas = getMuleSchemasMappings();
  }

  @Override
  public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws XNIException, IOException {
    String publicId = resourceIdentifier.getPublicId();
    String systemId = resourceIdentifier.getExpandedSystemId();
    if (publicId == null && systemId == null)
      return null;
    systemId = resolveSystemId(systemId);
    return resolveEntity(schemas, publicId, systemId);
  }

  private XMLInputSource resolveEntity(Map<String, String> schemas, String publicId, String systemId) {
    String resourceLocation = schemas.get(systemId);
    if (resourceLocation != null) {
      URL resource = DefaultXmlEntityResolver.class.getClassLoader().getResource(resourceLocation);
      if (resource == null) {
        LOGGER.debug("Couldn't find schema [" + systemId + "]: " + resourceLocation);
      } else {
        try {
          URLConnection connection = resource.openConnection();
          connection.setUseCaches(false);
          InputStream is = connection.getInputStream();
          XMLResourceIdentifier resourceIdentifier = new XMLResourceIdentifierImpl();
          resourceIdentifier.setPublicId(publicId);
          resourceIdentifier.setLiteralSystemId(systemId);
          resourceIdentifier.setBaseSystemId(null);
          XMLInputSource source = new XMLInputSource(resourceIdentifier);
          source.setByteStream(is);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Found XML schema [" + systemId + "] in classpath: " + resourceLocation);
          }
          return source;
        } catch (IOException e) {
          LOGGER.warn("Error loading XSD [" + systemId + "]: " + resourceLocation, e);
        }
      }
    }
    return null;
  }
}
