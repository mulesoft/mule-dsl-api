/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;

import org.mule.apache.xerces.xni.parser.XMLParseException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlGathererErrorHandlerTestCase {

  @Rule
  public ExpectedException thrown = none();

  private XmlGathererErrorHandler errorHandler;

  @Before
  public void setup() {
    errorHandler = new DefaultXmlGathererErrorHandler();
  }

  @Test
  public void xmlGathererErrorHandlerShouldGatherErrors() {
    XMLParseException e = new XMLParseException(null, "First fake xml parse exception");
    errorHandler.error("domain", "key", e);
    XMLParseException e2 = new XMLParseException(null, "Second fake xml parse exception");
    errorHandler.error("domain", "key", e2);
    assertThat(errorHandler.getErrors().size(), is(2));
    assertThat(errorHandler.getErrors().get(0), is(sameInstance(e)));
    assertThat(errorHandler.getErrors().get(1), is(sameInstance(e2)));
  }

  @Test
  public void xmlGathererErrorHandlerShouldNotGatherWarning() {
    XMLParseException exception = new XMLParseException(null, "Fake xml parse exception");
    errorHandler.warning("domain", "key", exception);
    assertThat(errorHandler.getErrors().size(), is(0));
  }

  @Test
  public void xmlGathererErrorHandlerShouldFailOnFatalError() {
    thrown.expect(XMLParseException.class);
    XMLParseException exception = new XMLParseException(null, "Fake xml parse exception");
    errorHandler.fatalError("domain", "key", exception);
  }
}
