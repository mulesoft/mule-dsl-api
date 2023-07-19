/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.xerces.xni.parser;

import org.mule.apache.xerces.xni.parser.XMLErrorHandler;
import org.mule.apache.xerces.xni.parser.XMLParseException;
import org.mule.api.annotation.NoImplement;

import java.util.List;

/**
 * Represents a specific type of {@link XMLErrorHandler} which gathers as many errors as possible to be displayed later for either
 * logging purposes or to propagate an exception with the full list of errors.
 *
 * @since 1.4.0
 */
@NoImplement
public interface XmlGathererErrorHandler extends XMLErrorHandler {

  /**
   * @return a collection with all the {@link XMLParseException} exceptions gathered from
   *         {@link XMLErrorHandler#error(String, String, XMLErrorHandler)}.
   *         <p/>
   *         An empty list means there were no error while parsing the file. Non null.
   */
  List<XMLParseException> getErrors();
}
