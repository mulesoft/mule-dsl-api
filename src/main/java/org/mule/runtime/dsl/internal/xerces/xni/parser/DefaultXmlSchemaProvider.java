/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import static org.mule.runtime.api.util.IOUtils.getInputStreamWithCacheControl;
import static org.mule.runtime.api.util.classloader.MuleImplementationLoaderUtils.getMuleImplementationsLoader;
import static org.mule.runtime.dsl.internal.util.SchemaMappingsUtils.getMuleSchemasMappings;

import static org.slf4j.LoggerFactory.getLogger;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import org.mule.apache.xerces.xni.parser.XMLInputSource;
import org.mule.apache.xerces.util.XMLResourceIdentifierImpl;
import org.mule.apache.xerces.xni.XMLResourceIdentifier;

import org.slf4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link XmlSchemaProvider}
 *
 * @since 1.4.0
 */
public class DefaultXmlSchemaProvider implements XmlSchemaProvider {

  private static final Logger LOGGER = getLogger(XmlSchemaProvider.class);

  private final Map<String, String> schemas;

  public DefaultXmlSchemaProvider() {
    this.schemas = getMuleSchemasMappings();
  }

  @Override
  public List<XMLInputSource> getSchemas() {
    return schemas.entrySet().stream()
        .map(entry -> {
          String systemId = entry.getKey();
          String resourceLocation = entry.getValue();
          XMLInputSource xis = null;
          URL resource = getMuleImplementationsLoader().getResource(resourceLocation);
          if (resource == null) {
            LOGGER.debug("Couldn't find schema [{}]: {}", systemId, resourceLocation);
          } else {
            try {
              InputStream is = getInputStreamWithCacheControl(resource);
              XMLResourceIdentifier resourceIdentifier = new XMLResourceIdentifierImpl();
              resourceIdentifier.setPublicId(null);
              resourceIdentifier.setLiteralSystemId(systemId);
              resourceIdentifier.setBaseSystemId(null);
              xis = new XMLInputSource(resourceIdentifier);
              xis.setByteStream(is);
            } catch (IOException e) {
              LOGGER.warn("Error loading XSD [{}]: {}", systemId, resourceLocation, e);
            }
          }
          return ofNullable(xis);
        })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }
}
