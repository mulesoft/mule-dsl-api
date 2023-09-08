/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.dsl.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;
import static org.mule.runtime.dsl.internal.util.SchemasConstants.CORE_XSD;
import static org.mule.runtime.dsl.internal.util.SchemasConstants.CORE_CURRENT_XSD;
import static org.mule.runtime.dsl.internal.util.SchemaMappingsUtils.resolveSystemId;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class SchemaMappingsUtilsTestCase {

  private static final String LEGACY_SPRING_XSD = "http://www.springframework.org/schema/beans/spring-beans-current.xsd";
  private static final String UNKNOW_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-unknow.xsd";

  @Test
  @Issue("MULE-16572")
  public void legacySpring() {
    String systemId = resolveSystemId(LEGACY_SPRING_XSD);
    assertThat(systemId, is("http://www.springframework.org/schema/beans/spring-beans.xsd"));
  }

  @Test
  public void unknownSystemIdShouldNotBeResolved() {
    String systemId = resolveSystemId(UNKNOW_XSD);
    assertThat(systemId, is(systemId));
  }

  @Test
  public void coreCurrentXsdShouldBeResolved() {
    String systemId = resolveSystemId(CORE_XSD);
    assertThat(systemId, is(CORE_CURRENT_XSD));
  }
}
