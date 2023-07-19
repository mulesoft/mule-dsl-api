/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.component;

import org.mule.api.annotation.NoImplement;

/**
 * Interface that can implement instances of {@link org.mule.runtime.api.ioc.ObjectProvider} in case the type of the object
 * created is dynamic and cannot be known at compile time.
 * 
 * @since 4.0
 */
@NoImplement
public interface ObjectTypeProvider {

  /**
   * @return the type of the object created.
   */
  Class<?> getObjectType();

}
