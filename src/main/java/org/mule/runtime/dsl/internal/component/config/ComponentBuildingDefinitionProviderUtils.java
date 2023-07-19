/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.component.config;

import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;

import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.util.stream.Stream;

/**
 * Provides utilities for the Runtime to discover {@link ComponentBuildingDefinitionProvider} implementations.
 * 
 * @since 1.5.0
 */
public final class ComponentBuildingDefinitionProviderUtils {

  private ComponentBuildingDefinitionProviderUtils() {
    // nothing to do
  }

  /**
   * Looks up implementations of {@link ComponentBuildingDefinitionProvider} from the Mule container.
   * 
   * @return the discovered {@link ComponentBuildingDefinitionProvider}.
   */
  public static final Stream<ComponentBuildingDefinitionProvider> lookupComponentBuildingDefinitionProviders() {
    return stream(((Iterable<ComponentBuildingDefinitionProvider>) () -> load(ComponentBuildingDefinitionProvider.class,
                                                                              ComponentBuildingDefinitionProvider.class
                                                                                  .getClassLoader())
                                                                                      .iterator())
                                                                                          .spliterator(),
                  false);
  }

  /**
   * Looks up implementations of {@link ComponentBuildingDefinitionProvider} with the provided classloader.
   * 
   * @param classLoader the classloader of a deployable artifact to use for loading the services through SPI.
   * @return the discovered {@link ComponentBuildingDefinitionProvider}.
   */
  public static final Stream<ComponentBuildingDefinitionProvider> lookupComponentBuildingDefinitionProviders(ClassLoader deployableArtifactClassLoader) {
    return stream(((Iterable<ComponentBuildingDefinitionProvider>) () -> load(ComponentBuildingDefinitionProvider.class,
                                                                              deployableArtifactClassLoader)
                                                                                  .iterator())
                                                                                      .spliterator(),
                  false);
  }
}
