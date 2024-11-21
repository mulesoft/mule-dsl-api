/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

  private static final Pattern URL_PATTERN = compile("url=\"[a-z]*://([^@]*)@");
  private static final Pattern ADDRESS_PATTERN = compile("address=\"[a-z]*://([^@]*)@");
  private static final Pattern PASSWORD_PATTERN = compile("password=\"([^\"|\n]*)\"");
  private static final String PASSWORD_MASK = "@@credentials@@";
  private static final String PASSWORD_ATTRIBUTE_MASK = "password=\"%s\"";

  private final StringBuilder xmlContent = new StringBuilder();

  private final TagBoundaries openingTagBoundaries = new DefaultTagBoundaries();
  private final TagBoundaries closingTagBoundaries = new DefaultTagBoundaries();

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
    xmlContent.append(elementBody.trim());
  }

  /**
   * Builds the closing tag of the xml element.
   *
   * @param qName the qualified name of the element
   */
  @Override
  public void appendElementEnd(String qName) {
    xmlContent.append("</" + qName + ">" + lineSeparator());
  }

  /**
   * @return the reconstruction of the declaration of the element in its source xml file.
   *         <p/>
   *         Note that the order of the elements may be different, and any implicit attributes with default values will be
   *         included.
   */
  @Override
  public String getElementString() {
    return xmlContent.toString().trim();
  }

  /**
   * @return Whether the element was written as in {@code <element />} in the source code. In such case, the opening and closing
   *         tag boundaries will be the same, as both tags are merged into one.
   */
  @Override
  public boolean isSelfClosing() {
    return openingTagBoundaries.getStartLineNumber() == closingTagBoundaries.getStartLineNumber() &&
        openingTagBoundaries.getStartColumnNumber() == closingTagBoundaries.getStartColumnNumber();
  }

  /**
   * @return the boundaries of the opening tag on the source xml file.
   */
  @Override
  public TagBoundaries getOpeningTagBoundaries() {
    return openingTagBoundaries;
  }

  /**
   * @return the boundaries of the closing tag on the source xml file.
   */
  @Override
  public TagBoundaries getClosingTagBoundaries() {
    return closingTagBoundaries;
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

  private static class DefaultTagBoundaries implements TagBoundaries {

    private int startLineNumber;
    private int startColumnNumber;
    private int endLineNumber;
    private int endColumnNumber;

    @Override
    public int getStartLineNumber() {
      return startLineNumber;
    }

    @Override
    public void setStartLineNumber(int lineNumber) {
      this.startLineNumber = lineNumber;
    }

    @Override
    public int getStartColumnNumber() {
      return startColumnNumber;
    }

    @Override
    public void setStartColumnNumber(int columnNumber) {
      this.startColumnNumber = columnNumber;
    }

    @Override
    public int getEndLineNumber() {
      return endLineNumber;
    }

    @Override
    public void setEndLineNumber(int lineNumber) {
      this.endLineNumber = lineNumber;
    }

    @Override
    public int getEndColumnNumber() {
      return endColumnNumber;
    }

    @Override
    public void setEndColumnNumber(int columnNumber) {
      this.endColumnNumber = columnNumber;
    }
  }
}
