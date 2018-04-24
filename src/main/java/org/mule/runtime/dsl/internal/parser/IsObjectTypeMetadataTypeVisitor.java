/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.parser;

import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;

public class IsObjectTypeMetadataTypeVisitor extends MetadataTypeVisitor {

  private boolean objectType = false;

  @Override
  public void visitObject(ObjectType objectType) {
    this.objectType = true;
  }

  public boolean isObjectType() {
    return objectType;
  }
}
