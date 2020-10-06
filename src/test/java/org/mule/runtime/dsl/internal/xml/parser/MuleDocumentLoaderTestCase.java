/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xml.parser;

import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.mock;

import io.qameta.allure.Issue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.dsl.api.xml.parser.XmlGathererErrorHandler;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParserFactory;
import java.net.URLClassLoader;

public class MuleDocumentLoaderTestCase {

  private static final String SIMPLE_APPLICATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
      "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
      "      xsi:schemaLocation=\"\n" +
      "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n" +
      "\n" +
      "    <flow name=\"test\">\n" +
      "        <logger category=\"SOMETHING\" level=\"WARN\" message=\"logging info\"/>\n" +
      "    </flow>\n" +
      "\n" +
      "</mule>";

  private ClassLoader originalClassLoader;

  @Before
  public void setup() {
    originalClassLoader = currentThread().getContextClassLoader();
  }

  @After
  public void tearDown() {
    currentThread().setContextClassLoader(originalClassLoader);
  }

  @Test
  @Issue("MULE-18813")
  public void documentLoaderShouldNotUseThreadCurrentClassLoader() throws Exception {

    MuleDocumentLoader loader = new MuleDocumentLoader();

    // Setting failure class loader
    URLClassLoader failing = mock(URLClassLoader.class);
    currentThread().setContextClassLoader(failing);

    InputSource is = new InputSource(toInputStream(SIMPLE_APPLICATION, UTF_8));
    XmlGathererErrorHandler errorHandler = new DefaultXmlGathererErrorHandlerFactory().create();
    Document document = loader.loadDocument(SAXParserFactory::newInstance, is, null, errorHandler, 0, false);

    verifyZeroInteractions(failing);

    assertThat(document, is(notNullValue()));
    assertThat(errorHandler.getErrors().size(), is(0));
  }
}
