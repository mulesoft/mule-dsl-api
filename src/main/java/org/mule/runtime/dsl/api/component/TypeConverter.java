/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.component;

/**
 * Converter from one type to another. Meant to be used for converting plain values from the mule configuration to specific types
 * required by a runtime object.
 *
 * @param <InputType>  type of the value to be converted.
 * @param <OutputType> type of the converted value.
 */
public interface TypeConverter<InputType, OutputType> {

  /**
   * Converters from one type to another.
   *
   * @param inputType the value to be converted.
   * @return the converted value.
   */
  OutputType convert(InputType inputType);

}
