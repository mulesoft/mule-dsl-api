/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
