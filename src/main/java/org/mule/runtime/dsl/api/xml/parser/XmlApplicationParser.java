/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.xml.parser;

import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.DOMAIN_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.DOMAIN_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EE_DOMAIN_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.EE_DOMAIN_PREFIX;

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
