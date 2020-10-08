/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xni.parser;

import static com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription.XML_DTD;
import static com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription.XML_SCHEMA;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;

import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class ReadOnlyXmlGrammarPoolTestCase {

  private XMLGrammarPool core;
  private ReadOnlyXmlGrammarPool readOnlyXmlGrammarPool;

  @Before
  public void setup() {
    core = mock(XMLGrammarPool.class);
    when(core.retrieveInitialGrammarSet(eq(XML_SCHEMA))).thenReturn(new Grammar[] {mock(Grammar.class)});
    when(core.retrieveInitialGrammarSet(eq(XML_DTD))).thenReturn(new Grammar[0]);

    readOnlyXmlGrammarPool = new ReadOnlyXmlGrammarPool(core);
  }

  @Test
  public void readOnlyXmlGrammarPoolShouldContainsOnlyPreloadGrammars() {
    Grammar[] grammars = new Grammar[1];
    grammars[0] = mock(Grammar.class);
    readOnlyXmlGrammarPool.cacheGrammars(XML_SCHEMA, grammars);
    readOnlyXmlGrammarPool.cacheGrammars(XML_DTD, grammars);
    verify(core, never()).cacheGrammars(anyString(), any());

    grammars = readOnlyXmlGrammarPool.retrieveInitialGrammarSet(XML_SCHEMA);
    assertThat(grammars.length, is(1));
    verify(core).retrieveInitialGrammarSet(XML_SCHEMA);

    grammars = readOnlyXmlGrammarPool.retrieveInitialGrammarSet(XML_DTD);
    assertThat(grammars.length, is(0));
    verify(core).retrieveInitialGrammarSet(XML_DTD);
  }

  @Test
  public void readOnlyXmlGrammarPoolUseCorePoolToRetrieveGrammars() {
    XMLGrammarDescription description = mock(XMLGrammarDescription.class);
    readOnlyXmlGrammarPool.retrieveGrammar(description);
    verify(core).retrieveGrammar(description);
  }

  @Test
  public void readOnlyXmlGrammarPoolNotAllowLockOrUnlockPool() {
    readOnlyXmlGrammarPool.lockPool();
    verify(core, never()).lockPool();

    readOnlyXmlGrammarPool.unlockPool();
    verify(core, never()).unlockPool();
  }

  @Test
  public void readOnlyXmlGrammarPoolNotAllowClearPool() {
    readOnlyXmlGrammarPool.clear();
    verify(core, never()).clear();
  }
}
