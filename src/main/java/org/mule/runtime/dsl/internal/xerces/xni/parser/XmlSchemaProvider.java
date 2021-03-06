/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.mule.api.annotation.NoImplement;

import java.util.List;

/**
 * Provide {@link XMLInputSource} schemas to be loaded.
 *
 * @see 1.4.0
 */
@NoImplement
public interface XmlSchemaProvider {

  /**
   * @return a {@link List} of mule schemas
   */
  List<XMLInputSource> getSchemas();
}
