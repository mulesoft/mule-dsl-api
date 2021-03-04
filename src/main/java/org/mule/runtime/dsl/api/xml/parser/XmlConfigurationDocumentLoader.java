/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml.parser;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.dsl.internal.xml.parser.DefaultXmlGathererErrorHandlerFactory;
import org.mule.runtime.dsl.internal.xml.parser.DefaultXmlLoggerErrorHandler;
import org.mule.runtime.dsl.internal.xml.parser.MuleDocumentLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Loads a mule configuration file into a {@link Document} object.
 * <p/>
 * If when loading the configuration one, or more, {@link ErrorHandler#error(SAXParseException)} are call, at the end of
 * {@link #loadDocument(Supplier, EntityResolver, String, InputStream)} will throw an exception containing all the errors.
 *
 * @see {@link #loadDocument(Supplier, EntityResolver, String, InputStream)}
 * @since 4.0
 */
public final class XmlConfigurationDocumentLoader {

  /**
   * Indicates that XSD validation should be used (found no "DOCTYPE" declaration).
   */
  private static final int VALIDATION_XSD = 3;

  /**
   * Indicates that the validation should be disabled.
   */
  private static final int NO_VALIDATION = 0;

  private final XmlGathererErrorHandlerFactory xmlGathererErrorHandlerFactory;
  private final int validationMode;

  /**
   * Creates an {@link XmlConfigurationDocumentLoader} using the default {@link DefaultXmlGathererErrorHandlerFactory} to
   * instantiate {@link XmlGathererErrorHandler}s objects when executing the
   * {@link #loadDocument(Supplier, EntityResolver, String, InputStream)} method.
   * <p/>
   * Using the default constructor implies that all XSD validations will take place and, at the end of the parsing, all the errors
   * will be contained in an {@link MuleRuntimeException} which will be thrown.
   *
   * @return a new instance of {@link XmlConfigurationDocumentLoader}
   */
  public static XmlConfigurationDocumentLoader schemaValidatingDocumentLoader() {
    return new XmlConfigurationDocumentLoader(new DefaultXmlGathererErrorHandlerFactory());
  }

  /**
   * Creates an {@link XmlConfigurationDocumentLoader} using a parametrized {@link XmlGathererErrorHandlerFactory} to instantiate
   * {@link XmlGathererErrorHandler}s objects when executing the
   * {@link #loadDocument(Supplier, EntityResolver, String, InputStream)} method.
   * <p/>
   * Depending on what type of {@link XmlGathererErrorHandler} the factory returns, the
   * {@link #loadDocument(Supplier, EntityResolver, String, InputStream)} method will not thrown any exception if
   * {@link XmlGathererErrorHandler#getErrors()} is an empty list.
   *
   * @param errorHandlerFactory to create {@link XmlGathererErrorHandler} in the
   *                            {@link #loadDocument(Supplier, EntityResolver, String, InputStream)}
   * @return a new instance of {@link XmlConfigurationDocumentLoader}
   */
  public static XmlConfigurationDocumentLoader schemaValidatingDocumentLoader(XmlGathererErrorHandlerFactory errorHandlerFactory) {
    return new XmlConfigurationDocumentLoader(errorHandlerFactory);
  }

  /**
   * Creates an {@link XmlConfigurationDocumentLoader} that will ignore XSD validation when executing the
   * {@link #loadDocument(Supplier, EntityResolver, String, InputStream)} method.
   * <p/>
   *
   * @return a new instance of {@link XmlConfigurationDocumentLoader}
   */
  public static XmlConfigurationDocumentLoader noValidationDocumentLoader() {
    return new XmlConfigurationDocumentLoader(null);
  }

  private XmlConfigurationDocumentLoader(XmlGathererErrorHandlerFactory errorHandlerFactory) {
    this.validationMode = errorHandlerFactory != null ? VALIDATION_XSD : NO_VALIDATION;
    this.xmlGathererErrorHandlerFactory = errorHandlerFactory;
  }

  /**
   * Creates a {@link Document} from an {@link InputStream} with the required configuration of a mule configuration file parsing.
   *
   * @param inputStream the input stream with the XML configuration content.
   * @return a new {@link Document} object with the provided content.
   * @throws MuleRuntimeException if an error occurs in {@link MuleDocumentLoader} factory, or if the current {@code filename}
   *                              contains 1 or more errors.
   * @see {@link DefaultXmlLoggerErrorHandler#getErrors()}
   */
  public Document loadDocument(Supplier<SAXParserFactory> saxParserFactorySupplier, String filename, InputStream inputStream,
                               EntityResolver entityResolver) {
    return loadDocument(saxParserFactorySupplier,
                        entityResolver, filename, inputStream);
  }

  /**
   * Creates a {@link Document} from an {@link InputStream} with the required configuration of a mule configuration file parsing.
   *
   * @param entityResolver resolver for XML schemas.
   * @param filename       name of the file to display a better error messages (if there are any). Non null.
   * @param inputStream    the input stream with the XML configuration content.
   * @return a new {@link Document} object with the provided content.
   * @throws MuleRuntimeException if an error occurs in {@link MuleDocumentLoader} factory, or if the current {@code filename}
   *                              contains 1 or more errors.
   * @see {@link DefaultXmlLoggerErrorHandler#getErrors()}
   */
  public Document loadDocument(Supplier<SAXParserFactory> saxParserFactorySupplier, EntityResolver entityResolver,
                               String filename, InputStream inputStream) {
    return loadDocument(saxParserFactorySupplier, entityResolver, filename, inputStream, null);
  }

  /**
   * Creates a {@link Document} from an {@link InputStream} with the required configuration of a mule configuration file parsing.
   *
   * @param entityResolver resolver for XML schemas.
   * @param filename       name of the file to display a better error messages (if there are any). Non null.
   * @param inputStream    the input stream with the XML configuration content.
   * @param xmlGrammarPool a {@link XMLGrammarPool} pool to be used while file parsing.
   * @return a new {@link Document} object with the provided content.
   * @throws MuleRuntimeException if an error occurs in {@link MuleDocumentLoader} factory, or if the current {@code filename}
   *                              contains 1 or more errors.
   * @see {@link DefaultXmlLoggerErrorHandler#getErrors()}
   */
  public Document loadDocument(Supplier<SAXParserFactory> saxParserFactorySupplier, EntityResolver entityResolver,
                               String filename, InputStream inputStream, XMLGrammarPool xmlGrammarPool) {
    final XmlGathererErrorHandler errorHandler = createXmlGathererErrorHandler();
    Document document;
    try {
      document = new MuleDocumentLoader()
          .loadDocument(saxParserFactorySupplier, new InputSource(inputStream),
                        entityResolver,
                        errorHandler == null ? new DefaultHandler() : errorHandler,
                        validationMode, true, xmlGrammarPool);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(format("Error loading: %s, %s", filename, e.getMessage())), e);
    } finally {
      closeQuietly(inputStream);
    }
    if (validationMode == VALIDATION_XSD) {
      throwExceptionIfErrorsWereFound(errorHandler, filename);
    }
    return document;
  }

  private void closeQuietly(InputStream inputStream) {
    try {
      inputStream.close();
    } catch (IOException e) {
    }
  }

  private void throwExceptionIfErrorsWereFound(XmlGathererErrorHandler errorHandler, String filename) {
    final List<SAXParseException> errors = errorHandler.getErrors();
    if (!errors.isEmpty()) {
      final String subMessage = format(errors.size() == 1 ? "was '%s' error" : "were '%s' errors", errors.size());
      final StringBuilder sb =
          new StringBuilder("There " + subMessage + " while parsing the given file"
              + (filename.isEmpty() ? "." : " '" + filename + "'."));
      sb.append(lineSeparator()).append("Full list:");
      errors.forEach(error -> sb.append(lineSeparator()).append(error));
      sb.append(lineSeparator());
      throw new MuleRuntimeException(createStaticMessage(sb.toString()));
    }
  }

  private XmlGathererErrorHandler createXmlGathererErrorHandler() {
    return validationMode == VALIDATION_XSD ? xmlGathererErrorHandlerFactory.create() : null;
  }
}
