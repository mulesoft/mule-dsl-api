/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.factories.xni;

import org.mule.runtime.dsl.api.factories.xni.XmlGathererErrorHandlerFactory;
import org.mule.runtime.dsl.api.xni.parser.XmlGathererErrorHandler;
import org.mule.runtime.dsl.internal.xni.parser.DefaultXmlGathererErrorHandler;

/**
 * Default implementation of {@link XmlGathererErrorHandlerFactory} which will return the {@link DefaultXmlGathererErrorHandler}
 * instance.
 *
 * @since 1.4.0
 */
public class DefaultXmlGathererErrorHandlerFactory implements XmlGathererErrorHandlerFactory {

  @Override
  public XmlGathererErrorHandler create() {
    return new DefaultXmlGathererErrorHandler();
  }
}
