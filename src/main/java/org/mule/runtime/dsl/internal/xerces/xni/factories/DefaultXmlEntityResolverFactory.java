/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
