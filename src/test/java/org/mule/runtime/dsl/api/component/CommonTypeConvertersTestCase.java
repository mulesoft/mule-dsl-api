/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.api.component;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.dsl.api.component.CommonTypeConverters.stringToEnumConverter;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class CommonTypeConvertersTestCase {

  private enum TestEnum {
    ENUM1, ENUM2
  }

  @Test
  public void stringToEnumDifferentTypes() {
    assertThat(stringToEnumConverter(TimeUnit.class).convert("SECONDS"), is(SECONDS));
    assertThat(stringToEnumConverter(TestEnum.class).convert("ENUM2"), is(TestEnum.ENUM2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullEnumClassConverter() {
    stringToEnumConverter(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullToEnum() {
    stringToEnumConverter(TestEnum.class).convert(null);
  }

}
