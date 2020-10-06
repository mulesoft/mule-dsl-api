/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xni.parser;

import static org.mule.runtime.dsl.internal.xni.parser.XmlGrammarPoolBuilder.builder;

import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import org.mule.runtime.dsl.api.factories.xni.XmlEntityResolverFactory;
import org.mule.runtime.dsl.api.factories.xni.XmlGathererErrorHandlerFactory;
import org.mule.runtime.dsl.api.factories.xni.XmlSchemaProviderFactory;

/**
 * This class manages {@link XMLGrammarPool} preloaded with mule schemas.
 *
 * @since 1.4.0
 */
public class DefaultXmlGrammarPoolManager {

  private static XMLGrammarPool RUNTIME_GRAMMAR_POOL =
      builder(XmlSchemaProviderFactory.getDefault().create(), XmlGathererErrorHandlerFactory.getDefault().create(),
              XmlEntityResolverFactory.getDefault().create()).build();

  private DefaultXmlGrammarPoolManager() {}

  public static XMLGrammarPool getGrammarPool() {
    return RUNTIME_GRAMMAR_POOL;
  }

}
