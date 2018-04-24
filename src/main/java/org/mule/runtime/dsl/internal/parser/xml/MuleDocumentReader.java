/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.parser.xml;

import static org.apache.commons.lang3.SystemUtils.LINE_SEPARATOR;
import static org.mule.runtime.dsl.internal.parser.xml.XmlMetadataAnnotations.METADATA_ANNOTATIONS_KEY;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mule.runtime.api.exception.MuleRuntimeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * TODO document
 */
public class MuleDocumentReader {

  /**
   * JAXP attribute used to configure the schema language for validation.
   */
  private static final String SCHEMA_LANGUAGE_ATTRIBUTE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

  /**
   * JAXP attribute value indicating the XSD schema language.
   */
  private static final String XSD_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";


  private static final Logger logger = LoggerFactory.getLogger(MuleDocumentReader.class);

  private static final UserDataHandler COPY_METADATA_ANNOTATIONS_DATA_HANDLER = new UserDataHandler() {

    @Override
    public void handle(short operation, String key, Object data, Node src, Node dst) {
      if (operation == NODE_IMPORTED || operation == NODE_CLONED) {
        dst.setUserData(METADATA_ANNOTATIONS_KEY, src.getUserData(METADATA_ANNOTATIONS_KEY), this);
      }
    }
  };

  private final XmlMetadataAnnotationsFactory metadataFactory;

  public MuleDocumentReader() {
    this.metadataFactory = new DefaultXmlMetadataFactory();
  }


