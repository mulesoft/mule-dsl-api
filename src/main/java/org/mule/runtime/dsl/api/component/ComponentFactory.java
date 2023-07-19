/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.component;

import org.mule.runtime.api.component.Component;

/**
 * Interface that must be implemented by those classes that are meant to be used as a factory to create complex domain objects
 * which in turn are {@link Component}s. Implementations should extend {@link AbstractComponentFactory}.
 *
 * @param <T> the type of the object to be created, which should be an {@link Component}.
 */
public interface ComponentFactory<T> extends ObjectFactory<T>, Component {

}
