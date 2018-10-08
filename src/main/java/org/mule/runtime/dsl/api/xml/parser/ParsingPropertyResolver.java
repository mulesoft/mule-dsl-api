/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml.parser;

/**
 * Resolve values for XML properties.
 * <p/>
 * Properties have the following syntax: ${PROPERTY_KEY} where the property key is the unique identifier for the property.
 * <p/>
 * During processing of XML configuration files not all properties will be resolved. Only the ones required to fully process the
 * XML configuration files. For instance the properties used within the <import/> element.
 * 
 * @since 4.2
 */
public interface ParsingPropertyResolver {

  /**
   * Resolver the value for a property key.
   * 
   * @param propertyKey property key
   * @return the property value
   */
  String resolveProperty(String propertyKey);

}
