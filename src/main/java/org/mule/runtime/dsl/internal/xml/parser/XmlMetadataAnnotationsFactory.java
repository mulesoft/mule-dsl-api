/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xml.parser;

import org.xml.sax.Locator;

/**
 * Factory object to get a fresh instance of a {@link XmlMetadataAnnotations}.
 *
 * This interface is an extension point to other Mule projects. Please be careful if you are going to make changes here.
 */
public interface XmlMetadataAnnotationsFactory {

  /**
   * @param locator the xml parser context.
   * @return a fresh {@link XmlMetadataAnnotations}
   */
  XmlMetadataAnnotations create(Locator locator);

}
