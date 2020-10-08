/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xni.parser;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;
import static org.mule.runtime.dsl.internal.xni.parser.DefaultXmlGrammarPoolManager.getGrammarPool;

import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

import java.util.Optional;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlGrammarPoolManagerTestCase {

  @Test
  public void grammarPoolManagerShouldReturnSingletonInstance() {
    Optional<XMLGrammarPool> grammarPool = getGrammarPool();
    assertThat(grammarPool.isPresent(), is(true));
    assertThat(grammarPool.get(), is(instanceOf(RuntimeXmlGrammarPool.class)));
    Optional<XMLGrammarPool> grammarPool2 = getGrammarPool();
    assertThat(grammarPool2.isPresent(), is(true));
    assertThat(grammarPool.get(), is(sameInstance(grammarPool2.get())));
  }
}
