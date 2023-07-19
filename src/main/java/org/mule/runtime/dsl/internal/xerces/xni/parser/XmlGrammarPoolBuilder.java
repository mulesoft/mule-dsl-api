/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import org.mule.apache.xerces.xni.grammars.XMLGrammarPool;
import org.mule.apache.xerces.xni.parser.XMLEntityResolver;
import org.mule.apache.xerces.xni.parser.XMLInputSource;

/**
 * Provides a way of creating {@link XMLGrammarPool} instances.
 *
 * @since 1.4.0
 */
public interface XmlGrammarPoolBuilder {

  /**
   * Instantiates a new raw builder, whose components will be declared in the provided {@code extensionModels}.
   *
   * @param schemaProvider provides {@link XMLInputSource} schemas to be loaded.
   * @param errorHandler   a {@link XmlGathererErrorHandler} which gathers as many errors as possible.
   * @param entityResolver a {@link XMLEntityResolver} that resolve entities over mule schemas.
   * @return the newly created builder
   */
  public static XmlGrammarPoolBuilder builder(XmlSchemaProvider schemaProvider, XmlGathererErrorHandler errorHandler,
                                              XMLEntityResolver entityResolver) {
    return new DefaultXmlGrammarPoolBuilder(schemaProvider, errorHandler, entityResolver);
  }

  /**
   * Builds the target {@link XMLGrammarPool}
   *
   * @return the target {@link XMLGrammarPool}
   */
  XMLGrammarPool build();


}
