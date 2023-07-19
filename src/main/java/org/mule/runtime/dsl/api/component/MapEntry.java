/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.component;

import org.mule.runtime.api.component.AbstractComponent;

/**
 * Instances of this classes represent a map entry defined in the configuration.
 *
 * It's possible to create map instances from a set of entries or receive a list of entries for any custom map processing.
 *
 * @since 4.0
 *
 * @param <KeyType>   the key type
 * @param <ValueType> the value type
 */
public final class MapEntry<KeyType, ValueType> extends AbstractComponent {

  private final KeyType key;
  private final ValueType value;

  public MapEntry(KeyType key, ValueType value) {
    this.key = key;
    this.value = value;
  }

  /**
   * @return the entry key
   */
  public KeyType getKey() {
    return key;
  }

  /**
   * @return the entry value
   */
  public ValueType getValue() {
    return value;
  }
}
