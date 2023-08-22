/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml.parser;

import org.mule.api.annotation.NoImplement;
import org.mule.api.annotation.NoInstantiate;

/**
 * A configuration line provider allows to have a parent child relationship between {@code ConfigLine} while keeping the object
 * immutable.
 *
 * @since 4.0
 * @deprecated From 4.4 onwards, use the {@code mule-artifact-ast-xml-parser} module.
 */
@FunctionalInterface
@NoImplement
@NoInstantiate
@Deprecated
public interface ConfigLineProvider {

  /**
   * @return a {@code ConfigLine}.
   */
  ConfigLine getConfigLine();

}
