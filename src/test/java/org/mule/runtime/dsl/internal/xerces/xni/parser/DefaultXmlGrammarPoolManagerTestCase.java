/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;
import static org.mule.runtime.dsl.internal.xerces.xni.parser.DefaultXmlGrammarPoolManager.getGrammarPool;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.apache.xerces.xni.grammars.XMLGrammarPool;

import java.util.Optional;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlGrammarPoolManagerTestCase {

  @Test
  public void grammarPoolManagerShouldReturnSingletonInstance() {
    Optional<XMLGrammarPool> grammarPool = getGrammarPool();
    assertThat(grammarPool.isPresent(), is(true));
    assertThat(grammarPool.get(), is(instanceOf(ReadOnlyXmlGrammarPool.class)));
    Optional<XMLGrammarPool> grammarPool2 = getGrammarPool();
    assertThat(grammarPool2.isPresent(), is(true));
    assertThat(grammarPool.get(), is(sameInstance(grammarPool2.get())));
  }
}
