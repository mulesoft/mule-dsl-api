/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import static com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription.XML_DTD;
import static com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription.XML_SCHEMA;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.tuple.Pair.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;

import com.sun.org.apache.xerces.internal.util.XMLResourceIdentifierImpl;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.mule.runtime.dsl.api.xerces.xni.factories.XmlEntityResolverFactory;
import org.mule.runtime.dsl.api.xerces.xni.factories.XmlGathererErrorHandlerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlGrammarPoolBuilderTestCase {

  private static final Pair<String, String> COMPANY_XSD =
      of("http://www.mulesoft.org/schema/mule/fake-company/current/company.xsd", "META-INF/fake-company/company.xsd");
  private static final Pair<String, String> PERSON_XSD =
      of("http://www.mulesoft.org/schema/mule/fake-person/current/person.xsd", "META-INF/fake-company/person.xsd");
  private static final Pair<String, String> PRODUCT_XSD =
      of("http://www.mulesoft.org/schema/mule/fake-product/current/product.xsd", "META-INF/fake-company/product.xsd");
  private static final Pair<String, String> FAKE_XSD =
      of("http://www.mulesoft.org/schema/mule/core/current/mule-invalid-target.xsd", "META-INF/mule-unexisting-target.xsd");

  private XMLInputSource createXmlInputSource(Pair<String, String> schema) {
    return createXmlInputSource(schema.getLeft(), schema.getRight());
  }

  private XMLInputSource createXmlInputSource(String systemId, String resourceLocation) {
    InputStream is = DefaultXmlGrammarPoolBuilderTestCase.class.getClassLoader().getResourceAsStream(resourceLocation);
    XMLResourceIdentifier resourceIdentifier = new XMLResourceIdentifierImpl();
    resourceIdentifier.setPublicId(null);
    resourceIdentifier.setLiteralSystemId(systemId);
    resourceIdentifier.setBaseSystemId(null);
    XMLInputSource xis = new XMLInputSource(resourceIdentifier);
    xis.setByteStream(is);
    return xis;
  }

  @Test
  public void emptySchemaProviderShouldReturnEmptyGrammarPool() {
    XmlSchemaProvider emptySchemaProvider = mock(XmlSchemaProvider.class);
    when(emptySchemaProvider.getSchemas()).thenReturn(new ArrayList<>());

    XmlGathererErrorHandler errorHandler = mock(XmlGathererErrorHandler.class);
    XMLEntityResolver entityResolver = mock(XMLEntityResolver.class);
    DefaultXmlGrammarPoolBuilder builder = new DefaultXmlGrammarPoolBuilder(emptySchemaProvider, errorHandler, entityResolver);
    XMLGrammarPool xmlGrammarPool = builder.build();

    assertThat(xmlGrammarPool, is(notNullValue()));
    assertThat(xmlGrammarPool, is(instanceOf(ReadOnlyXmlGrammarPool.class)));

    Grammar[] grammars = xmlGrammarPool.retrieveInitialGrammarSet(XML_SCHEMA);
    assertThat(grammars, is(notNullValue()));
    assertThat(grammars.length, is(0));

    grammars = xmlGrammarPool.retrieveInitialGrammarSet(XML_DTD);
    assertThat(grammars.length, is(0));
  }

  @Test
  public void failureWhileCreatingXmlGrammarPoolShouldReturnEmptyGrammarPool() {
    XmlSchemaProvider emptySchemaProvider = mock(XmlSchemaProvider.class);
    when(emptySchemaProvider.getSchemas()).thenThrow(new RuntimeException("Fake error"));

    XmlGathererErrorHandler errorHandler = mock(XmlGathererErrorHandler.class);
    XMLEntityResolver entityResolver = mock(XMLEntityResolver.class);
    DefaultXmlGrammarPoolBuilder builder = new DefaultXmlGrammarPoolBuilder(emptySchemaProvider, errorHandler, entityResolver);
    XMLGrammarPool xmlGrammarPool = builder.build();

    assertThat(xmlGrammarPool, is(notNullValue()));
    assertThat(xmlGrammarPool, is(instanceOf(ReadOnlyXmlGrammarPool.class)));

    Grammar[] grammars = xmlGrammarPool.retrieveInitialGrammarSet(XML_SCHEMA);
    assertThat(grammars, is(notNullValue()));
    assertThat(grammars.length, is(0));

    grammars = xmlGrammarPool.retrieveInitialGrammarSet(XML_DTD);
    assertThat(grammars.length, is(0));

    XMLGrammarDescription description = mock(XMLGrammarDescription.class);
    Grammar grammar = xmlGrammarPool.retrieveGrammar(description);
    assertThat(grammar, is(nullValue()));
  }

  @Test
  public void failureWhileParsingSchemasShouldReturnEmptyGrammarPool() {
    List<Pair<String, String>> schemas = new ArrayList<>();
    schemas.add(COMPANY_XSD);
    schemas.add(PERSON_XSD);
    schemas.add(PRODUCT_XSD);
    schemas.add(FAKE_XSD);
    XmlSchemaProvider emptySchemaProvider = mock(XmlSchemaProvider.class);
    when(emptySchemaProvider.getSchemas()).thenReturn(schemas.stream().map(this::createXmlInputSource).collect(toList()));

    XmlGathererErrorHandler errorHandler = XmlGathererErrorHandlerFactory.getDefault().create();
    XMLEntityResolver entityResolver = XmlEntityResolverFactory.getDefault().create();
    DefaultXmlGrammarPoolBuilder builder = new DefaultXmlGrammarPoolBuilder(emptySchemaProvider, errorHandler, entityResolver);
    XMLGrammarPool xmlGrammarPool = builder.build();

    assertThat(xmlGrammarPool, is(notNullValue()));
    assertThat(xmlGrammarPool, is(instanceOf(ReadOnlyXmlGrammarPool.class)));

    Grammar[] grammars = xmlGrammarPool.retrieveInitialGrammarSet(XML_SCHEMA);
    assertThat(grammars, is(notNullValue()));
    assertThat(grammars.length, is(0));

    grammars = xmlGrammarPool.retrieveInitialGrammarSet(XML_DTD);
    assertThat(grammars.length, is(0));

    assertThat(errorHandler.getErrors().size(), is(1));

    XMLGrammarDescription description = mock(XMLGrammarDescription.class);
    Grammar grammar = xmlGrammarPool.retrieveGrammar(description);
    assertThat(grammar, is(nullValue()));
  }

  @Test
  public void createXmlGrammarPool() {
    List<Pair<String, String>> schemas = new ArrayList<>();
    schemas.add(COMPANY_XSD);
    schemas.add(PERSON_XSD);
    schemas.add(PRODUCT_XSD);
    XmlSchemaProvider emptySchemaProvider = mock(XmlSchemaProvider.class);
    when(emptySchemaProvider.getSchemas()).thenReturn(schemas.stream().map(this::createXmlInputSource).collect(toList()));

    XmlGathererErrorHandler errorHandler = XmlGathererErrorHandlerFactory.getDefault().create();
    XMLEntityResolver entityResolver = XmlEntityResolverFactory.getDefault().create();
    DefaultXmlGrammarPoolBuilder builder = new DefaultXmlGrammarPoolBuilder(emptySchemaProvider, errorHandler, entityResolver);
    XMLGrammarPool xmlGrammarPool = builder.build();

    assertThat(xmlGrammarPool, is(notNullValue()));
    assertThat(xmlGrammarPool, is(instanceOf(ReadOnlyXmlGrammarPool.class)));

    Grammar[] grammars = xmlGrammarPool.retrieveInitialGrammarSet(XML_SCHEMA);
    assertThat(grammars, is(notNullValue()));
    assertThat(grammars.length, is(3));

    grammars = xmlGrammarPool.retrieveInitialGrammarSet(XML_DTD);
    assertThat(grammars.length, is(0));

    assertThat(errorHandler.getErrors().isEmpty(), is(true));
  }
}
