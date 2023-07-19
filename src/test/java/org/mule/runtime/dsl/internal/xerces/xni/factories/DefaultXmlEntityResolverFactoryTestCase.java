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
import static org.mule.runtime.dsl.api.xerces.xni.factories.XmlEntityResolverFactory.getDefault;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.apache.xerces.xni.parser.XMLEntityResolver;
import org.mule.runtime.dsl.api.xerces.xni.factories.XmlEntityResolverFactory;
import org.mule.runtime.dsl.internal.xerces.xni.parser.DefaultXmlEntityResolver;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlEntityResolverFactoryTestCase {

  @Test
  public void createXmlEntityResolver() {
    XMLEntityResolver entityResolver = DefaultXmlEntityResolverFactory.getInstance().create();
    assertThat(entityResolver, is(notNullValue()));
    assertThat(entityResolver, is(instanceOf(DefaultXmlEntityResolver.class)));
  }

  @Test
  public void createDefaultXmlEntityResolverFactory() {
    XmlEntityResolverFactory factory = getDefault();
    assertThat(factory, is(notNullValue()));
    assertThat(factory, is(instanceOf(DefaultXmlEntityResolverFactory.class)));
  }
}
