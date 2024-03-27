/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml.parser;

import static java.lang.String.format;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Node;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;

/**
 * Simple parser that allows to obtain the required data from an XML document.
 * <p>
 * It uses the SPI interface {@link XmlNamespaceInfoProvider} to locate for all namespace info provided and normalize the
 * namespace from the XML document.
 *
 * @since 4.0
 */
public class XmlApplicationParser {

  private static final String DEFAULT_NAMESPACE_URI_MASK = "http://www.mulesoft.org/schema/mule/%s";
  static final String CORE_NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, "core");
  private static final String CORE_PREFIX = "mule";
  private static final String DOMAIN_NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, "domain");
  private static final String DOMAIN_PREFIX = "domain";
  private static final String EE_DOMAIN_NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, "ee/domain");
  private static final String EE_DOMAIN_PREFIX = "ee-domain";

  public static final String DECLARED_PREFIX = "DECLARED_PREFIX";
  public static final String XML_NODE = "XML_NODE";
  public static final String LINE_NUMBER = "LINE_NUMBER";
  public static final String CONFIG_FILE_NAME = "CONFIG_FILE_NAME";
  public static final String IS_CDATA = "IS_CDATA";

  private static final String COLON = ":";
  private static final Map<String, String> predefinedNamespace = new HashMap<>();
  private static final String UNDEFINED_NAMESPACE = "undefined";
  private final List<XmlNamespaceInfoProvider> namespaceInfoProviders;
  private final Cache<String, String> namespaceCache;

  static {
    predefinedNamespace.put(DOMAIN_NAMESPACE, DOMAIN_PREFIX);
    predefinedNamespace.put(EE_DOMAIN_NAMESPACE, EE_DOMAIN_PREFIX);
  }

  public XmlApplicationParser(List<XmlNamespaceInfoProvider> namespaceInfoProviders) {
    this.namespaceInfoProviders = ImmutableList.<XmlNamespaceInfoProvider>builder().addAll(namespaceInfoProviders).build();
    this.namespaceCache = CacheBuilder.newBuilder().build();
  }

  private String loadNamespaceFromProviders(String namespaceUri) {
    if (predefinedNamespace.containsKey(namespaceUri)) {
      return predefinedNamespace.get(namespaceUri);
    }
    for (XmlNamespaceInfoProvider namespaceInfoProvider : namespaceInfoProviders) {
      Optional<XmlNamespaceInfo> matchingXmlNamespaceInfo = namespaceInfoProvider.getXmlNamespacesInfo().stream()
          .filter(xmlNamespaceInfo -> namespaceUri.equals(xmlNamespaceInfo.getNamespaceUriPrefix())).findFirst();
      if (matchingXmlNamespaceInfo.isPresent()) {
        return matchingXmlNamespaceInfo.get().getNamespace();
      }
    }
    // TODO MULE-9638 for now since just return a fake value since guava cache does not support null values. When done right throw
    // a configuration exception with a meaningful message if there's no info provider defined
    return UNDEFINED_NAMESPACE;
  }

  public String getNormalizedNamespace(String namespaceUri, String namespacePrefix) {
    try {
      return namespaceCache.get(namespaceUri, () -> {
        String namespace = loadNamespaceFromProviders(namespaceUri);
        if (namespace == null) {
          namespace = namespacePrefix;
        }
        return namespace;
      });
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  public String parseNamespace(Node node) {
    String namespace = CORE_PREFIX;
    if (node.getNodeType() != Node.CDATA_SECTION_NODE) {
      namespace = getNormalizedNamespace(node.getNamespaceURI(), node.getPrefix());
      if (namespace.equals(UNDEFINED_NAMESPACE)) {
        namespace = node.getPrefix();
      }
    }
    return namespace;
  }

  public String parseNamespaceUri(Node node) {
    String namespace = CORE_NAMESPACE;
    if (node.getNodeType() != Node.CDATA_SECTION_NODE) {
      namespace = node.getNamespaceURI();
    }
    return namespace;
  }

  public String parseIdentifier(Node node) {
    String identifier = node.getNodeName();
    String[] nameParts = identifier.split(COLON);
    if (nameParts.length > 1) {
      identifier = nameParts[1];
    }
    return identifier;
  }

  public boolean isValidType(Node node) {
    return node.getNodeType() != Node.TEXT_NODE && node.getNodeType() != Node.COMMENT_NODE;
  }

  public static boolean isTextContent(Node node) {
    return node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE;
  }

}
