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
import static org.mule.runtime.dsl.api.xerces.xni.factories.XmlSchemaProviderFactory.getDefault;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.dsl.api.xerces.xni.factories.XmlSchemaProviderFactory;
import org.mule.runtime.dsl.internal.xerces.xni.parser.XmlSchemaProvider;
import org.mule.runtime.dsl.internal.xerces.xni.parser.DefaultXmlSchemaProvider;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlSchemaProviderFactoryTestCase {

  @Test
  public void createXmlSchemaProvider() {
    XmlSchemaProvider schemaProvider = DefaultXmlSchemaProviderFactory.getInstance().create();
    assertThat(schemaProvider, is(notNullValue()));
    assertThat(schemaProvider, is(instanceOf(DefaultXmlSchemaProvider.class)));
  }

  @Test
  public void createDefaultXmlSchemaProviderFactory() {
    XmlSchemaProviderFactory factory = getDefault();
    assertThat(factory, is(notNullValue()));
    assertThat(factory, is(instanceOf(DefaultXmlSchemaProviderFactory.class)));
  }
}
