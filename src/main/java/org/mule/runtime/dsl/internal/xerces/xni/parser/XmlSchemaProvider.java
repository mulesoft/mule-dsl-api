/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import org.mule.apache.xerces.xni.parser.XMLInputSource;
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
