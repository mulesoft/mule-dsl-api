/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xml.parser.pool;

import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class MuleXmlEntityResolver implements XMLEntityResolver {

  private static final Logger LOGGER = getLogger(MuleXmlEntityResolver.class);

  private static final String CORE_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule.xsd";
  private static final String CORE_CURRENT_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-core.xsd";
  private static final String CORE_DEPRECATED_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-core-deprecated.xsd";
  private static final String COMPATIBILITY_XSD =
      "http://www.mulesoft.org/schema/mule/compatibility/current/mule-compatibility.xsd";

  private final Map<String, String> muleSchemaMappings;

  public MuleXmlEntityResolver(Map<String, String> muleSchemaMappings) {
    this.muleSchemaMappings = muleSchemaMappings;
  }

  @Override
  public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws XNIException, IOException {
    String systemId = overrideSystemIdForCompatibility(resourceIdentifier.getLiteralSystemId());
    return getInputSource(systemId);
  }

  private String overrideSystemIdForCompatibility(String systemId) {
    if (CORE_XSD.equals(systemId)) {
      Boolean useDeprecated = canResolveEntity(CORE_DEPRECATED_XSD);
      Boolean usingCompatibility = canResolveEntity(COMPATIBILITY_XSD);
      if (useDeprecated && usingCompatibility) {
        return CORE_DEPRECATED_XSD;
      } else {
        return CORE_CURRENT_XSD;
      }
    }

    return systemId;
  }

  protected boolean canResolveEntity(String systemId) {
    return muleSchemaMappings.containsKey(systemId);
  }

  private XMLInputSource getInputSource(String systemId) {
    String resourceLocation = muleSchemaMappings.get(systemId);
    if (resourceLocation != null) {
      InputStream is = MuleXmlEntityResolver.class.getClassLoader().getResourceAsStream(resourceLocation);
      if (is == null) {
        LOGGER.debug("Couldn't find XML schema [" + systemId + "]: " + resourceLocation);
        return null;
      }
      XMLInputSource source = new XMLInputSource(null, systemId, null);
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
