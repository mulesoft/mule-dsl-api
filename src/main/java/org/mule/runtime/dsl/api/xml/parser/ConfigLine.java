/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml.parser;

import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * A configuration line represents the data within a line in a configuration file
 *
 * @since 4.0
 *
 * @deprecated From 4.4 onwards, use the {@code mule-artifact-ast} module.
 */
@NoExtend
@NoInstantiate
@Deprecated
public final class ConfigLine {

  /**
   * Provides access to the parent configuration line of this config line
   */
  private ConfigLineProvider parent;
  /**
   * Prefix of the namespace which defines the config line definition
   */
  private String namespace;
  /**
   * Uri of the namespace which defines the config line definition
   */
  private String namespaceUri;
  /**
   * Identifier of the configuration entry
   */
  private String identifier;

  /**
   * The identifier attributes defined in the configuration
   */
  private final Map<String, SimpleConfigAttribute> configAttributes = new HashMap<>();

  /**
   * Generic set of attributes to be used for custom configuration file formats attributes
   */
  private final Map<String, Object> customAttributes = new HashMap<>();

  /**
   * Config lines embedded inside this config line
   */
  private final List<ConfigLine> childrenConfigLines = new LinkedList<>();

  /**
   * Line number within the config file in which this config was defined.
   */
  private int lineNumber;

  private String textContent;
  private int startColumn;
  private String sourceCode;

  public ConfigLine() {}

  public String getNamespace() {
    return namespace;
  }

  public String getNamespaceUri() {
    return namespaceUri;
  }

  public String getIdentifier() {
    return identifier;
  }

  public Map<String, SimpleConfigAttribute> getConfigAttributes() {
    return ImmutableMap.copyOf(configAttributes);
  }

  public Map<String, Object> getCustomAttributes() {
    return Collections.unmodifiableMap(customAttributes);
  }

  public List<ConfigLine> getChildren() {
    return childrenConfigLines;
  }

  /**
   * @deprecated since 1.4 Use the AST instead to navigate the structure.
   */
  @Deprecated
  public ConfigLine getParent() {
    return parent.getConfigLine();
  }

  public String getTextContent() {
    return textContent;
  }

  /**
   * @return the first line number in which the config line was defined in the configuration file.
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * @return the start column in which the config line was defined in the configuration file.
   */
  public int getStartColumn() {
    return startColumn;
  }

  public String getSourceCode() {
    return sourceCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ConfigLine that = (ConfigLine) o;

    if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null)
      return false;
    if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null)
      return false;
    if (!configAttributes.equals(that.configAttributes))
      return false;
    return childrenConfigLines.equals(that.childrenConfigLines);

  }

  @Override
  public int hashCode() {
    int result = namespace != null ? namespace.hashCode() : 0;
    result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
    result = 31 * result + (configAttributes.hashCode());
    result = 31 * result + (childrenConfigLines.hashCode());
    return result;
  }


  public static class Builder {

    public static final String BUILDER_ALREADY_BUILD_AN_OBJECT_YOU_CANNOT_MODIFY_IT =
        "builder already build an object, you cannot modify it";
    private final ConfigLine configLine = new ConfigLine();
    private boolean alreadyBuild;

    public Builder setNamespace(String namespace) {
      checkState(!alreadyBuild, BUILDER_ALREADY_BUILD_AN_OBJECT_YOU_CANNOT_MODIFY_IT);
      configLine.namespace = namespace;
      return this;
    }

    public Builder setNamespaceUri(String namespaceUri) {
      checkState(!alreadyBuild, BUILDER_ALREADY_BUILD_AN_OBJECT_YOU_CANNOT_MODIFY_IT);
      configLine.namespaceUri = namespaceUri;
      return this;
    }

    public Builder setIdentifier(String operation) {
      checkState(!alreadyBuild, BUILDER_ALREADY_BUILD_AN_OBJECT_YOU_CANNOT_MODIFY_IT);
      configLine.identifier = operation;
      return this;
    }

    public Builder setLineNumber(int lineNumber) {
      checkState(!alreadyBuild, BUILDER_ALREADY_BUILD_AN_OBJECT_YOU_CANNOT_MODIFY_IT);
      configLine.lineNumber = lineNumber;
      return this;
    }

    public Builder setStartColumn(int startColumn) {
      checkState(!alreadyBuild, BUILDER_ALREADY_BUILD_AN_OBJECT_YOU_CANNOT_MODIFY_IT);
      configLine.startColumn = startColumn;
      return this;
    }

    public Builder addConfigAttribute(String name, String value, boolean valueFromSchema) {
      checkState(!alreadyBuild, BUILDER_ALREADY_BUILD_AN_OBJECT_YOU_CANNOT_MODIFY_IT);
      configLine.configAttributes.put(name, new SimpleConfigAttribute(name, value, valueFromSchema));
      return this;
    }

    public Builder addCustomAttribute(String name, Object value) {
      checkState(!alreadyBuild, BUILDER_ALREADY_BUILD_AN_OBJECT_YOU_CANNOT_MODIFY_IT);
      configLine.customAttributes.put(name, value);
      return this;
    }

    public Builder addChild(ConfigLine line) {
      checkState(!alreadyBuild, BUILDER_ALREADY_BUILD_AN_OBJECT_YOU_CANNOT_MODIFY_IT);
      configLine.childrenConfigLines.add(line);
      return this;
    }

    /**
     * @deprecated since 1.4 Use the AST instead to navigate the structure.
     */
    @Deprecated
    public Builder setParent(ConfigLineProvider parent) {
      checkState(!alreadyBuild, BUILDER_ALREADY_BUILD_AN_OBJECT_YOU_CANNOT_MODIFY_IT);
      configLine.parent = parent;
      return this;
    }

    public Builder setTextContent(String textContent) {
      configLine.textContent = textContent;
      return this;
    }

    public Builder setSourceCode(String sourceCode) {
      configLine.sourceCode = sourceCode;
      return this;
    }

    public ConfigLine build() {
      alreadyBuild = true;
      return configLine;
    }
  }

}
