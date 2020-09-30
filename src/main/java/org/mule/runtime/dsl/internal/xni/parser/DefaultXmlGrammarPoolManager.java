/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xni.parser;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.dsl.internal.xni.parser.XmlGrammarPoolBuilder.builder;
import static org.mule.runtime.dsl.internal.util.SchemasConstants.COMPATIBILITY_XSD;
import static org.mule.runtime.dsl.internal.util.SchemasConstants.CORE_DEPRECATED_XSD;
import static org.mule.runtime.dsl.internal.util.SchemaMappingsUtils.getSchemaMappings;

import com.sun.org.apache.xerces.internal.util.XMLGrammarPoolImpl;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.dsl.api.factories.xni.XmlEntityResolverFactory;
import org.mule.runtime.dsl.api.factories.xni.XmlGathererErrorHandlerFactory;
import org.mule.runtime.dsl.api.factories.xni.XmlSchemaProviderFactory;
import org.mule.runtime.dsl.internal.util.SchemaMappingsUtils;

import java.net.URL;
import java.util.Map;

/**
 * This class manages {@link XMLGrammarPool} preloaded with mule schemas.
 *
 * @since 1.4.0
 */
public class DefaultXmlGrammarPoolManager {

  private static XMLGrammarPool RUNTIME_GRAMMAR_POOL =
      builder(XmlSchemaProviderFactory.getDefault().create(), XmlGathererErrorHandlerFactory.getDefault().create(),
              XmlEntityResolverFactory.getDefault().create()).build();

  private static XMLGrammarPool EMPTY_GRAMMAR_POOL = new RuntimeXmlGrammarPool(new LazyValue<>(() -> {
    XMLGrammarPool pool = new XMLGrammarPoolImpl(1);
    pool.lockPool();
    return pool;
  }));

  private DefaultXmlGrammarPoolManager() {}

  public static XMLGrammarPool getGrammarPool() {
    ClassLoader contextClassLoader = currentThread().getContextClassLoader();
    if (usingCompatibility(contextClassLoader)) {
      return EMPTY_GRAMMAR_POOL;
    }
    return RUNTIME_GRAMMAR_POOL;
  }

  private static boolean usingCompatibility(ClassLoader contextClassLoader) {
    Map<String, String> schemas =
        getSchemaMappings(SchemaMappingsUtils.CUSTOM_SCHEMA_MAPPINGS_LOCATION, () -> contextClassLoader);
    return containsSchemas(schemas, CORE_DEPRECATED_XSD, contextClassLoader)
        && containsSchemas(schemas, COMPATIBILITY_XSD, contextClassLoader);
  }

  private static boolean containsSchemas(Map<String, String> schemas, String schema, ClassLoader classLoader) {
    String resourceLocation = schemas.get(schema);
    if (resourceLocation != null) {
      URL deprecatedXsd = classLoader.getResource(resourceLocation);
      return deprecatedXsd != null;
    }
    return false;
  }

}
