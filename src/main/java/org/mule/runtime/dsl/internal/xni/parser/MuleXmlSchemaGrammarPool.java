/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xni.parser;

import static com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription.XML_SCHEMA;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.parsers.XMLGrammarPreparser;
import com.sun.org.apache.xerces.internal.util.XMLGrammarPoolImpl;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.mule.runtime.api.util.LazyValue;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * A read-only {@link XMLGrammarPool} preloaded with mule schemas
 *
 * @since 1.4.0
 */
public class MuleXmlSchemaGrammarPool implements XMLGrammarPool {

  private static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
  private static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

  private static final Logger LOGGER = getLogger(MuleXmlSchemaGrammarPool.class);
  private static MuleXmlSchemaGrammarPool INSTANCE = new MuleXmlSchemaGrammarPool();

  private final XmlSchemaProvider schemaProvider;
  private final XmlGathererErrorHandler errorHandler;
  private final XMLEntityResolver entityResolver;
  private final LazyValue<XMLGrammarPool> core;

  public static MuleXmlSchemaGrammarPool getInstance() {
    return INSTANCE;
  }

  private MuleXmlSchemaGrammarPool() {
    this.schemaProvider = new XmlSchemaProvider();
    this.errorHandler = new XmlGathererErrorHandler();
    this.entityResolver = new XmlEntityResolver();
    this.core = new LazyValue<>(initSafeGrammarPool());
  }

  private XMLGrammarPool initSafeGrammarPool() {
    XMLGrammarPool pool;
    try {
      pool = initializeCoreGrammarPool();
    } catch (IOException e) {
      LOGGER.warn("Unable to create grammar pool. Using empty XMLGrammarPool", e);
      pool = createEmptyXMLGrammarPool();
    }
    return pool;
  }

  private XMLGrammarPool initializeCoreGrammarPool() throws IOException {
    XMLGrammarPool core = new XMLGrammarPoolImpl();

    // create grammar preparser
    XMLGrammarPreparser preparser = new XMLGrammarPreparser();
    preparser.setGrammarPool(core);

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
      Grammar[] grammars = core.retrieveInitialGrammarSet(XML_SCHEMA);
      LOGGER.info(format("Loaded %s grammars", grammars.length));
    } else {
      final String subMessage =
          format(errorHandler.getErrors().size() == 1 ? "was '%s' error" : "were '%s' errors", errorHandler.getErrors().size());
      final StringBuilder sb =
          new StringBuilder("There " + subMessage + " while creating XMLSchemaGrammarPool. Using empty XMLGrammarPool\"");
      sb.append(lineSeparator()).append("Full list:");
      errorHandler.getErrors().forEach(error -> sb.append(lineSeparator()).append(error));
      sb.append(lineSeparator());
      LOGGER.warn(sb.toString());

      core = createEmptyXMLGrammarPool();
    }
    core.lockPool();
    return core;
  }

  private XMLGrammarPool createEmptyXMLGrammarPool() {
    XMLGrammarPool pool = new XMLGrammarPoolImpl(1);
    pool.lockPool();
    return pool;
  }

  private XMLGrammarPool core() {
    return core.get();
  }

  @Override
  public Grammar[] retrieveInitialGrammarSet(String s) {
    return core().retrieveInitialGrammarSet(s);
  }

  @Override
  public Grammar retrieveGrammar(XMLGrammarDescription xmlGrammarDescription) {
    return core().retrieveGrammar(xmlGrammarDescription);
  }

  @Override
  public void cacheGrammars(String s, Grammar[] grammars) {
    // Nothing to do
  }

  @Override
  public void lockPool() {
    // Nothing to do
  }

  @Override
  public void unlockPool() {
    // Nothing to do
  }

  @Override
  public void clear() {
    // Nothing to do
  }
}
