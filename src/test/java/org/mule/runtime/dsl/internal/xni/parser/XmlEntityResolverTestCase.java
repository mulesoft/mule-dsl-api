/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xni.parser;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;

import com.sun.org.apache.xerces.internal.impl.xs.XSDDescription;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class XmlEntityResolverTestCase {

  private XmlEntityResolver resolveEntity;

  @Before
  public void setup() {
    resolveEntity = new XmlEntityResolver();
  }

  @Test
  public void legacySpring() throws Exception {
    XSDDescription resourceIdentifier = new XSDDescription();
    resourceIdentifier.setPublicId(null);
    resourceIdentifier.setExpandedSystemId("http://www.springframework.org/schema/beans/spring-beans-current.xsd");
    XMLInputSource source = resolveEntity.resolveEntity(resourceIdentifier);
    // InputSource source = resolver.resolveEntity(null, "http://www.springframework.org/schema/beans/spring-beans-current.xsd");
    //assertThat(source, is(not(nullValue())));
    //assertThat(IOUtils.toString(source.getByteStream()), is(not(isEmptyString())));
  }
}
