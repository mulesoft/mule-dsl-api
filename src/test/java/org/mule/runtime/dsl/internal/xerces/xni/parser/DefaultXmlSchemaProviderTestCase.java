/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;

import org.mule.apache.xerces.xni.parser.XMLInputSource;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlSchemaProviderTestCase {

  private DefaultXmlSchemaProvider schemaProvider;

  @Before
  public void setup() {
    this.schemaProvider = new DefaultXmlSchemaProvider();
  }

  @Test
  public void retrieveDefaultXmlSchemas() {
    List<XMLInputSource> schemas = schemaProvider.getSchemas();
    assertThat(schemas, is(notNullValue()));
    assertThat(schemas.isEmpty(), is(false));
    List<XMLInputSource> fakesSchemas = schemas.stream().filter(is -> is.getSystemId().contains("fake-")).collect(toList());
    assertThat(fakesSchemas.size(), is(3));

    for (XMLInputSource is : schemas) {
      assertThat(is, is(notNullValue()));
      assertThat(is.getPublicId(), is(nullValue()));
      assertThat(is.getSystemId(), is(notNullValue()));
      assertThat(is.getBaseSystemId(), is(nullValue()));
      assertThat(is.getByteStream(), is(notNullValue()));
    }
  }
}
