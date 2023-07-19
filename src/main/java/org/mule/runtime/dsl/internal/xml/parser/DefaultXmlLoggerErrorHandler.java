/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xml.parser;

import static java.lang.String.format;
import org.mule.runtime.dsl.api.xml.parser.XmlGathererErrorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Default implementation of {@link XmlGathererErrorHandler} which collects all errors, and on a fatal exception will propagate an
 * exception.
 * <p/>
 * If logging is enabled, it will also log all warnings, errors and fatal when encountered.
 * <p/>
 * Instances of this class are not reusable among several readings of
 * {@link MuleDocumentLoader#loadDocument(Supplier, InputSource, EntityResolver, ErrorHandler, int, boolean)} as it holds state of
 * the exceptions that were gathered.
 *
 * @since 4.0
 */
public class DefaultXmlLoggerErrorHandler implements XmlGathererErrorHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultXmlLoggerErrorHandler.class);

  private List<SAXParseException> errors = new ArrayList<>();

  @Override
  public void warning(SAXParseException e) throws SAXException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found a waring exception parsing document, message '%s'", e.toString()), e);
    }
  }

  @Override
  public void fatalError(SAXParseException e) throws SAXException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found a fatal error exception parsing document, message '%s'", e.toString()), e);
    }
    throw e;
  }

  @Override
  public void error(SAXParseException e) throws SAXException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found error exception parsing document, message '%s'", e.toString()), e);
    }
    errors.add(e);
  }

  @Override
  public List<SAXParseException> getErrors() {
    return errors;
  }
}
