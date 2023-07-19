/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xerces.xni.factories;

import org.mule.runtime.dsl.api.xerces.xni.factories.XmlSchemaProviderFactory;
import org.mule.runtime.dsl.internal.xerces.xni.parser.XmlSchemaProvider;
import org.mule.runtime.dsl.internal.xerces.xni.parser.DefaultXmlSchemaProvider;

/**
 * Default implementation of {@link XmlSchemaProviderFactory} which will return the {@link DefaultXmlSchemaProvider} instance.
 *
 * @since 1.4.0
 */
public class DefaultXmlSchemaProviderFactory implements XmlSchemaProviderFactory {

  private static final DefaultXmlSchemaProviderFactory INSTANCE = new DefaultXmlSchemaProviderFactory();

  public static DefaultXmlSchemaProviderFactory getInstance() {
    return INSTANCE;
  }

  private DefaultXmlSchemaProviderFactory() {}

  @Override
  public XmlSchemaProvider create() {
    return new DefaultXmlSchemaProvider();
  }
}
