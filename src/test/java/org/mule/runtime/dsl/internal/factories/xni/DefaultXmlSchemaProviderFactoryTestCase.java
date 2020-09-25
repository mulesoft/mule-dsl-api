/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.factories.xni;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;
import static org.mule.runtime.dsl.api.factories.xni.XmlSchemaProviderFactory.getDefault;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.dsl.api.factories.xni.XmlSchemaProviderFactory;
import org.mule.runtime.dsl.api.xni.parser.XmlSchemaProvider;
import org.mule.runtime.dsl.internal.xni.parser.DefaultXmlSchemaProvider;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlSchemaProviderFactoryTestCase {

  private DefaultXmlSchemaProviderFactory factory;

  @Before
  public void setup() {
    this.factory = new DefaultXmlSchemaProviderFactory();
  }

  @Test
  public void createXmlSchemaProvider() {
    XmlSchemaProvider schemaProvider = factory.create();
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
