/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xerces.xni.factories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;
import static org.mule.runtime.dsl.api.xerces.xni.factories.XmlGathererErrorHandlerFactory.getDefault;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.dsl.api.xerces.xni.factories.XmlGathererErrorHandlerFactory;
import org.mule.runtime.dsl.internal.xerces.xni.parser.XmlGathererErrorHandler;
import org.mule.runtime.dsl.internal.xerces.xni.parser.DefaultXmlGathererErrorHandler;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlGathererErrorHandlerFactoryTestCase {

  @Test
  public void createXmlGathererErrorHandler() {
    XmlGathererErrorHandler errorHandler = DefaultXmlGathererErrorHandlerFactory.getInstance().create();
    assertThat(errorHandler, is(notNullValue()));
    assertThat(errorHandler, is(instanceOf(DefaultXmlGathererErrorHandler.class)));
  }

  @Test
  public void createDefaultXmlGathererErrorHandlerFactory() {
    XmlGathererErrorHandlerFactory factory = getDefault();
    assertThat(factory, is(notNullValue()));
    assertThat(factory, is(instanceOf(DefaultXmlGathererErrorHandlerFactory.class)));
  }
}
