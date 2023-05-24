/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.component.config;

import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;

import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.util.stream.Stream;

/**
 * Provides utilities for the Runtime to discover {@link ComponentBuildingDefinitionProvider} implementations.
 * 
 * @since 1.6.0
 */
public final class ComponentBuildingDefinitionProviderUtils {

  private ComponentBuildingDefinitionProviderUtils() {
    // nothing to do
  }

  /**
   * Looks up implementations of {@link ComponentBuildingDefinitionProvider} with the provided classloader.
   * 
   * @param classLoader the classlaoder to use for loading the services through SPI.
   * @return the discovered {@link ComponentBuildingDefinitionProvider}.
   */
  public static final Stream<ComponentBuildingDefinitionProvider> lookupComponentBuildingDefinitionProviders(ClassLoader classLoader) {
    return stream(((Iterable<ComponentBuildingDefinitionProvider>) () -> load(ComponentBuildingDefinitionProvider.class,
                                                                              classLoader)
                                                                                  .iterator())
                                                                                      .spliterator(),
                  false);
  }
}