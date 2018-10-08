/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xml.parser;


import static org.mule.runtime.dsl.internal.xml.parser.XmlApplicationParser.DECLARED_PREFIX;
import static org.mule.runtime.dsl.internal.xml.parser.XmlApplicationParser.NAMESPACE_URI;
import static org.mule.runtime.dsl.internal.xml.parser.XmlApplicationParser.XML_NODE;
import org.mule.runtime.dsl.api.xml.parser.ConfigLine;

import org.w3c.dom.Node;

/**
 * Handler for adding and removing custom XML attributes from and to {@code ConfigLine} and {@code ComponentModel}.
 *
 * @since 4.0
 */
public class XmlCustomAttributeHandler {

  /**
   * @param builder builder which is going to be used to create the {@code org.mule.runtime.config.dsl.processor.ConfigLine}.
   * @return handler for adding custom attributes to the builder.
   */
  public static ConfigLineCustomAttributeStore to(ConfigLine.Builder builder) {
    return new ConfigLineCustomAttributeStore(builder);
  }

  /**
   * @param configLine line from which the custom attribute must be retrieved.
   * @return a handler for retrieving custom attributes.
   */
  public static ConfigLineCustomAttributeRetrieve from(ConfigLine configLine) {
    return new ConfigLineCustomAttributeRetrieve(configLine);
  }


  public static class ConfigLineCustomAttributeStore {

    private final ConfigLine.Builder builder;

    private ConfigLineCustomAttributeStore(ConfigLine.Builder builder) {
      this.builder = builder;
    }

    public ConfigLineCustomAttributeStore addNode(Node node) {
      addCustomAttributes(node);
      return this;
    }

    private void addCustomAttributes(Node node) {
      this.builder.addCustomAttribute(XML_NODE, node);
      this.builder.addCustomAttribute(NAMESPACE_URI, node.getNamespaceURI());
      if (node.getPrefix() != null) {
        this.builder.addCustomAttribute(DECLARED_PREFIX, node.getPrefix());
      }
    }
  }

  public static class ConfigLineCustomAttributeRetrieve {

    private final ConfigLine configLine;

    private ConfigLineCustomAttributeRetrieve(ConfigLine configLine) {
      this.configLine = configLine;
    }

    public Node getNode() {
      return (Node) this.configLine.getCustomAttributes().get(XML_NODE);
    }

  }

}
