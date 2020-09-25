/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.factories.xni;

import org.mule.runtime.dsl.api.factories.xni.XmlSchemaProviderFactory;
import org.mule.runtime.dsl.api.xni.parser.XmlSchemaProvider;
import org.mule.runtime.dsl.internal.xni.parser.DefaultXmlSchemaProvider;

/**
 * Default implementation of {@link XmlSchemaProviderFactory} which will return the {@link DefaultXmlSchemaProvider}
 * instance.
 *
 * @since 1.4.0
 */
public class DefaultXmlSchemaProviderFactory implements XmlSchemaProviderFactory {

  @Override
  public XmlSchemaProvider create() {
    return new DefaultXmlSchemaProvider();
  }
}
