/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xerces.xni.factories;

import org.mule.apache.xerces.xni.parser.XMLEntityResolver;
import org.mule.runtime.dsl.api.xerces.xni.factories.XmlEntityResolverFactory;
import org.mule.runtime.dsl.internal.xerces.xni.parser.DefaultXmlEntityResolver;

/**
 * Default implementation of {@link XmlEntityResolverFactory} which will return the {@link DefaultXmlEntityResolver} instance.
 *
 * @since 1.4.0
 */
public class DefaultXmlEntityResolverFactory implements XmlEntityResolverFactory {

  private final static DefaultXmlEntityResolverFactory INSTANCE = new DefaultXmlEntityResolverFactory();

  public static DefaultXmlEntityResolverFactory getInstance() {
    return INSTANCE;
  }

  private DefaultXmlEntityResolverFactory() {}

  @Override
  public XMLEntityResolver create() {
    return new DefaultXmlEntityResolver();
  }
}
