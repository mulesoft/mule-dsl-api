/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xml.parser;

import static java.lang.System.lineSeparator;
import static java.lang.Thread.currentThread;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.apache.xerces.impl.xs.SchemaValidatorHelper.XMLGRAMMAR_POOL;
import static org.mule.runtime.dsl.internal.xml.parser.XmlMetadataAnnotations.METADATA_ANNOTATIONS_KEY;
import static org.mule.runtime.internal.util.xmlsecurity.DefaultXMLSecureFactories.DOCUMENT_BUILDER_FACTORY;

import org.mule.apache.xerces.xni.grammars.XMLGrammarPool;
import org.mule.runtime.dsl.internal.SourcePosition;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
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
 * Alternative to Spring's default document loader that uses <b>SAX</b> to add metadata to the <b>DOM</b> elements that are the
 * result of the default parser.
 *
 * @since 3.8.0
 */
final public class MuleDocumentLoader {

  private static final String MULE_DOCUMENT_BUILDER_FACTORY = "org.mule.apache.xerces.jaxp.DocumentBuilderFactoryImpl";

  private static final String SCHEMA_AUGMENT_PSVI_FEATURE = "http://apache.org/xml/features/validation/schema/augment-psvi";

  private static final UserDataHandler COPY_METADATA_ANNOTATIONS_DATA_HANDLER = new UserDataHandler() {

    @Override
    public void handle(short operation, String key, Object data, Node src, Node dst) {
      if (operation == NODE_IMPORTED || operation == NODE_CLONED) {
        dst.setUserData(METADATA_ANNOTATIONS_KEY, src.getUserData(METADATA_ANNOTATIONS_KEY), this);
      }
    }
  };

  private final XmlMetadataAnnotationsFactory metadataFactory;

  public MuleDocumentLoader() {
    this.metadataFactory = new DefaultXmlMetadataFactory();
  }

