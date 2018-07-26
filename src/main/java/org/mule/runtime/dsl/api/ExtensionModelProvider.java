/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api;

import java.util.Optional;
import java.util.Set;

import org.mule.runtime.api.meta.model.ExtensionModel;

/**
 * Provider of {@link ExtensionModel}s.
 * <p/>
 * This abstraction allows decoupling the mechanism to get load extension models so there may be different implementation. For
 * instance, for tooling purposes an implementation may load the extension model from a serialized version from disk or from
 * in-memory from a cache. The runtime may decide to create the whole extension model from scratch based on the mule plugin.
 *
 * @since 1.2.0
 */
public interface ExtensionModelProvider {

  /**
   * @param extensionNamespace the extension model namespace.
   * 
   * @return the extension model or empty if it couldn't be found based on the context.
   */
  Optional<ExtensionModel> getExtensionModel(String extensionNamespace);

  /**
   * @return the set of extension models available.
   */
  Set<ExtensionModel> getExtensionModels();

}
