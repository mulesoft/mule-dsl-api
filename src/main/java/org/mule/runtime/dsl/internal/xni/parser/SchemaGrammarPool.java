/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xni.parser;

import static com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription.XML_SCHEMA;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.parsers.XMLGrammarPreparser;
import com.sun.org.apache.xerces.internal.util.EntityResolverWrapper;
import com.sun.org.apache.xerces.internal.util.XMLGrammarPoolImpl;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.mule.runtime.dsl.api.xni.parser.SchemaProvider;
import org.mule.runtime.dsl.api.xni.parser.XmlGathererErrorHandler;
import org.slf4j.Logger;
import org.xml.sax.EntityResolver;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SchemaGrammarPool implements XMLGrammarPool {

  private static final Logger LOGGER = getLogger(SchemaGrammarPool.class);

  private static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
  private static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

  private final static Object LOCK = new Object();
  private static SchemaGrammarPool INSTANCE;

  private final AtomicBoolean initialized = new AtomicBoolean(false);

  private final SchemaProvider schemaProvider;
  private final XmlGathererErrorHandler errorHandler;
  private EntityResolver entityResolver;
  private XMLGrammarPool core;

  public static SchemaGrammarPool getInstance() {
    if (INSTANCE == null) {
      synchronized (LOCK) {
        if (INSTANCE == null) {
          INSTANCE = new SchemaGrammarPool();
        }
      }
    }

    return INSTANCE;
  }

  private SchemaGrammarPool() {
    this.schemaProvider = new DefaultSchemaProvider();
    this.errorHandler = new DefaultXmlGathererErrorHandler();
  }

  public void init(EntityResolver entityResolver) {
    if (initialized.get()) {
      return;
    }

    initialized.set(true);


    this.entityResolver = entityResolver;
    this.core = createXMLGrammarPool();
  }

  private XMLGrammarPool createXMLGrammarPool() {
    XMLGrammarPool core = new XMLGrammarPoolImpl();

    // create grammar preparser
    XMLGrammarPreparser preparser = new XMLGrammarPreparser();
    preparser.setGrammarPool(core);

    preparser.registerPreparser(XML_SCHEMA, null);

    // set properties
    preparser.setFeature(NAMESPACES_FEATURE_ID, true);
    preparser.setFeature(VALIDATION_FEATURE_ID, true);

    preparser.setErrorHandler(errorHandler);

    if (entityResolver != null) {
      preparser.setEntityResolver(new EntityResolverWrapper(entityResolver));
    }

    // parse grammars
    schemaProvider.getSchemas().forEach(is -> preparseGrammar(preparser, is));

    // TODO: ML Verify errors on schemas

    core.lockPool();

    Grammar[] grammars = core.retrieveInitialGrammarSet(XML_SCHEMA);
    LOGGER.info(format("Loaded %s grammars", grammars.length));
    return core;
  }

  private void preparseGrammar(XMLGrammarPreparser preparser, XMLInputSource is) {
    try {
      preparser.preparseGrammar(XML_SCHEMA, is);
    } catch (IOException e) {
      LOGGER.warn(format("Unable to load schema [%s]", is), e);
    }
  }

  @Override
  public Grammar[] retrieveInitialGrammarSet(String s) {
    return core.retrieveInitialGrammarSet(s);
  }

  @Override
  public void cacheGrammars(String s, Grammar[] grammars) {
    // Nothing to do
  }

  @Override
  public Grammar retrieveGrammar(XMLGrammarDescription xmlGrammarDescription) {
    return core.retrieveGrammar(xmlGrammarDescription);
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
