/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.component.config;

import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

public class InternalComponentConfiguration extends ComponentConfiguration {

  public InternalComponentConfiguration() {}

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InternalComponentConfiguration that = (InternalComponentConfiguration) o;

    if (!getIdentifier().equals(that.getIdentifier())) {
      return false;
    }
    if (getComponentLocation() != null ? !getComponentLocation().equals(that.getComponentLocation())
        : that.getComponentLocation() != null) {
      return false;
    }
    if (!properties.equals(that.properties)) {
      return false;
    }
    if (!getParameters().equals(that.getParameters())) {
      return false;
    }
    if (!getNestedComponents().equals(that.getNestedComponents())) {
      return false;
    }
    return getValue() != null ? getValue().equals(that.getValue()) : that.getValue() == null;
  }

  @Override
  public int hashCode() {
    int result = getIdentifier().hashCode();
    result = 31 * result + (getComponentLocation() != null ? getComponentLocation().hashCode() : 0);
    result = 31 * result + properties.hashCode();
    result = 31 * result + getParameters().hashCode();
    result = 31 * result + getNestedComponents().hashCode();
    result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
    return result;
  }

}
