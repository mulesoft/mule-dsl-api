/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml.parser;

import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

/**
 * Provides a factory method for creating {@link XmlApplicationParser} instances.
 *
 * @since 1.8
 */
public class XmlApplicationParserUtils {

  /**
   * Creates a new instance of {@link XmlApplicationParser}.
   */
  public static Optional<ConfigLine> parse(List<XmlNamespaceInfoProvider> namespaceInfoProviders, Element configElement) {
    return new org.mule.runtime.dsl.internal.xml.parser.XmlApplicationParser(namespaceInfoProviders).parse(configElement);
  }
}
