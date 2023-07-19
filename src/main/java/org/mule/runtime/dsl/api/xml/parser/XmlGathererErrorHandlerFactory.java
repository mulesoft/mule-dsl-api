/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.xml.parser;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.dsl.internal.xml.parser.MuleDocumentLoader;

import java.util.function.Supplier;

import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

/**
 * Factory object to create instances of {@link XmlGathererErrorHandler} that will be used in the reading of XML files.
 *
 * @since 4.0
 */
@NoImplement
public interface XmlGathererErrorHandlerFactory {

  /**
   * @return Creates an {@link XmlGathererErrorHandler} to be used when executing
   *         {@link MuleDocumentLoader#loadDocument(Supplier, InputSource, EntityResolver, ErrorHandler, int, boolean)}
   */
  XmlGathererErrorHandler create();
}
