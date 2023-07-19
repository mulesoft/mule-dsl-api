/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.xml.parser;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;

/**
 * Represents a simple configuration attribute.
 *
 * @since 4.0
 */
@NoExtend
@NoInstantiate
public final class SimpleConfigAttribute {

  private final String name;
  private final String value;
  private final boolean valueFromSchema;

  /**
   * @param name            configuration attribute name as it appears in the configuration file.
   * @param value           configuration value as defined in the configuration file.
   * @param valueFromSchema true if the configuration value was not explicitly defined by the user and was retrieved from the DSL
   *                        schema, false otherwise.
   */
  public SimpleConfigAttribute(String name, String value, boolean valueFromSchema) {
    this.name = name;
    this.value = value;
    this.valueFromSchema = valueFromSchema;
  }

  /**
   * @return the configuration attribute name as it appears in the configuration file.
   */
  public String getName() {
    return name;
  }

  /**
   * @return configuration value as defined in the configuration file.
   */
  public String getValue() {
    return value;
  }

  /**
   * @return true if the value came from the DSL schema, false if the value comes from explicit user configuration.
   */
  public boolean isValueFromSchema() {
    return valueFromSchema;
  }
}
