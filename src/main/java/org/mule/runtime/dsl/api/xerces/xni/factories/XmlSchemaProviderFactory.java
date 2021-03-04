/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
