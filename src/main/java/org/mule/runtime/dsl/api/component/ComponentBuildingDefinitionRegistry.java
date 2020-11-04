/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.ComponentIdentifier;

import java.util.Optional;

/**
 * Registry with all {@link ComponentBuildingDefinition} that were discovered.
 *
 * @since 1.4.0
 */
@NoImplement
public interface ComponentBuildingDefinitionRegistry {

  /**
   * Adds a new {@code ComponentBuildingDefinition} to the registry.
   *
   * @param builderDefinition definition to be added in the registry
   */
  public void register(ComponentBuildingDefinition<?> builderDefinition);

  /**
   * Lookups a {@code ComponentBuildingDefinition} for a certain configuration component.
   *
   * @param identifier the component identifier
   * @return the definition to build the component
   */
  public Optional<ComponentBuildingDefinition<?>> getBuildingDefinition(ComponentIdentifier identifier);

  /**
   * Lookups a {@link WrapperElementType} for a certain configuration element.
   *
   * @param identifier the wrapper component identifier
   * @return the element type of the wrapper component
   */
  public Optional<WrapperElementType> getWrappedComponent(ComponentIdentifier identifier);

  /**
   * Types of wrapper elements in the XML config.
   */
  public enum WrapperElementType {
    SINGLE, COLLECTION, MAP
  }

}
