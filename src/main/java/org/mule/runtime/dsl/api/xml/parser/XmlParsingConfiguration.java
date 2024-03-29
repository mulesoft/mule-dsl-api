/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml.parser;

import org.mule.runtime.api.util.ResourceLocator;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.List;
import java.util.function.Supplier;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.EntityResolver;

/**
 * @deprecated since 1.4 use {@code mule-artifact-ast-xml-parser} instead.
 */
@Deprecated
public interface XmlParsingConfiguration {


  /**
   * @return resolver for XML parsing properties.
   */
  ParsingPropertyResolver getParsingPropertyResolver();

  /**
   * @return {@link ConfigResource[]} of config resources.
   */
  ConfigResource[] getArtifactConfigResources();


  ResourceLocator getResourceLocator();

  Supplier<SAXParserFactory> getSaxParserFactory();

  XmlConfigurationDocumentLoader getXmlConfigurationDocumentLoader();

  EntityResolver getEntityResolver();

  List<XmlNamespaceInfoProvider> getXmlNamespaceInfoProvider();

}
