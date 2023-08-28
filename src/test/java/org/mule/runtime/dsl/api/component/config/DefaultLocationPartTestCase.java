/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component.config;

import static java.util.Optional.empty;
import static java.util.OptionalInt.of;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DefaultLocationPartTestCase {

  private DefaultLocationPart locationPart;

  @Before
  public void setup() {
    locationPart =
        new DefaultLocationPart("part/path", empty(), empty(), of(13), of(14));
  }

  @Test
  @Issue("W-10790023")
  public void serializedAndDeserializedPartPreservesHashAndEquality() throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(locationPart);
    }

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    try (ObjectInputStream ois = new ObjectInputStream(bais)) {
      Object deserialized = ois.readObject();
      assertThat(deserialized, instanceOf(DefaultLocationPart.class));

      assertThat(deserialized.hashCode(), is(locationPart.hashCode()));
      assertThat(deserialized, is(locationPart));
    }
  }
}
