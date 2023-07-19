/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.component;

/**
 * Wrapper class for a setter attribute definitions.
 *
 * It contains the attribute name plus the attribute definition
 *
 * @since 4.0
 */
public final class SetterAttributeDefinition {

  private String attributeName;
  private AttributeDefinition attributeDefinition;

  /**
   * @param attributeName       name of the attribute to be set
   * @param attributeDefinition definition of the attribute to be set
   */
  public SetterAttributeDefinition(String attributeName, AttributeDefinition attributeDefinition) {
    this.attributeName = attributeName;
    this.attributeDefinition = attributeDefinition;
  }

  /**
   * @return the object attribute name
   */
  public String getAttributeName() {
    return attributeName;
  }

  /**
   * @return the object attribute definition
   */
  public AttributeDefinition getAttributeDefinition() {
    return attributeDefinition;
  }

}