  /**
   * Load the {@link Document} at the supplied {@link InputSource} using the standard JAXP-configured XML parser.
   */
  public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
                               ErrorHandler errorHandler, boolean enableValidations)
      throws Exception {

    // TODO review if this copy is really needed
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (InputStream inputStream = inputSource.getByteStream()) {

      copy(inputStream, output);
    }

    InputSource defaultInputSource = new InputSource(new ByteArrayInputStream(output.toByteArray()));
    InputSource enrichInputSource = new InputSource(new ByteArrayInputStream(output.toByteArray()));

    DocumentBuilderFactory factory = createDocumentBuilderFactory(enableValidations);
    DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
    Document document = builder.parse(defaultInputSource);
    createSaxAnnotator(document).parse(enrichInputSource);
    return document;
  }



  protected XMLReader createSaxAnnotator(Document doc) throws ParserConfigurationException, SAXException {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    SAXParser saxParser = saxParserFactory.newSAXParser();
    XMLReader documentReader = saxParser.getXMLReader();
    documentReader.setContentHandler(new XmlMetadataAnnotator(doc, metadataFactory));
    return documentReader;
  }

  protected DocumentBuilderFactory createDocumentBuilderFactory(boolean enableValidations)
      throws ParserConfigurationException {

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);

    if (enableValidations) {
      factory.setValidating(true);

      factory.setNamespaceAware(true);
      try {
        factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
      } catch (IllegalArgumentException ex) {
        ParserConfigurationException pcex = new ParserConfigurationException(
                                                                             "Unable to validate using XSD: Your JAXP provider ["
                                                                                 + factory +
                                                                                 "] does not support XML Schema. Are you running on Java 1.4 with Apache Crimson? "
                                                                                 +
                                                                                 "Upgrade to Apache Xerces (or Java 1.5) for full XSD support.");
        pcex.initCause(ex);
        throw pcex;
      }
    }
    return factory;
  }

  /**
   * Create a JAXP DocumentBuilder that this bean definition reader will use for parsing XML documents. Can be overridden in
   * subclasses, adding further initialization of the builder.
   * 
   * @param factory the JAXP DocumentBuilderFactory that the DocumentBuilder should be created with
   * @param entityResolver the SAX EntityResolver to use
   * @param errorHandler the SAX ErrorHandler to use
   * @return the JAXP DocumentBuilder
   * @throws ParserConfigurationException if thrown by JAXP methods
   */
  protected DocumentBuilder createDocumentBuilder(
                                                  DocumentBuilderFactory factory, EntityResolver entityResolver,
                                                  ErrorHandler errorHandler)
      throws ParserConfigurationException {

    DocumentBuilder docBuilder = factory.newDocumentBuilder();
    if (entityResolver != null) {
      docBuilder.setEntityResolver(entityResolver);
    }
    if (errorHandler != null) {
      docBuilder.setErrorHandler(errorHandler);
    }
    return docBuilder;
  }

  private final class DefaultXmlMetadataFactory implements XmlMetadataAnnotationsFactory {

    @Override
    public XmlMetadataAnnotations create(Locator locator) {
      DefaultXmlMetadataAnnotations annotations = new DefaultXmlMetadataAnnotations();
      annotations.setLineNumber(locator.getLineNumber());
      annotations.setColumnNumber(locator.getColumnNumber());
      return annotations;
    }
  }

  /**
   * SAX filter that builds the metadata that will annotate the built nodes.
   */
  public final static class XmlMetadataAnnotator extends DefaultHandler {

    private Locator locator;
    private DomWalkerElement walker;
    private XmlMetadataAnnotationsFactory metadataFactory;
    private Stack<XmlMetadataAnnotations> annotationsStack = new Stack<>();

    private XmlMetadataAnnotator(Document doc, XmlMetadataAnnotationsFactory metadataFactory) {
      this.walker = new DomWalkerElement(doc.getDocumentElement());
      this.metadataFactory = metadataFactory;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
      super.setDocumentLocator(locator);
      this.locator = locator;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      walker = walker.walkIn();

      XmlMetadataAnnotations metadataBuilder = metadataFactory.create(locator);
      LinkedHashMap<String, String> attsMap = new LinkedHashMap<>();
      for (int i = 0; i < atts.getLength(); ++i) {
        attsMap.put(atts.getQName(i), atts.getValue(i));
      }
      metadataBuilder.appendElementStart(qName, attsMap);
      annotationsStack.push(metadataBuilder);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      annotationsStack.peek().appendElementBody(new String(ch, start, length).trim());
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      XmlMetadataAnnotations metadataAnnotations = annotationsStack.pop();
      metadataAnnotations.appendElementEnd(qName);

      if (!annotationsStack.isEmpty()) {
        XmlMetadataAnnotations xmlMetadataAnnotations = annotationsStack.peek();
        xmlMetadataAnnotations
            .appendElementBody(LINE_SEPARATOR + metadataAnnotations.getElementString() + LINE_SEPARATOR);
        if (xmlMetadataAnnotations instanceof DefaultXmlMetadataAnnotations) {
          ((DefaultXmlMetadataAnnotations) xmlMetadataAnnotations).setEndLineNumber(locator.getLineNumber());
          ((DefaultXmlMetadataAnnotations) xmlMetadataAnnotations).setEndColumnNumber(locator.getColumnNumber());
        }
      }

      walker.getParentNode().setUserData(METADATA_ANNOTATIONS_KEY, metadataAnnotations, COPY_METADATA_ANNOTATIONS_DATA_HANDLER);
      walker = walker.walkOut();
    }
  }

  /**
   * Allows for sequential navigation of a DOM tree.
   */
  private final static class DomWalkerElement {

    private final DomWalkerElement parent;
    private final Node node;

    private int childIndex = 0;

    public DomWalkerElement(Node node) {
      this.parent = null;
      this.node = node;
    }

    private DomWalkerElement(DomWalkerElement parent, Node node) {
      this.parent = parent;
      this.node = node;
    }

    public DomWalkerElement walkIn() {
      Node nextChild = node.getChildNodes().item(childIndex++);
      while (nextChild != null && nextChild.getNodeType() != Node.ELEMENT_NODE) {
        nextChild = node.getChildNodes().item(childIndex++);
      }
      return new DomWalkerElement(this, nextChild);
    }

    public DomWalkerElement walkOut() {
      Node nextSibling = parent.node.getNextSibling();
      while (nextSibling != null && nextSibling.getNodeType() != Node.ELEMENT_NODE) {
        nextSibling = nextSibling.getNextSibling();
      }
      return new DomWalkerElement(parent.parent, nextSibling);
    }

    public Node getParentNode() {
      return parent.node;
    }
  }

  private void copy(InputStream inputStream, ByteArrayOutputStream outputStream) {
    try {
      try {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
          outputStream.write(buffer, 0, length);
        }
      } finally {
        inputStream.close();
        outputStream.close();
      }
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }
}
