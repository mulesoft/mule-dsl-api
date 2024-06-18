/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component.config;

import static org.mule.runtime.api.util.classloader.MuleImplementationLoaderUtils.getMuleImplementationsLoader;

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
                                                                              getMuleImplementationsLoader())
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
