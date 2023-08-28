/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xml.parser;

import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import static org.mule.runtime.dsl.internal.xml.parser.XmlMetadataAnnotations.METADATA_ANNOTATIONS_KEY;

import io.qameta.allure.Issue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.dsl.api.xml.parser.XmlGathererErrorHandler;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.net.URLClassLoader;

public class MuleDocumentLoaderTestCase {

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

    InputStream inputStream = currentThread().getContextClassLoader().getResourceAsStream("simple_application.xml");
    MuleDocumentLoader loader = new MuleDocumentLoader();

    // Setting failure class loader
    URLClassLoader failing = mock(URLClassLoader.class);
    currentThread().setContextClassLoader(failing);

    InputSource is = new InputSource(inputStream);
    XmlGathererErrorHandler errorHandler = new DefaultXmlGathererErrorHandlerFactory().create();
    Document document = loader.loadDocument(SAXParserFactory::newInstance, is, null, errorHandler, 0, false, null);

    verifyNoInteractions(failing);

    assertThat(document, is(notNullValue()));
    assertThat(errorHandler.getErrors(), is(empty()));
  }

  @Test
  public void xmlMetadataIsProperlyPopulatedWhenParsingSimpleApplication() throws Exception {

    InputStream inputStream = currentThread().getContextClassLoader().getResourceAsStream("simple_application.xml");
    MuleDocumentLoader loader = new MuleDocumentLoader();

    InputSource is = new InputSource(inputStream);
    XmlGathererErrorHandler errorHandler = new DefaultXmlGathererErrorHandlerFactory().create();
    Document document = loader.loadDocument(SAXParserFactory::newInstance, is, null, errorHandler, 0, false, null);

    assertThat(document, is(notNullValue()));
    assertThat(errorHandler.getErrors(), is(empty()));

    XmlMetadataAnnotations rootAnnotations =
        (XmlMetadataAnnotations) document.getDocumentElement().getUserData(METADATA_ANNOTATIONS_KEY);
    assertThat(rootAnnotations, is(not(nullValue())));
    assertThat(rootAnnotations.isSelfClosing(), is(false));
    // FIXME MULE-19799: these are currently returning the beginning of the document instead of the beginning of the first tag
    // assertThat(rootAnnotations.getOpeningTagBoundaries().getStartLineNumber(), is(2));
    // assertThat(rootAnnotations.getOpeningTagBoundaries().getStartColumnNumber(), is(1));
    assertThat(rootAnnotations.getOpeningTagBoundaries().getEndLineNumber(), is(5));
    assertThat(rootAnnotations.getOpeningTagBoundaries().getEndColumnNumber(), is(108));

    assertThat(rootAnnotations.getClosingTagBoundaries().getStartLineNumber(), is(11));
    assertThat(rootAnnotations.getClosingTagBoundaries().getStartColumnNumber(), is(1));
    assertThat(rootAnnotations.getClosingTagBoundaries().getEndLineNumber(), is(11));
    assertThat(rootAnnotations.getClosingTagBoundaries().getEndColumnNumber(), is(8));

    XmlMetadataAnnotations flowAnnotations =
        (XmlMetadataAnnotations) document.getElementsByTagName("flow").item(0).getUserData(METADATA_ANNOTATIONS_KEY);
    assertThat(flowAnnotations, is(not(nullValue())));
    assertThat(flowAnnotations.isSelfClosing(), is(false));
    assertThat(flowAnnotations.getOpeningTagBoundaries().getStartLineNumber(), is(7));
    assertThat(flowAnnotations.getOpeningTagBoundaries().getStartColumnNumber(), is(5));
    assertThat(flowAnnotations.getOpeningTagBoundaries().getEndLineNumber(), is(7));
    assertThat(flowAnnotations.getOpeningTagBoundaries().getEndColumnNumber(), is(23));

    assertThat(flowAnnotations.getClosingTagBoundaries().getStartLineNumber(), is(9));
    assertThat(flowAnnotations.getClosingTagBoundaries().getStartColumnNumber(), is(5));
    assertThat(flowAnnotations.getClosingTagBoundaries().getEndLineNumber(), is(9));
    assertThat(flowAnnotations.getClosingTagBoundaries().getEndColumnNumber(), is(12));

    XmlMetadataAnnotations loggerAnnotations =
        (XmlMetadataAnnotations) document.getElementsByTagName("logger").item(0).getUserData(METADATA_ANNOTATIONS_KEY);
    assertThat(loggerAnnotations, is(not(nullValue())));
    assertThat(loggerAnnotations.isSelfClosing(), is(true));
    assertThat(loggerAnnotations.getOpeningTagBoundaries().getStartLineNumber(), is(8));
    assertThat(loggerAnnotations.getOpeningTagBoundaries().getStartColumnNumber(), is(9));
    assertThat(loggerAnnotations.getOpeningTagBoundaries().getEndLineNumber(), is(8));
    assertThat(loggerAnnotations.getOpeningTagBoundaries().getEndColumnNumber(), is(75));

    assertThat(loggerAnnotations.getClosingTagBoundaries().getStartLineNumber(), is(8));
    assertThat(loggerAnnotations.getClosingTagBoundaries().getStartColumnNumber(), is(9));
    assertThat(loggerAnnotations.getClosingTagBoundaries().getEndLineNumber(), is(8));
    assertThat(loggerAnnotations.getClosingTagBoundaries().getEndColumnNumber(), is(75));
  }

  @Test
  public void xmlMetadataIsProperlyPopulatedWhenWhitespaceBetweenLineFeed() throws Exception {

    InputStream inputStream =
        currentThread().getContextClassLoader().getResourceAsStream("simple_application_with_whitespace_between_linefeed.xml");
    MuleDocumentLoader loader = new MuleDocumentLoader();

    InputSource is = new InputSource(inputStream);
    XmlGathererErrorHandler errorHandler = new DefaultXmlGathererErrorHandlerFactory().create();
    Document document = loader.loadDocument(SAXParserFactory::newInstance, is, null, errorHandler, 0, false, null);

    assertThat(document, is(notNullValue()));
    assertThat(errorHandler.getErrors(), is(empty()));

    XmlMetadataAnnotations rootAnnotations =
        (XmlMetadataAnnotations) document.getDocumentElement().getUserData(METADATA_ANNOTATIONS_KEY);
    assertThat(rootAnnotations, is(not(nullValue())));
    assertThat(rootAnnotations.isSelfClosing(), is(false));
    // FIXME MULE-19799: these are currently returning the beginning of the document instead of the beginning of the first tag
    // assertThat(rootAnnotations.getOpeningTagBoundaries().getStartLineNumber(), is(2));
    // assertThat(rootAnnotations.getOpeningTagBoundaries().getStartColumnNumber(), is(1));
    assertThat(rootAnnotations.getOpeningTagBoundaries().getEndLineNumber(), is(5));
    assertThat(rootAnnotations.getOpeningTagBoundaries().getEndColumnNumber(), is(108));

    assertThat(rootAnnotations.getClosingTagBoundaries().getStartLineNumber(), is(13));
    assertThat(rootAnnotations.getClosingTagBoundaries().getStartColumnNumber(), is(1));
    assertThat(rootAnnotations.getClosingTagBoundaries().getEndLineNumber(), is(13));
    assertThat(rootAnnotations.getClosingTagBoundaries().getEndColumnNumber(), is(8));

    XmlMetadataAnnotations flowAnnotations =
        (XmlMetadataAnnotations) document.getElementsByTagName("flow").item(0).getUserData(METADATA_ANNOTATIONS_KEY);
    assertThat(flowAnnotations, is(not(nullValue())));
    assertThat(flowAnnotations.isSelfClosing(), is(false));
    assertThat(flowAnnotations.getOpeningTagBoundaries().getStartLineNumber(), is(9));
    assertThat(flowAnnotations.getOpeningTagBoundaries().getStartColumnNumber(), is(5));
    assertThat(flowAnnotations.getOpeningTagBoundaries().getEndLineNumber(), is(9));
    assertThat(flowAnnotations.getOpeningTagBoundaries().getEndColumnNumber(), is(23));

    assertThat(flowAnnotations.getClosingTagBoundaries().getStartLineNumber(), is(11));
    assertThat(flowAnnotations.getClosingTagBoundaries().getStartColumnNumber(), is(5));
    assertThat(flowAnnotations.getClosingTagBoundaries().getEndLineNumber(), is(11));
    assertThat(flowAnnotations.getClosingTagBoundaries().getEndColumnNumber(), is(12));

    XmlMetadataAnnotations loggerAnnotations =
        (XmlMetadataAnnotations) document.getElementsByTagName("logger").item(0).getUserData(METADATA_ANNOTATIONS_KEY);
    assertThat(loggerAnnotations, is(not(nullValue())));
    assertThat(loggerAnnotations.isSelfClosing(), is(true));
    assertThat(loggerAnnotations.getOpeningTagBoundaries().getStartLineNumber(), is(10));
    assertThat(loggerAnnotations.getOpeningTagBoundaries().getStartColumnNumber(), is(9));
    assertThat(loggerAnnotations.getOpeningTagBoundaries().getEndLineNumber(), is(10));
    assertThat(loggerAnnotations.getOpeningTagBoundaries().getEndColumnNumber(), is(75));

    assertThat(loggerAnnotations.getClosingTagBoundaries().getStartLineNumber(), is(10));
    assertThat(loggerAnnotations.getClosingTagBoundaries().getStartColumnNumber(), is(9));
    assertThat(loggerAnnotations.getClosingTagBoundaries().getEndLineNumber(), is(10));
    assertThat(loggerAnnotations.getClosingTagBoundaries().getEndColumnNumber(), is(75));
  }
}
