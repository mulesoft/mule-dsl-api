/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;

import org.mule.apache.xerces.impl.xs.XSDDescription;
import org.mule.apache.xerces.xni.parser.XMLInputSource;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlEntityResolverTestCase {

  private static final String INVALID_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-invalid.xsd";
  private static final String INVALID_TARGET_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-invalid-target.xsd";
  private static final String COMPANY_XSD = "http://www.mulesoft.org/schema/mule/fake-company/current/company.xsd";

  private DefaultXmlEntityResolver entityResolver;

  @Before
  public void setup() {
    this.entityResolver = new DefaultXmlEntityResolver();
  }

  @Test
  public void emptyXSDDescriptionShouldReturnNullSource() throws Exception {
    XSDDescription resourceIdentifier = new XSDDescription();
    resourceIdentifier.setPublicId(null);
    resourceIdentifier.setExpandedSystemId(null);
    XMLInputSource source = entityResolver.resolveEntity(resourceIdentifier);
    assertThat(source, is(nullValue()));
  }

  @Test
  public void invalidSchemaMappingsShouldReturnNullSource() throws Exception {
    XSDDescription resourceIdentifier = new XSDDescription();
    resourceIdentifier.setPublicId(null);
    resourceIdentifier.setExpandedSystemId(INVALID_XSD);
    XMLInputSource source = entityResolver.resolveEntity(resourceIdentifier);
    assertThat(source, is(nullValue()));
  }

  @Test
  public void existingSchemaMappingsShouldReturnValidSource() throws Exception {
    XSDDescription resourceIdentifier = new XSDDescription();
    resourceIdentifier.setPublicId(null);
    resourceIdentifier.setExpandedSystemId(COMPANY_XSD);
    XMLInputSource is = entityResolver.resolveEntity(resourceIdentifier);
    assertThat(is, is(notNullValue()));
    assertThat(is.getPublicId(), is(nullValue()));
    assertThat(is.getSystemId(), is(notNullValue()));
    assertThat(is.getBaseSystemId(), is(nullValue()));
    assertThat(is.getByteStream(), is(notNullValue()));
  }

  @Test
  public void existingSchemaMappingsWithInvalidResourceShouldReturnNulSource() throws Exception {
    XSDDescription resourceIdentifier = new XSDDescription();
    resourceIdentifier.setPublicId(null);
    resourceIdentifier.setExpandedSystemId(INVALID_TARGET_XSD);
    XMLInputSource is = entityResolver.resolveEntity(resourceIdentifier);
    assertThat(is, is(nullValue()));
  }
}
