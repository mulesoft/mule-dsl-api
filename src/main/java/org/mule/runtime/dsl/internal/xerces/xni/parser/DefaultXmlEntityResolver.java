/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import static org.mule.runtime.api.util.IOUtils.getInputStreamWithCacheControl;
import static org.mule.runtime.api.util.classloader.MuleImplementationLoaderUtils.getMuleImplementationsLoader;
import static org.mule.runtime.dsl.internal.util.SchemaMappingsUtils.getFor;
import static org.mule.runtime.dsl.internal.util.SchemaMappingsUtils.resolveSystemId;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.apache.xerces.util.XMLResourceIdentifierImpl;
import org.mule.apache.xerces.xni.XMLResourceIdentifier;
import org.mule.apache.xerces.xni.XNIException;
import org.mule.apache.xerces.xni.parser.XMLEntityResolver;
import org.mule.apache.xerces.xni.parser.XMLInputSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Custom {@link XMLEntityResolver} that resolve entities over mule schemas.
 *
 * @since 1.4.0
 */
public class DefaultXmlEntityResolver implements XMLEntityResolver {

  private static final Logger LOGGER = getLogger(DefaultXmlEntityResolver.class);

  private final ClassLoader muleImplementationsLoader;
  private final Map<String, String> schemas;

  public DefaultXmlEntityResolver() {
    this.muleImplementationsLoader = getMuleImplementationsLoader();
    this.schemas = getFor(muleImplementationsLoader).getMuleSchemasMappings();
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
      URL resource = muleImplementationsLoader.getResource(resourceLocation);
      if (resource == null) {
        LOGGER.debug("Couldn't find schema [" + systemId + "]: " + resourceLocation);
      } else {
        try {
          InputStream is = getInputStreamWithCacheControl(resource);
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
