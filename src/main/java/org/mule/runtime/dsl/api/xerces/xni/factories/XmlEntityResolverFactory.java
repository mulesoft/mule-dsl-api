/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xerces.xni.factories;

import static org.mule.runtime.dsl.internal.xerces.xni.factories.DefaultXmlEntityResolverFactory.getInstance;

import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import org.mule.api.annotation.NoImplement;

/**
 * Factory object to create instances of {@link XMLEntityResolver} that will be used in the reading of XML files.
 *
 * @since 1.4.0
 */
@NoImplement
public interface XmlEntityResolverFactory {

  /**
   * @return an {@link org.mule.runtime.dsl.internal.xerces.xni.factories.DefaultXmlEntityResolverFactory} instance
   */
  public static XmlEntityResolverFactory getDefault() {
    return getInstance();
  }

  /**
   * @return Creates an {@link XMLEntityResolver} to be used in the reading of XML files.
   */
  public XMLEntityResolver create();
}
