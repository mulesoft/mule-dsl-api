/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
