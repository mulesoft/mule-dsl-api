/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component.config;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.dsl.internal.component.config.InternalComponentConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Defines a mule component configuration content.
 * <p>
 * A {@code ComponentConfiguration} allows to define a mule component configuration programmatically by defining the component
 * namespace, name and the set of simple attributes or complex nested attributes required by the component.
 *
 * @since 4.0
 *
 * @deprecated Use {@code org.mule.runtime.ast.api.ComponentAst} from {@code mule-artifact-ast} instead. This will be removed in
 *             4.4.0.
 */
@Deprecated
public abstract class ComponentConfiguration {

  protected ComponentIdentifier identifier;
  protected ComponentLocation componentLocation;
  protected Map<String, Object> properties = new HashMap<>();
  protected Map<String, String> parameters = new HashMap<>();
  protected List<ComponentConfiguration> nestedComponents = new ArrayList<>();
  protected String value;

  /**
   * @return the configuration identifier.
   */
  public ComponentIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * @return the location of the component in the configuration
   */
  public ComponentLocation getComponentLocation() {
    return componentLocation;
  }

  /**
   * @return a map with the configuration parameters of the component where the key is the parameter name and the value is the
   *         parameter value.
   */
  public Map<String, String> getParameters() {
    return unmodifiableMap(parameters);
  }

  /**
   * @return content of the configuration element.
   */
  public Optional<String> getValue() {
    return Optional.ofNullable(value);
  }


  /**
   * @param name the name of the property
   * @return the property for the given name, or {@link Optional#empty()} if none was found.
   */
  public Optional<Object> getProperty(String name) {
    return Optional.ofNullable(properties.get(name));
  }

  /**
   * @return a collection of the complex child configuration components.
   */
  public List<ComponentConfiguration> getNestedComponents() {
    return unmodifiableList(nestedComponents);
  }

  protected ComponentConfiguration() {}

  /**
   * @return a new {@link Builder}
   * @since 1.8
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating {@code ComponentConfiguration} instances.
   */
  public static class Builder {

    private final InternalComponentConfiguration componentConfiguration = new InternalComponentConfiguration();

    /**
     * @return the location of the component in the configuration.
     */
    public ComponentConfiguration getComponentConfiguration() {
      return componentConfiguration;
    }

    private Builder() {}

    /**
     * @param identifier identifier for the configuration element this object represents.
     * @return the builder.
     */
    public Builder withIdentifier(ComponentIdentifier identifier) {
      componentConfiguration.identifier = identifier;
      return this;
    }

    /**
     * Adds a configuration parameter to the component
     *
     * @param name  configuration attribute name
     * @param value configuration attribute value
     * @return the builder
     */
    public Builder withParameter(String name, String value) {
      componentConfiguration.parameters.put(name, value);
      return this;
    }

    /**
     * Sets the inner content of the configuration element.
     *
     * @param textContent inner text content from the configuration
     * @return the builder
     */
    public Builder withValue(String textContent) {
      componentConfiguration.value = textContent;
      return this;
    }

    /**
     * Adds a property to the {@link ComponentConfiguration}. This property is meant to hold only metadata of the configuration.
     *
     * @param name  custom attribute name.
     * @param value custom attribute value.
     * @return the builder.
     */
    public Builder withProperty(String name, Object value) {
      componentConfiguration.properties.put(name, value);
      return this;
    }

    /**
     * @param componentLocation the location of the component in the configuration.
     * @return the builder.
     */
    public Builder withComponentLocation(ComponentLocation componentLocation) {
      componentConfiguration.componentLocation = componentLocation;
      return this;
    }

    /**
     * Adds a complex configuration parameter to the component.
     * <p>
     * For instance, to define a file:matcher for a file:read component: *
     *
     * <pre>
     * {@code
     * <file:read>
     *   <file:matcher regex="XYZ"/>
     * </file:read>
     * }
     * </pre>
     *
     * @param nestedComponent the {@link ComponentConfiguration} that represents the nested configuration
     * @return {@code this} {@link Builder}
     */
    public Builder withNestedComponent(ComponentConfiguration nestedComponent) {
      componentConfiguration.nestedComponents.add(nestedComponent);
      return this;
    }

    /**
     * @return a {@code ComponentConfiguration} with the provided configuration
     */
    public ComponentConfiguration build() {
      return componentConfiguration;
    }
  }

}
