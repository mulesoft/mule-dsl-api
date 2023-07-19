/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.xerces.xni.factories;

import static org.mule.runtime.dsl.internal.xerces.xni.factories.DefaultXmlGathererErrorHandlerFactory.getInstance;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.dsl.internal.xerces.xni.parser.XmlGathererErrorHandler;

/**
 * Factory object to create instances of {@link XmlGathererErrorHandler} that will be used in the reading of XML files.
 *
 * @since 1.4.0
 */
@NoImplement
public interface XmlGathererErrorHandlerFactory {

  /**
   * @return an {@link org.mule.runtime.dsl.internal.xerces.xni.factories.DefaultXmlGathererErrorHandlerFactory} instance
   */
  public static XmlGathererErrorHandlerFactory getDefault() {
    return getInstance();
  }

  /**
   * @return Creates an {@link XmlGathererErrorHandler} to be used in the reading of XML files.
   */
  public XmlGathererErrorHandler create();
}
