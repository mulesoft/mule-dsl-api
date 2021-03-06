/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import static com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription.XML_SCHEMA;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.parsers.XMLGrammarPreparser;
import com.sun.org.apache.xerces.internal.util.XMLGrammarPoolImpl;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.slf4j.Logger;

/**
 * Default implementation of {@link XmlGrammarPoolBuilder} provides a way of creating {@link ReadOnlyXmlGrammarPool} instances.
 *
 * @since 1.4.0
 */
public class DefaultXmlGrammarPoolBuilder implements XmlGrammarPoolBuilder {

  private static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
  private static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

  private static Logger LOGGER = getLogger(DefaultXmlGrammarPoolBuilder.class);

  private final XmlSchemaProvider schemaProvider;
  private final XmlGathererErrorHandler errorHandler;
  private final XMLEntityResolver entityResolver;

  public DefaultXmlGrammarPoolBuilder(XmlSchemaProvider schemaProvider, XmlGathererErrorHandler errorHandler,
                                      XMLEntityResolver entityResolver) {
    this.schemaProvider = schemaProvider;
    this.errorHandler = errorHandler;
    this.entityResolver = entityResolver;
  }

  @Override
  public XMLGrammarPool build() {
    return new ReadOnlyXmlGrammarPool(buildCoreGrammarPool());
  }

  private XMLGrammarPool buildCoreGrammarPool() {
    XMLGrammarPool pool;
    try {
      pool = new XMLGrammarPoolImpl();
      // create grammar preparser
      XMLGrammarPreparser preparser = new XMLGrammarPreparser();
      preparser.setGrammarPool(pool);

      preparser.registerPreparser(XML_SCHEMA, null);

      // set properties
      preparser.setFeature(NAMESPACES_FEATURE_ID, true);
      preparser.setFeature(VALIDATION_FEATURE_ID, true);

      preparser.setErrorHandler(errorHandler);
      preparser.setEntityResolver(entityResolver);

      // parse grammars
      for (XMLInputSource is : schemaProvider.getSchemas()) {
        preparser.preparseGrammar(XML_SCHEMA, is);
      }

      if (errorHandler.getErrors().isEmpty()) {
        Grammar[] grammars = pool.retrieveInitialGrammarSet(XML_SCHEMA);
        LOGGER.debug(format("Loaded %s grammars", grammars.length));
      } else {
        final String subMessage =
            format(errorHandler.getErrors().size() == 1 ? "was '%s' error" : "were '%s' errors", errorHandler.getErrors().size());
        final StringBuilder sb =
            new StringBuilder("There " + subMessage + " while creating XMLSchemaGrammarPool. Using empty XMLGrammarPool");
        sb.append(lineSeparator()).append("Full list:");
        errorHandler.getErrors().forEach(error -> sb.append(lineSeparator()).append(error));
        sb.append(lineSeparator());
        LOGGER.warn(sb.toString());

        pool = buildEmptyXMLGrammarPool();
      }
      pool.lockPool();

    } catch (Throwable e) {
      LOGGER.warn("Unable to create grammar pool. Using empty XMLGrammarPool", e);
      pool = buildEmptyXMLGrammarPool();
    }
    return pool;
  }

  /**
   *
   * @return an empty grammar pool implementation
   */
  public static XMLGrammarPool buildEmptyXMLGrammarPool() {
    // By default XMLGrammarPoolImpl creates an internal array with initial capacity = 11.
    // Setting initial pool capacity = 1 minimize unnecessary array (using capacity = 0 can cause an ArithmeticException (/ by
    // zero) while trying to retrieveGrammar a grammar)
    XMLGrammarPool pool = new XMLGrammarPoolImpl(1);
    pool.lockPool();
    return pool;
  }
}
