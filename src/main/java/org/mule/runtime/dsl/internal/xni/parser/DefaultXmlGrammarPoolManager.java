/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xni.parser;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_DEPLOYMENT_SCHEMA_CACHE;

import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import org.mule.runtime.dsl.api.factories.xni.XmlEntityResolverFactory;
import org.mule.runtime.dsl.api.factories.xni.XmlGathererErrorHandlerFactory;
import org.mule.runtime.dsl.api.factories.xni.XmlSchemaProviderFactory;
import org.mule.runtime.dsl.api.xni.parser.XmlGathererErrorHandler;
import org.mule.runtime.dsl.api.xni.parser.XmlSchemaProvider;

import java.util.Optional;

/**
 * This class manages {@link XMLGrammarPool} preloaded mule.schemas
 *
 * @since 1.4.0
 */
public class DefaultXmlGrammarPoolManager {

  private static final boolean IS_CACHE_DISABLED = parseBoolean(getProperty(MULE_DISABLE_DEPLOYMENT_SCHEMA_CACHE, "false"));

  private static final Object LOCK = new Object();
  private static volatile Optional<XMLGrammarPool> INSTANCE;

  private DefaultXmlGrammarPoolManager() {
    // Nothing to do
  }

  public static Optional<XMLGrammarPool> getGrammarPool() {
    if (INSTANCE == null) {
      synchronized (LOCK) {
        if (INSTANCE == null) {
          INSTANCE = initialize();
        }
      }
    }
    return INSTANCE;
  }

  private static Optional<XMLGrammarPool> initialize() {
    if (IS_CACHE_DISABLED) {
      return empty();
    } else {
      XmlSchemaProvider schemaProvider = XmlSchemaProviderFactory.getDefault().create();
      XmlGathererErrorHandler errorHandler = XmlGathererErrorHandlerFactory.getDefault().create();
      XMLEntityResolver entityResolver = XmlEntityResolverFactory.getDefault().create();
      return of(XmlGrammarPoolBuilder.builder(schemaProvider, errorHandler, entityResolver).build());
    }
  }
}