  /**
   * Load the {@link Document} at the supplied {@link InputSource} using the standard JAXP-configured XML parser.
   */
  public Document loadDocument(Supplier<SAXParserFactory> saxParserFactorySupplier, InputSource inputSource,
                               EntityResolver entityResolver, ErrorHandler errorHandler,
                               int validationMode, boolean namespaceAware, XMLGrammarPool xmlGrammarPool)
      throws Exception {
    final Thread thread = currentThread();
    final ClassLoader currentClassLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(MuleDocumentLoader.class.getClassLoader());

      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try (InputStream inputStream = inputSource.getByteStream()) {
        IOUtils.copy(inputStream, output);
      }

      InputSource defaultInputSource = new InputSource(new ByteArrayInputStream(output.toByteArray()));
      InputSource enrichInputSource = new InputSource(new ByteArrayInputStream(output.toByteArray()));

      DocumentBuilderFactory factory = this.createDocumentBuilderFactory(validationMode, namespaceAware, xmlGrammarPool);
      DocumentBuilder builder = this.createDocumentBuilder(factory, entityResolver, errorHandler);
      Document doc = builder.parse(defaultInputSource);
      createSaxAnnotator(saxParserFactorySupplier, doc).parse(enrichInputSource);

      return doc;
    } finally {
      thread.setContextClassLoader(currentClassLoader);
    }
  }

  protected XMLReader createSaxAnnotator(Supplier<SAXParserFactory> saxParserFactorySupplier, Document doc)
      throws ParserConfigurationException, SAXException {
    SAXParserFactory saxParserFactory = saxParserFactorySupplier.get();
    SAXParser saxParser = saxParserFactory.newSAXParser();
    XMLReader documentReader = saxParser.getXMLReader();
    documentReader.setFeature(SCHEMA_AUGMENT_PSVI_FEATURE, false);
    documentReader.setContentHandler(new XmlMetadataAnnotator(doc, metadataFactory));
    return documentReader;
  }

  protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware,
                                                                XMLGrammarPool grammarPool)
      throws ParserConfigurationException {
    DocumentBuilderFactory factory;
    // Sure we are using standard Java implementations
    factory = DocumentBuilderFactory.newInstance(MULE_DOCUMENT_BUILDER_FACTORY, MuleDocumentLoader.class.getClassLoader());
    // Disable external entities
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

    factory.setFeature(SCHEMA_AUGMENT_PSVI_FEATURE, false);
    if (grammarPool != null) {
      factory.setAttribute(XMLGRAMMAR_POOL, grammarPool);
    }
    factory.setNamespaceAware(namespaceAware);
    factory.setValidating(isValidationEnabled(validationMode));
    if (validationMode == 3) {
      factory.setNamespaceAware(true);

      try {
        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
      } catch (IllegalArgumentException var6) {
        ParserConfigurationException pcex =
            new ParserConfigurationException("Unable to validate using XSD: Your JAXP provider [" + factory
                + "] does not support XML Schema. Are you running on Java 1.4 with Apache Crimson? Upgrade to Apache Xerces (or Java 1.5) for full XSD support.");
        pcex.initCause(var6);
        throw pcex;
      }
    }

    return factory;
  }

  private boolean isValidationEnabled(int validationMode) {
    return validationMode != 0;
  }

  private final class DefaultXmlMetadataFactory implements XmlMetadataAnnotationsFactory {

    @Override
    public XmlMetadataAnnotations create(Locator locator) {
      return new DefaultXmlMetadataAnnotations();
    }

  }

  protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory, EntityResolver entityResolver,
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

  /**
   * SAX filter that builds the metadata that will annotate the built nodes.
   */
  public final static class XmlMetadataAnnotator extends DefaultHandler {

    private Locator locator;
    private DomWalkerElement walker;
    private final XmlMetadataAnnotationsFactory metadataFactory;
    private final Deque<XmlMetadataAnnotations> annotationsStack = new ArrayDeque<>();
    private SourcePosition trackingPoint = new SourcePosition();
    private boolean writingBody = false;

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
      writingBody = false;
      walker = walker.walkIn();

      XmlMetadataAnnotations metadataBuilder = metadataFactory.create(locator);
      metadataBuilder.setLineNumber(locator.getLineNumber());
      metadataBuilder.setColumnNumber(trackingPoint.getColumn());
      LinkedHashMap<String, String> attsMap = new LinkedHashMap<>();
      for (int i = 0; i < atts.getLength(); ++i) {
        attsMap.put(atts.getQName(i), atts.getValue(i));
      }
      metadataBuilder.appendElementStart(qName, attsMap);
      annotationsStack.push(metadataBuilder);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      // update the starting point
      this.updateTrackingPoint(start > 0);

      final String body = new String(ch, start, length).trim();

      if (!isEmpty(body)) {
        if (!writingBody) {
          annotationsStack.peek()
              .appendElementBody("<![CDATA[" + lineSeparator());
        }

        annotationsStack.peek()
            .appendElementBody(body);
        writingBody = true;
      }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
      this.updateTrackingPoint(false);// update the starting point
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (writingBody) {
        annotationsStack.peek()
            .appendElementBody(lineSeparator() + "]]>");
      }
      writingBody = false;
      XmlMetadataAnnotations metadataAnnotations = annotationsStack.pop();
      metadataAnnotations.appendElementEnd(qName);

      if (!annotationsStack.isEmpty()) {
        XmlMetadataAnnotations xmlMetadataAnnotations = annotationsStack.peek();

        xmlMetadataAnnotations
            .appendElementBody(lineSeparator() + metadataAnnotations.getElementString() + lineSeparator());
      }

      walker.getParentNode().setUserData(METADATA_ANNOTATIONS_KEY, metadataAnnotations, COPY_METADATA_ANNOTATIONS_DATA_HANDLER);
      walker = walker.walkOut();

      // update the starting point for the next tag
      this.updateTrackingPoint(false);
    }

    private void updateTrackingPoint(boolean columnOff) {
      SourcePosition item = new SourcePosition(locator.getLineNumber(), locator.getColumnNumber() - (columnOff ? 1 : 0));
      if (this.trackingPoint.compareTo(item) < 0) {
        this.trackingPoint = item;
      }
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
}
