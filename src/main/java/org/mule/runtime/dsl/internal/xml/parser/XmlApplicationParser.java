/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.dsl.internal.xml.parser;

import static java.util.Optional.empty;

import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;
import org.mule.runtime.dsl.api.xml.parser.ConfigLine;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple parser that allows to obtain the required data from an XML document.
 * <p>
 * It uses the SPI interface {@link XmlNamespaceInfoProvider} to locate for all namespace info provided and normalize the
 * namespace from the XML document.
 *
 * @since 4.0
 */
public final class XmlApplicationParser {

  public static final String DECLARED_PREFIX = org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.DECLARED_PREFIX;
  public static final String XML_NODE = org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.XML_NODE;
  public static final String LINE_NUMBER = org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.LINE_NUMBER;
  public static final String CONFIG_FILE_NAME = org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.CONFIG_FILE_NAME;
  public static final String IS_CDATA = org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.IS_CDATA;

  private final org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser parser;

  public XmlApplicationParser(List<XmlNamespaceInfoProvider> namespaceInfoProviders) {
    parser = new org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser(namespaceInfoProviders);
  }

  public String getNormalizedNamespace(String namespaceUri, String namespacePrefix) {
    return parser.getNormalizedNamespace(namespaceUri, namespacePrefix);
  }

  /**
   * @deprecated From 4.4 onwards, use the {@code mule-artifact-ast-xml-parser} module.
   */
  @Deprecated
  public Optional<ConfigLine> parse(Element configElement) {
    return configLineFromElement(configElement);
  }

  /**
   * @deprecated From 4.4 onwards, use the {@code mule-artifact-ast-xml-parser} module.
   */
  @Deprecated
  private Optional<ConfigLine> configLineFromElement(Node node) {
    if (!isValidType(node)) {
      return empty();
    }

    String identifier = parseIdentifier(node);
    String namespace = parseNamespace(node);
    String namespaceUri = parseNamespaceUri(node);

    ConfigLine.Builder builder = new ConfigLine.Builder()
        .setIdentifier(identifier)
        .setNamespace(namespace)
        .setNamespaceUri(namespaceUri);

    XmlMetadataAnnotations userData = (XmlMetadataAnnotations) node.getUserData(XmlMetadataAnnotations.METADATA_ANNOTATIONS_KEY);
    int lineNumber = userData.getLineNumber();
    builder.setLineNumber(lineNumber).setStartColumn(userData.getColumnNumber());
    builder.setSourceCode(userData.getElementString());

    XmlCustomAttributeHandler.to(builder).addCustomAttributes(node);

    Element element = (Element) node;
    NamedNodeMap attributes = element.getAttributes();
    if (element.hasAttributes()) {
      for (int i = 0; i < attributes.getLength(); i++) {
        Node attribute = attributes.item(i);
        Attr attributeNode = element.getAttributeNode(attribute.getNodeName());
        boolean isFromXsd = !attributeNode.getSpecified();
        builder.addConfigAttribute(attribute.getNodeName(), attribute.getNodeValue(), isFromXsd);
      }
    }
    if (node.hasChildNodes()) {
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (isTextContent(child)) {
          builder.setTextContent(child.getNodeValue());
          if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
            builder.addCustomAttribute(IS_CDATA, Boolean.TRUE);
            break;
          }
        } else {
          configLineFromElement(child).ifPresent(builder::addChild);
        }
      }
    }
    return Optional.of(builder.build());
  }

  public String parseNamespace(Node node) {
    return parser.parseNamespace(node);
  }

  public String parseNamespaceUri(Node node) {
    return parser.parseNamespaceUri(node);
  }

  public String parseIdentifier(Node node) {
    return parser.parseIdentifier(node);
  }

  private boolean isValidType(Node node) {
    return parser.isValidType(node);
  }

  public boolean isTextContent(Node node) {
    return org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.isTextContent(node);
  }

}
