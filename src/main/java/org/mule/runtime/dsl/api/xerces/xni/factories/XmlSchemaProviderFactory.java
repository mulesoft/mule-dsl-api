/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.xerces.xni.factories;

import static org.mule.runtime.dsl.internal.xerces.xni.factories.DefaultXmlSchemaProviderFactory.getInstance;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.dsl.internal.xerces.xni.parser.XmlSchemaProvider;

/**
 * Factory object to create instances of {@link XmlSchemaProvider} that will be used to create
 * {@link com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool}.
 *
 * @since 1.4.0
 */
@NoImplement
public interface XmlSchemaProviderFactory {

  /**
   * @return an {@link org.mule.runtime.dsl.internal.xerces.xni.factories.DefaultXmlSchemaProviderFactory} instance
   */
  public static XmlSchemaProviderFactory getDefault() {
    return getInstance();
  }

  /**
   * @return Creates an {@link XmlSchemaProvider} to be used to create
   *         {@link com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool}.
   */
  public XmlSchemaProvider create();
}
