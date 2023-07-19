/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xml.parser;

import org.mule.runtime.dsl.api.xml.parser.XmlGathererErrorHandler;
import org.mule.runtime.dsl.api.xml.parser.XmlGathererErrorHandlerFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Default implementation of {@link XmlGathererErrorHandlerFactory} which will return the {@link DefaultXmlLoggerErrorHandler}
 * instance that registers all errors when {@link ErrorHandler#error(SAXParseException)} is called, to then return the complete
 * gathered list of exceptions through {@link XmlGathererErrorHandler#getErrors()} method.
 *
 * @since 4.0
 */
public class DefaultXmlGathererErrorHandlerFactory implements XmlGathererErrorHandlerFactory {

  @Override
  public XmlGathererErrorHandler create() {
    return new DefaultXmlLoggerErrorHandler();
  }
}
