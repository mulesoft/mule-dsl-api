/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xml.parser;

import org.mule.api.annotation.NoImplement;

import java.util.Map;

/**
 * Stores the metadata annotations from the XML parser so they are available when building the actual objects of the application.
 */
@NoImplement
public interface XmlMetadataAnnotations {

  interface TagBoundaries {

    /**
     * @return the line where the declaration of the tag starts in its source xml file.
     */
    int getStartLineNumber();

    /**
     * @param lineNumber the line where the declaration of the tag starts in its source xml file.
     */
    void setStartLineNumber(int lineNumber);

    /**
     * @return the column where the declaration of the tag starts in the source xml file.
     */
    int getStartColumnNumber();

    /**
     * @param columnNumber the column where the declaration of the tag starts in the source xml file.
     */
    void setStartColumnNumber(int columnNumber);

    /**
     * @return the line where the declaration of the tag ends in its source xml file.
     */
    int getEndLineNumber();

    /**
     * @param lineNumber the line where the declaration of the tag ends in its source xml file.
     */
    void setEndLineNumber(int lineNumber);

    /**
     * @return the column where the declaration of the tag ends in the source xml file.
     */
    int getEndColumnNumber();

    /**
     * @param columnNumber the column where the declaration of the tag ends in the source xml file.
     */
    void setEndColumnNumber(int columnNumber);
  }

  String METADATA_ANNOTATIONS_KEY = "metadataAnnotations";

  /**
   * Builds the opening tag of the xml element.
   * 
   * @param qName the qualified name of the element
   * @param atts  the attributes of the element, with the qualified name as key
   */
  void appendElementStart(String qName, Map<String, String> atts);

  /**
   * Adds the body of the xml tag.
   * 
   * @param elementBody the body content to be added
   */
  void appendElementBody(String elementBody);

  /**
   * Builds the closing tag of the xml element.
   * 
   * @param qName the qualified name of the element
   */
  void appendElementEnd(String qName);

  /**
   * @return the reconstruction of the declaration of the element in its source xml file.
   *         <p/>
   *         Note that the order of the elements may be different, and any implicit attributes with default values will be
   *         included.
   */
  String getElementString();

  /**
   * @return Whether the element was written as in {@code <element />}. In such case, the opening and closing tag boundaries will
   *         be the same.
   */
  boolean isSelfClosing();

  /**
   * @return the boundaries of the opening tag on the source xml file.
   */
  TagBoundaries getOpeningTagBoundaries();

  /**
   * @return the boundaries of the closing tag on the source xml file.
   */
  TagBoundaries getClosingTagBoundaries();
}
