/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
