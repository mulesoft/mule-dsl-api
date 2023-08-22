/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import org.mule.apache.xerces.xni.grammars.Grammar;
import org.mule.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.mule.apache.xerces.xni.grammars.XMLGrammarPool;

/**
 * A read-only {@link XMLGrammarPool} preloaded with mule schemas
 *
 * @since 1.4.0
 */
public class ReadOnlyXmlGrammarPool implements XMLGrammarPool {

  private final XMLGrammarPool core;

  public ReadOnlyXmlGrammarPool(XMLGrammarPool core) {
    this.core = core;
  }

  @Override
  public Grammar[] retrieveInitialGrammarSet(String s) {
    return core.retrieveInitialGrammarSet(s);
  }

  @Override
  public Grammar retrieveGrammar(XMLGrammarDescription xmlGrammarDescription) {
    return core.retrieveGrammar(xmlGrammarDescription);
  }

  @Override
  public void cacheGrammars(String s, Grammar[] grammars) {
    // Nothing to do
  }

  @Override
  public void lockPool() {
    // Nothing to do
  }

  @Override
  public void unlockPool() {
    // Nothing to do
  }

  @Override
  public void clear() {
    // Nothing to do
  }
}
