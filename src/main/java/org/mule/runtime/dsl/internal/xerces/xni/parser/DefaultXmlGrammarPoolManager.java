/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_DEPLOYMENT_SCHEMA_CACHE;

import org.mule.apache.xerces.xni.grammars.XMLGrammarPool;
import org.mule.apache.xerces.xni.parser.XMLEntityResolver;
import org.mule.runtime.dsl.api.xerces.xni.factories.XmlEntityResolverFactory;
import org.mule.runtime.dsl.api.xerces.xni.factories.XmlGathererErrorHandlerFactory;
import org.mule.runtime.dsl.api.xerces.xni.factories.XmlSchemaProviderFactory;
import org.mule.runtime.api.util.LazyValue;

import java.util.Optional;

/**
 * This class manages {@link XMLGrammarPool} preloaded mule.schemas
 *
 * @since 1.4.0
 */
public class DefaultXmlGrammarPoolManager {

  private static final boolean IS_CACHE_DISABLED = parseBoolean(getProperty(MULE_DISABLE_DEPLOYMENT_SCHEMA_CACHE, "false"));

  private static final LazyValue<Optional<XMLGrammarPool>> INSTANCE = new LazyValue<>(DefaultXmlGrammarPoolManager::initialize);

  private DefaultXmlGrammarPoolManager() {
    // Nothing to do
  }

  public static Optional<XMLGrammarPool> getGrammarPool() {
    return INSTANCE.get();
  }


  private static Optional<XMLGrammarPool> initialize() {
    if (IS_CACHE_DISABLED) {
      return empty();
    } else {
      final Thread thread = currentThread();
      final ClassLoader currentClassLoader = thread.getContextClassLoader();
      try {
        thread.setContextClassLoader(DefaultXmlGrammarPoolManager.class.getClassLoader());
        return doInitialize();
      } finally {
        thread.setContextClassLoader(currentClassLoader);
      }
    }
  }

  private static Optional<XMLGrammarPool> doInitialize() {
    XmlSchemaProvider schemaProvider = XmlSchemaProviderFactory.getDefault().create();
    XmlGathererErrorHandler errorHandler = XmlGathererErrorHandlerFactory.getDefault().create();
    XMLEntityResolver entityResolver = XmlEntityResolverFactory.getDefault().create();
    return of(XmlGrammarPoolBuilder.builder(schemaProvider, errorHandler, entityResolver).build());
  }

}
