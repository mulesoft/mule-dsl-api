/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.xerces.xni.factories;

import static org.mule.runtime.dsl.internal.xerces.xni.factories.DefaultXmlEntityResolverFactory.getInstance;

import org.mule.apache.xerces.xni.parser.XMLEntityResolver;
import org.mule.api.annotation.NoImplement;

/**
 * Factory object to create instances of {@link XMLEntityResolver} that will be used in the reading of XML files.
 *
 * @since 1.4.0
 */
@NoImplement
public interface XmlEntityResolverFactory {

  /**
   * @return an {@link org.mule.runtime.dsl.internal.xerces.xni.factories.DefaultXmlEntityResolverFactory} instance
   */
  public static XmlEntityResolverFactory getDefault() {
    return getInstance();
  }

  /**
   * @return Creates an {@link XMLEntityResolver} to be used in the reading of XML files.
   */
  public XMLEntityResolver create();
}
