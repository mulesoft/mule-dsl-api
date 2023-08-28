/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xml.parser;


import static org.mule.runtime.api.component.Component.Annotations.NAME_ANNOTATION_KEY;
import static org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.DECLARED_PREFIX;

import org.mule.runtime.dsl.api.xml.parser.ConfigLine;

import javax.xml.namespace.QName;

import org.w3c.dom.Node;

/**
 * Handler for adding and removing custom XML attributes from and to {@code ConfigLine} and {@code ComponentModel}.
 *
 * @since 4.0
 * @deprecated From 4.4 onwards, use the {@code mule-artifact-ast-xml-parser} module.
 */
@Deprecated
public class XmlCustomAttributeHandler {

  /**
   * @param builder builder which is going to be used to create the {@code org.mule.runtime.config.dsl.processor.ConfigLine}.
   * @return handler for adding custom attributes to the builder.
   */
  public static ConfigLineCustomAttributeStore to(ConfigLine.Builder builder) {
    return new ConfigLineCustomAttributeStore(builder);
  }

  public static class ConfigLineCustomAttributeStore {

    private final ConfigLine.Builder builder;

    private ConfigLineCustomAttributeStore(ConfigLine.Builder builder) {
      this.builder = builder;
    }

    public void addCustomAttributes(Node node) {
      Node nameAttribute = node.getAttributes()
          .getNamedItemNS(NAME_ANNOTATION_KEY.getNamespaceURI(), NAME_ANNOTATION_KEY.getLocalPart());
      if (nameAttribute != null) {
        this.builder.addCustomAttribute(NAME_ANNOTATION_KEY.toString(), nameAttribute.getNodeValue());
      }
      if (node.getPrefix() != null) {
        this.builder.addCustomAttribute(DECLARED_PREFIX, node.getPrefix());
      }
      for (int i = 0; i < node.getAttributes().getLength(); i++) {
        Node attributeNode = node.getAttributes().item(i);
        if (attributeNode.getNamespaceURI() != null) {
          this.builder.addCustomAttribute(new QName(attributeNode.getNamespaceURI(), attributeNode.getLocalName()).toString(),
                                          attributeNode.getNodeValue());
        }
      }
    }
  }

}
