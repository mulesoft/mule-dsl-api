/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xerces.xni.factories;

import org.mule.runtime.dsl.api.xerces.xni.factories.XmlGathererErrorHandlerFactory;
import org.mule.runtime.dsl.internal.xerces.xni.parser.XmlGathererErrorHandler;
import org.mule.runtime.dsl.internal.xerces.xni.parser.DefaultXmlGathererErrorHandler;

/**
 * Default implementation of {@link XmlGathererErrorHandlerFactory} which will return the {@link DefaultXmlGathererErrorHandler}
 * instance.
 *
 * @since 1.4.0
 */
public class DefaultXmlGathererErrorHandlerFactory implements XmlGathererErrorHandlerFactory {

  private static final DefaultXmlGathererErrorHandlerFactory INSTANCE = new DefaultXmlGathererErrorHandlerFactory();

  public static DefaultXmlGathererErrorHandlerFactory getInstance() {
    return INSTANCE;
  }

  private DefaultXmlGathererErrorHandlerFactory() {}

  @Override
  public XmlGathererErrorHandler create() {
    return new DefaultXmlGathererErrorHandler();
  }
}
