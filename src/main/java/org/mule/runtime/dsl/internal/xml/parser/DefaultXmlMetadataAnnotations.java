/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xml.parser;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores the metadata annotations from the XML parser so they are available when building the actual objects of the application.
 */
public class DefaultXmlMetadataAnnotations implements XmlMetadataAnnotations {

  /**
   * compact whitespaces and line breaks
   */
  private static final Pattern COMPACT_PATTERN = compile(">\\s+<+");
  public static final String METADATA_ANNOTATIONS_KEY = "metadataAnnotations";

  private static final Pattern URL_PATTERN = compile("url=\"[a-z]*://([^@]*)@");
  private static final Pattern ADDRESS_PATTERN = compile("address=\"[a-z]*://([^@]*)@");
  private static final Pattern PASSWORD_PATTERN = compile("password=\"([^\"|\n]*)\"");
  private static final String PASSWORD_MASK = "@@credentials@@";
  private static final String PASSWORD_ATTRIBUTE_MASK = "password=\"%s\"";

  private final StringBuilder xmlContent = new StringBuilder();
  private int lineNumber;
  private int columnNumber;

  /**
   * Builds the opening tag of the xml element.
   *
   * @param qName the qualified name of the element
   * @param atts  the attributes of the element, with the qualified name as key
   */
  @Override
  public void appendElementStart(String qName, Map<String, String> atts) {
    xmlContent.append("<" + qName);
    for (Entry<String, String> entry : atts.entrySet()) {
      xmlContent.append(maskPasswords(" " + entry.getKey() + "=\"" + entry.getValue() + "\""));
    }
    xmlContent.append(">");
  }

  /**
   * Adds the body of the xml tag.
   *
   * @param elementBody the body content to be added
   */
  @Override
  public void appendElementBody(String elementBody) {
    xmlContent.append(elementBody);
  }

  /**
   * Builds the closing tag of the xml element.
   *
   * @param qName the qualified name of the element
   */
  @Override
  public void appendElementEnd(String qName) {
    xmlContent.append("</" + qName + ">");
  }

  /**
   * @return the reconstruction of the declaration of the element in its source xml file.
   *         <p/>
   *         Note that the order of the elements may be different, and any implicit attributes with default values will be
   *         included.
   */
  @Override
  public String getElementString() {
    return COMPACT_PATTERN.matcher(xmlContent.toString().trim()).replaceAll(">" + lineSeparator() + "<");
  }

  /**
   * @param lineNumber the line where the declaration of the element starts in its source xml file.
   */
  @Override
  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  /**
   * @return the line where the declaration of the element starts in its source xml file.
   */
  @Override
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * @param columnNumber the column where the declaration of the element starts in the source xml file.
   */
  @Override
  public void setColumnNumber(int columnNumber) {
    this.columnNumber = columnNumber;
  }

  /**
   * @return the column where the declaration of the element starts in the source xml file.
   */
  @Override
  public int getColumnNumber() {
    return columnNumber;
  }

  private static String maskPasswords(String xml, String passwordMask) {
    xml = maskUrlPassword(xml, URL_PATTERN, passwordMask);
    xml = maskUrlPassword(xml, ADDRESS_PATTERN, passwordMask);

    Matcher matcher = PASSWORD_PATTERN.matcher(xml);
    if (matcher.find() && matcher.groupCount() > 0) {
      xml = xml.replaceAll(quote(maskPasswordAttribute(matcher.group(1))), maskPasswordAttribute(passwordMask));
    }
    xml = maskUrlPassword(xml, PASSWORD_PATTERN, passwordMask);

    return xml;
  }

  private static String maskPasswords(String xml) {
    return maskPasswords(xml, PASSWORD_MASK);
  }

  private static String maskUrlPassword(String xml, Pattern pattern, String passwordMask) {
    Matcher matcher = pattern.matcher(xml);
    if (matcher.find() && matcher.groupCount() > 0) {
      xml = xml.replaceAll(quote(matcher.group(1)), passwordMask);
    }
    return xml;
  }

  private static String maskPasswordAttribute(String password) {
    return format(PASSWORD_ATTRIBUTE_MASK, password);
  }
}
