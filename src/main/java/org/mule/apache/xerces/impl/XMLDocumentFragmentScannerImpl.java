/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.apache.xerces.impl;

import java.io.CharConversionException;
import java.io.EOFException;
import java.io.IOException;
import org.mule.apache.xerces.impl.io.MalformedByteSequenceException;
import org.mule.apache.xerces.util.AugmentationsImpl;
import org.mule.apache.xerces.util.XMLAttributesImpl;
import org.mule.apache.xerces.util.XMLChar;
import org.mule.apache.xerces.util.XMLStringBuffer;
import org.mule.apache.xerces.util.XMLSymbols;
import org.mule.apache.xerces.xni.Augmentations;
import org.mule.apache.xerces.xni.QName;
import org.mule.apache.xerces.xni.XMLAttributes;
import org.mule.apache.xerces.xni.XMLDocumentHandler;
import org.mule.apache.xerces.xni.XMLResourceIdentifier;
import org.mule.apache.xerces.xni.XMLString;
import org.mule.apache.xerces.xni.XNIException;
import org.mule.apache.xerces.xni.parser.XMLComponent;
import org.mule.apache.xerces.xni.parser.XMLComponentManager;
import org.mule.apache.xerces.xni.parser.XMLConfigurationException;
import org.mule.apache.xerces.xni.parser.XMLDocumentScanner;
import org.mule.apache.xerces.xni.parser.XMLInputSource;

/**
 * Shadowed class so that an empty CDATA section is retrievable (XERCESJ-1033). This is the default behavior in OpenJDK. The
 * shadowed class was introduced in MULE-19710. The modified logic is in
 * {@link XMLDocumentFragmentScannerImpl#scanCDATASection(boolean)}.
 *
 * @see <a href="https://issues.apache.org/jira/browse/XERCESJ-1033">http://google.com</a>
 */
public class XMLDocumentFragmentScannerImpl extends XMLScanner implements XMLDocumentScanner, XMLComponent, XMLEntityHandler {

  protected static final int SCANNER_STATE_START_OF_MARKUP = 1;
  protected static final int SCANNER_STATE_COMMENT = 2;
  protected static final int SCANNER_STATE_PI = 3;
  protected static final int SCANNER_STATE_DOCTYPE = 4;
  protected static final int SCANNER_STATE_ROOT_ELEMENT = 6;
  protected static final int SCANNER_STATE_CONTENT = 7;
  protected static final int SCANNER_STATE_REFERENCE = 8;
  protected static final int SCANNER_STATE_END_OF_INPUT = 13;
  protected static final int SCANNER_STATE_TERMINATED = 14;
  protected static final int SCANNER_STATE_CDATA = 15;
  protected static final int SCANNER_STATE_TEXT_DECL = 16;
  protected static final String NAMESPACES = "http://xml.org/sax/features/namespaces";
  protected static final String NOTIFY_BUILTIN_REFS = "http://apache.org/xml/features/scanner/notify-builtin-refs";
  protected static final String ENTITY_RESOLVER = "http://apache.org/xml/properties/internal/entity-resolver";
  private static final String[] RECOGNIZED_FEATURES = new String[] {"http://xml.org/sax/features/namespaces",
      "http://xml.org/sax/features/validation", "http://apache.org/xml/features/scanner/notify-builtin-refs",
      "http://apache.org/xml/features/scanner/notify-char-refs"};
  private static final Boolean[] FEATURE_DEFAULTS;
  private static final String[] RECOGNIZED_PROPERTIES;
  private static final Object[] PROPERTY_DEFAULTS;
  private static final boolean DEBUG_SCANNER_STATE = false;
  private static final boolean DEBUG_DISPATCHER = false;
  protected static final boolean DEBUG_CONTENT_SCANNING = false;
  protected XMLDocumentHandler fDocumentHandler;
  protected int[] fEntityStack = new int[4];
  protected int fMarkupDepth;
  protected int fScannerState;
  protected boolean fInScanContent = false;
  protected boolean fHasExternalDTD;
  protected boolean fStandalone;
  protected boolean fIsEntityDeclaredVC;
  protected ExternalSubsetResolver fExternalSubsetResolver;
  protected QName fCurrentElement;
  protected final XMLDocumentFragmentScannerImpl.ElementStack fElementStack = new XMLDocumentFragmentScannerImpl.ElementStack();
  protected boolean fNotifyBuiltInRefs = false;
  protected XMLDocumentFragmentScannerImpl.Dispatcher fDispatcher;
  protected final XMLDocumentFragmentScannerImpl.Dispatcher fContentDispatcher = this.createContentDispatcher();
  protected final QName fElementQName = new QName();
  protected final QName fAttributeQName = new QName();
  protected final XMLAttributesImpl fAttributes = new XMLAttributesImpl();
  protected final XMLString fTempString = new XMLString();
  protected final XMLString fTempString2 = new XMLString();
  private final String[] fStrings = new String[3];
  private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();
  private final XMLStringBuffer fStringBuffer2 = new XMLStringBuffer();
  private final QName fQName = new QName();
  private final char[] fSingleChar = new char[1];
  private boolean fSawSpace;
  private Augmentations fTempAugmentations = null;

  public XMLDocumentFragmentScannerImpl() {}

  public void setInputSource(XMLInputSource inputSource) throws IOException {
    this.fEntityManager.setEntityHandler(this);
    this.fEntityManager.startEntity("$fragment$", inputSource, false, true);
  }

  public boolean scanDocument(boolean complete) throws IOException, XNIException {
    this.fEntityScanner = this.fEntityManager.getEntityScanner();
    this.fEntityManager.setEntityHandler(this);

    while (this.fDispatcher.dispatch(complete)) {
      if (!complete) {
        return true;
      }
    }

    return false;
  }

  public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {
    super.reset(componentManager);
    this.fAttributes.setNamespaces(this.fNamespaces);
    this.fMarkupDepth = 0;
    this.fCurrentElement = null;
    this.fElementStack.clear();
    this.fHasExternalDTD = false;
    this.fStandalone = false;
    this.fIsEntityDeclaredVC = false;
    this.fInScanContent = false;
    this.setScannerState(7);
    this.setDispatcher(this.fContentDispatcher);
    if (this.fParserSettings) {
      try {
        this.fNotifyBuiltInRefs = componentManager.getFeature("http://apache.org/xml/features/scanner/notify-builtin-refs");
      } catch (XMLConfigurationException var4) {
        this.fNotifyBuiltInRefs = false;
      }

      try {
        Object resolver = componentManager.getProperty("http://apache.org/xml/properties/internal/entity-resolver");
        this.fExternalSubsetResolver = resolver instanceof ExternalSubsetResolver ? (ExternalSubsetResolver) resolver : null;
      } catch (XMLConfigurationException var3) {
        this.fExternalSubsetResolver = null;
      }
    }

  }

  public String[] getRecognizedFeatures() {
    return (String[]) ((String[]) RECOGNIZED_FEATURES.clone());
  }

  public void setFeature(String featureId, boolean state) throws XMLConfigurationException {
    super.setFeature(featureId, state);
    if (featureId.startsWith("http://apache.org/xml/features/")) {
      int suffixLength = featureId.length() - "http://apache.org/xml/features/".length();
      if (suffixLength == "scanner/notify-builtin-refs".length() && featureId.endsWith("scanner/notify-builtin-refs")) {
        this.fNotifyBuiltInRefs = state;
      }
    }

  }

  public String[] getRecognizedProperties() {
    return (String[]) ((String[]) RECOGNIZED_PROPERTIES.clone());
  }

  public void setProperty(String propertyId, Object value) throws XMLConfigurationException {
    super.setProperty(propertyId, value);
    if (propertyId.startsWith("http://apache.org/xml/properties/")) {
      int suffixLength = propertyId.length() - "http://apache.org/xml/properties/".length();
      if (suffixLength == "internal/entity-manager".length() && propertyId.endsWith("internal/entity-manager")) {
        this.fEntityManager = (XMLEntityManager) value;
        return;
      }

      if (suffixLength == "internal/entity-resolver".length() && propertyId.endsWith("internal/entity-resolver")) {
        this.fExternalSubsetResolver = value instanceof ExternalSubsetResolver ? (ExternalSubsetResolver) value : null;
        return;
      }
    }

  }

  public Boolean getFeatureDefault(String featureId) {
    for (int i = 0; i < RECOGNIZED_FEATURES.length; ++i) {
      if (RECOGNIZED_FEATURES[i].equals(featureId)) {
        return FEATURE_DEFAULTS[i];
      }
    }

    return null;
  }

  public Object getPropertyDefault(String propertyId) {
    for (int i = 0; i < RECOGNIZED_PROPERTIES.length; ++i) {
      if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
        return PROPERTY_DEFAULTS[i];
      }
    }

    return null;
  }

  public void setDocumentHandler(XMLDocumentHandler documentHandler) {
    this.fDocumentHandler = documentHandler;
  }

  public XMLDocumentHandler getDocumentHandler() {
    return this.fDocumentHandler;
  }

  public void startEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs)
      throws XNIException {
    if (this.fEntityDepth == this.fEntityStack.length) {
      int[] entityarray = new int[this.fEntityStack.length * 2];
      System.arraycopy(this.fEntityStack, 0, entityarray, 0, this.fEntityStack.length);
      this.fEntityStack = entityarray;
    }

    this.fEntityStack[this.fEntityDepth] = this.fMarkupDepth;
    super.startEntity(name, identifier, encoding, augs);
    if (this.fStandalone && this.fEntityManager.isEntityDeclInExternalSubset(name)) {
      this.reportFatalError("MSG_REFERENCE_TO_EXTERNALLY_DECLARED_ENTITY_WHEN_STANDALONE", new Object[] {name});
    }

    if (this.fDocumentHandler != null && !this.fScanningAttribute && !name.equals("[xml]")) {
      this.fDocumentHandler.startGeneralEntity(name, identifier, encoding, augs);
    }

  }

  public void endEntity(String name, Augmentations augs) throws XNIException {
    if (this.fInScanContent && this.fStringBuffer.length != 0 && this.fDocumentHandler != null) {
      this.fDocumentHandler.characters(this.fStringBuffer, (Augmentations) null);
      this.fStringBuffer.length = 0;
    }

    super.endEntity(name, augs);
    if (this.fMarkupDepth != this.fEntityStack[this.fEntityDepth]) {
      this.reportFatalError("MarkupEntityMismatch", (Object[]) null);
    }

    if (this.fDocumentHandler != null && !this.fScanningAttribute && !name.equals("[xml]")) {
      this.fDocumentHandler.endGeneralEntity(name, augs);
    }

  }

  protected XMLDocumentFragmentScannerImpl.Dispatcher createContentDispatcher() {
    return new XMLDocumentFragmentScannerImpl.FragmentContentDispatcher();
  }

  protected void scanXMLDeclOrTextDecl(boolean scanningTextDecl) throws IOException, XNIException {
    super.scanXMLDeclOrTextDecl(scanningTextDecl, this.fStrings);
    --this.fMarkupDepth;
    String version = this.fStrings[0];
    String encoding = this.fStrings[1];
    String standalone = this.fStrings[2];
    this.fStandalone = standalone != null && standalone.equals("yes");
    this.fEntityManager.setStandalone(this.fStandalone);
    this.fEntityScanner.setXMLVersion(version);
    if (this.fDocumentHandler != null) {
      if (scanningTextDecl) {
        this.fDocumentHandler.textDecl(version, encoding, (Augmentations) null);
      } else {
        this.fDocumentHandler.xmlDecl(version, encoding, standalone, (Augmentations) null);
      }
    }

    if (encoding != null && !this.fEntityScanner.fCurrentEntity.isEncodingExternallySpecified()) {
      this.fEntityScanner.setEncoding(encoding);
    }

  }

  protected void scanPIData(String target, XMLString data) throws IOException, XNIException {
    super.scanPIData(target, data);
    --this.fMarkupDepth;
    if (this.fDocumentHandler != null) {
      this.fDocumentHandler.processingInstruction(target, data, (Augmentations) null);
    }

  }

  protected void scanComment() throws IOException, XNIException {
    this.scanComment(this.fStringBuffer);
    --this.fMarkupDepth;
    if (this.fDocumentHandler != null) {
      this.fDocumentHandler.comment(this.fStringBuffer, (Augmentations) null);
    }

  }

  protected boolean scanStartElement() throws IOException, XNIException {
    String rawname;
    if (this.fNamespaces) {
      this.fEntityScanner.scanQName(this.fElementQName);
    } else {
      rawname = this.fEntityScanner.scanName();
      this.fElementQName.setValues((String) null, rawname, rawname, (String) null);
    }

    rawname = this.fElementQName.rawname;
    this.fCurrentElement = this.fElementStack.pushElement(this.fElementQName);
    boolean empty = false;
    this.fAttributes.removeAllAttributes();

    while (true) {
      boolean sawSpace = this.fEntityScanner.skipSpaces();
      int c = this.fEntityScanner.peekChar();
      if (c == 62) {
        this.fEntityScanner.scanChar();
        break;
      }

      if (c == 47) {
        this.fEntityScanner.scanChar();
        if (!this.fEntityScanner.skipChar(62)) {
          this.reportFatalError("ElementUnterminated", new Object[] {rawname});
        }

        empty = true;
        break;
      }

      if ((!this.isValidNameStartChar(c) || !sawSpace) && (!this.isValidNameStartHighSurrogate(c) || !sawSpace)) {
        this.reportFatalError("ElementUnterminated", new Object[] {rawname});
      }

      this.scanAttribute(this.fAttributes);
    }

    if (this.fDocumentHandler != null) {
      if (empty) {
        --this.fMarkupDepth;
        if (this.fMarkupDepth < this.fEntityStack[this.fEntityDepth - 1]) {
          this.reportFatalError("ElementEntityMismatch", new Object[] {this.fCurrentElement.rawname});
        }

        this.fDocumentHandler.emptyElement(this.fElementQName, this.fAttributes, (Augmentations) null);
        this.fElementStack.popElement(this.fElementQName);
      } else {
        this.fDocumentHandler.startElement(this.fElementQName, this.fAttributes, (Augmentations) null);
      }
    }

    return empty;
  }

  protected void scanStartElementName() throws IOException, XNIException {
    if (this.fNamespaces) {
      this.fEntityScanner.scanQName(this.fElementQName);
    } else {
      String name = this.fEntityScanner.scanName();
      this.fElementQName.setValues((String) null, name, name, (String) null);
    }

    this.fSawSpace = this.fEntityScanner.skipSpaces();
  }

  protected boolean scanStartElementAfterName() throws IOException, XNIException {
    String rawname = this.fElementQName.rawname;
    this.fCurrentElement = this.fElementStack.pushElement(this.fElementQName);
    boolean empty = false;
    this.fAttributes.removeAllAttributes();

    while (true) {
      int c = this.fEntityScanner.peekChar();
      if (c == 62) {
        this.fEntityScanner.scanChar();
        break;
      }

      if (c == 47) {
        this.fEntityScanner.scanChar();
        if (!this.fEntityScanner.skipChar(62)) {
          this.reportFatalError("ElementUnterminated", new Object[] {rawname});
        }

        empty = true;
        break;
      }

      if ((!this.isValidNameStartChar(c) || !this.fSawSpace) && (!this.isValidNameStartHighSurrogate(c) || !this.fSawSpace)) {
        this.reportFatalError("ElementUnterminated", new Object[] {rawname});
      }

      this.scanAttribute(this.fAttributes);
      this.fSawSpace = this.fEntityScanner.skipSpaces();
    }

    if (this.fDocumentHandler != null) {
      if (empty) {
        --this.fMarkupDepth;
        if (this.fMarkupDepth < this.fEntityStack[this.fEntityDepth - 1]) {
          this.reportFatalError("ElementEntityMismatch", new Object[] {this.fCurrentElement.rawname});
        }

        this.fDocumentHandler.emptyElement(this.fElementQName, this.fAttributes, (Augmentations) null);
        this.fElementStack.popElement(this.fElementQName);
      } else {
        this.fDocumentHandler.startElement(this.fElementQName, this.fAttributes, (Augmentations) null);
      }
    }

    return empty;
  }

  protected void scanAttribute(XMLAttributes attributes) throws IOException, XNIException {
    if (this.fNamespaces) {
      this.fEntityScanner.scanQName(this.fAttributeQName);
    } else {
      String name = this.fEntityScanner.scanName();
      this.fAttributeQName.setValues((String) null, name, name, (String) null);
    }

    this.fEntityScanner.skipSpaces();
    if (!this.fEntityScanner.skipChar(61)) {
      this.reportFatalError("EqRequiredInAttribute", new Object[] {this.fCurrentElement.rawname, this.fAttributeQName.rawname});
    }

    this.fEntityScanner.skipSpaces();
    int oldLen = attributes.getLength();
    int attrIndex = attributes.addAttribute(this.fAttributeQName, XMLSymbols.fCDATASymbol, (String) null);
    if (oldLen == attributes.getLength()) {
      this.reportFatalError("AttributeNotUnique", new Object[] {this.fCurrentElement.rawname, this.fAttributeQName.rawname});
    }

    boolean isSameNormalizedAttr = this.scanAttributeValue(this.fTempString, this.fTempString2, this.fAttributeQName.rawname,
                                                           this.fIsEntityDeclaredVC, this.fCurrentElement.rawname);
    attributes.setValue(attrIndex, this.fTempString.toString());
    if (!isSameNormalizedAttr) {
      attributes.setNonNormalizedValue(attrIndex, this.fTempString2.toString());
    }

    attributes.setSpecified(attrIndex, true);
  }

  protected int scanContent() throws IOException, XNIException {
    XMLString content = this.fTempString;
    int c = this.fEntityScanner.scanContent((XMLString) content);
    if (c == 13) {
      this.fEntityScanner.scanChar();
      this.fStringBuffer.clear();
      this.fStringBuffer.append(this.fTempString);
      this.fStringBuffer.append((char) c);
      content = this.fStringBuffer;
      c = -1;
    }

    if (this.fDocumentHandler != null && ((XMLString) content).length > 0) {
      this.fDocumentHandler.characters((XMLString) content, (Augmentations) null);
    }

    if (c == 93 && this.fTempString.length == 0) {
      this.fStringBuffer.clear();
      this.fStringBuffer.append((char) this.fEntityScanner.scanChar());
      this.fInScanContent = true;
      if (this.fEntityScanner.skipChar(93)) {
        this.fStringBuffer.append(']');

        while (this.fEntityScanner.skipChar(93)) {
          this.fStringBuffer.append(']');
        }

        if (this.fEntityScanner.skipChar(62)) {
          this.reportFatalError("CDEndInContent", (Object[]) null);
        }
      }

      if (this.fDocumentHandler != null && this.fStringBuffer.length != 0) {
        this.fDocumentHandler.characters(this.fStringBuffer, (Augmentations) null);
      }

      this.fInScanContent = false;
      c = -1;
    }

    return c;
  }

  protected boolean scanCDATASection(boolean complete) throws IOException, XNIException {
    if (this.fDocumentHandler != null) {
      this.fDocumentHandler.startCDATA((Augmentations) null);
    }

    while (true) {
      while (true) {
        this.fStringBuffer.clear();
        int brackets;
        if (!this.fEntityScanner.scanData("]]", this.fStringBuffer)) {
          // MULE-19710 This condition is modified in the shadowed class.
          // XERCESJ-1033 Empty CDATA sections are also processed.
          if (this.fDocumentHandler != null) {
            this.fDocumentHandler.characters(this.fStringBuffer, (Augmentations) null);
          }

          for (brackets = 0; this.fEntityScanner.skipChar(93); ++brackets) {
          }

          if (this.fDocumentHandler != null && brackets > 0) {
            this.fStringBuffer.clear();
            int chunks;
            if (brackets > 2048) {
              chunks = brackets / 2048;
              int remainder = brackets % 2048;

              int i;
              for (i = 0; i < 2048; ++i) {
                this.fStringBuffer.append(']');
              }

              for (i = 0; i < chunks; ++i) {
                this.fDocumentHandler.characters(this.fStringBuffer, (Augmentations) null);
              }

              if (remainder != 0) {
                this.fStringBuffer.length = remainder;
                this.fDocumentHandler.characters(this.fStringBuffer, (Augmentations) null);
              }
            } else {
              for (chunks = 0; chunks < brackets; ++chunks) {
                this.fStringBuffer.append(']');
              }

              this.fDocumentHandler.characters(this.fStringBuffer, (Augmentations) null);
            }
          }

          if (this.fEntityScanner.skipChar(62)) {
            --this.fMarkupDepth;
            if (this.fDocumentHandler != null) {
              this.fDocumentHandler.endCDATA((Augmentations) null);
            }

            return true;
          }

          if (this.fDocumentHandler != null) {
            this.fStringBuffer.clear();
            this.fStringBuffer.append("]]");
            this.fDocumentHandler.characters(this.fStringBuffer, (Augmentations) null);
          }
        } else {
          if (this.fDocumentHandler != null) {
            this.fDocumentHandler.characters(this.fStringBuffer, (Augmentations) null);
          }

          brackets = this.fEntityScanner.peekChar();
          if (brackets != -1 && this.isInvalidLiteral(brackets)) {
            if (XMLChar.isHighSurrogate(brackets)) {
              this.fStringBuffer.clear();
              this.scanSurrogates(this.fStringBuffer);
              if (this.fDocumentHandler != null) {
                this.fDocumentHandler.characters(this.fStringBuffer, (Augmentations) null);
              }
            } else {
              this.reportFatalError("InvalidCharInCDSect", new Object[] {Integer.toString(brackets, 16)});
              this.fEntityScanner.scanChar();
            }
          }
        }
      }
    }
  }

  protected int scanEndElement() throws IOException, XNIException {
    this.fElementStack.popElement(this.fElementQName);
    if (!this.fEntityScanner.skipString(this.fElementQName.rawname)) {
      this.reportFatalError("ETagRequired", new Object[] {this.fElementQName.rawname});
    }

    this.fEntityScanner.skipSpaces();
    if (!this.fEntityScanner.skipChar(62)) {
      this.reportFatalError("ETagUnterminated", new Object[] {this.fElementQName.rawname});
    }

    --this.fMarkupDepth;
    --this.fMarkupDepth;
    if (this.fMarkupDepth < this.fEntityStack[this.fEntityDepth - 1]) {
      this.reportFatalError("ElementEntityMismatch", new Object[] {this.fCurrentElement.rawname});
    }

    if (this.fDocumentHandler != null) {
      this.fDocumentHandler.endElement(this.fElementQName, (Augmentations) null);
    }

    return this.fMarkupDepth;
  }

  protected void scanCharReference() throws IOException, XNIException {
    this.fStringBuffer2.clear();
    int ch = this.scanCharReferenceValue(this.fStringBuffer2, (XMLStringBuffer) null);
    --this.fMarkupDepth;
    if (ch != -1 && this.fDocumentHandler != null) {
      if (this.fNotifyCharRefs) {
        this.fDocumentHandler.startGeneralEntity(this.fCharRefLiteral, (XMLResourceIdentifier) null, (String) null,
                                                 (Augmentations) null);
      }

      Augmentations augs = null;
      if (this.fValidation && ch <= 32) {
        if (this.fTempAugmentations != null) {
          this.fTempAugmentations.removeAllItems();
        } else {
          this.fTempAugmentations = new AugmentationsImpl();
        }

        augs = this.fTempAugmentations;
        augs.putItem("CHAR_REF_PROBABLE_WS", Boolean.TRUE);
      }

      this.fDocumentHandler.characters(this.fStringBuffer2, augs);
      if (this.fNotifyCharRefs) {
        this.fDocumentHandler.endGeneralEntity(this.fCharRefLiteral, (Augmentations) null);
      }
    }

  }

  protected void scanEntityReference() throws IOException, XNIException {
    String name = this.fEntityScanner.scanName();
    if (name == null) {
      this.reportFatalError("NameRequiredInReference", (Object[]) null);
    } else {
      if (!this.fEntityScanner.skipChar(59)) {
        this.reportFatalError("SemicolonRequiredInReference", new Object[] {name});
      }

      --this.fMarkupDepth;
      if (name == fAmpSymbol) {
        this.handleCharacter('&', fAmpSymbol);
      } else if (name == fLtSymbol) {
        this.handleCharacter('<', fLtSymbol);
      } else if (name == fGtSymbol) {
        this.handleCharacter('>', fGtSymbol);
      } else if (name == fQuotSymbol) {
        this.handleCharacter('"', fQuotSymbol);
      } else if (name == fAposSymbol) {
        this.handleCharacter('\'', fAposSymbol);
      } else if (this.fEntityManager.isUnparsedEntity(name)) {
        this.reportFatalError("ReferenceToUnparsedEntity", new Object[] {name});
      } else {
        if (!this.fEntityManager.isDeclaredEntity(name)) {
          if (this.fIsEntityDeclaredVC) {
            if (this.fValidation) {
              this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "EntityNotDeclared",
                                              new Object[] {name}, (short) 1);
            }
          } else {
            this.reportFatalError("EntityNotDeclared", new Object[] {name});
          }
        }

        this.fEntityManager.startEntity(name, false);
      }

    }
  }

  private void handleCharacter(char c, String entity) throws XNIException {
    if (this.fDocumentHandler != null) {
      if (this.fNotifyBuiltInRefs) {
        this.fDocumentHandler.startGeneralEntity(entity, (XMLResourceIdentifier) null, (String) null, (Augmentations) null);
      }

      this.fSingleChar[0] = c;
      this.fTempString.setValues(this.fSingleChar, 0, 1);
      this.fDocumentHandler.characters(this.fTempString, (Augmentations) null);
      if (this.fNotifyBuiltInRefs) {
        this.fDocumentHandler.endGeneralEntity(entity, (Augmentations) null);
      }
    }

  }

  protected int handleEndElement(QName element, boolean isEmpty) throws XNIException {
    --this.fMarkupDepth;
    if (this.fMarkupDepth < this.fEntityStack[this.fEntityDepth - 1]) {
      this.reportFatalError("ElementEntityMismatch", new Object[] {this.fCurrentElement.rawname});
    }

    QName startElement = this.fQName;
    this.fElementStack.popElement(startElement);
    if (element.rawname != startElement.rawname) {
      this.reportFatalError("ETagRequired", new Object[] {startElement.rawname});
    }

    if (this.fNamespaces) {
      element.uri = startElement.uri;
    }

    if (this.fDocumentHandler != null && !isEmpty) {
      this.fDocumentHandler.endElement(element, (Augmentations) null);
    }

    return this.fMarkupDepth;
  }

  protected final void setScannerState(int state) {
    this.fScannerState = state;
  }

  protected final void setDispatcher(XMLDocumentFragmentScannerImpl.Dispatcher dispatcher) {
    this.fDispatcher = dispatcher;
  }

  protected String getScannerStateName(int state) {
    switch (state) {
      case 1:
        return "SCANNER_STATE_START_OF_MARKUP";
      case 2:
        return "SCANNER_STATE_COMMENT";
      case 3:
        return "SCANNER_STATE_PI";
      case 4:
        return "SCANNER_STATE_DOCTYPE";
      case 5:
      case 9:
      case 10:
      case 11:
      case 12:
      default:
        return "??? (" + state + ')';
      case 6:
        return "SCANNER_STATE_ROOT_ELEMENT";
      case 7:
        return "SCANNER_STATE_CONTENT";
      case 8:
        return "SCANNER_STATE_REFERENCE";
      case 13:
        return "SCANNER_STATE_END_OF_INPUT";
      case 14:
        return "SCANNER_STATE_TERMINATED";
      case 15:
        return "SCANNER_STATE_CDATA";
      case 16:
        return "SCANNER_STATE_TEXT_DECL";
    }
  }

  public String getDispatcherName(XMLDocumentFragmentScannerImpl.Dispatcher dispatcher) {
    return "null";
  }

  static {
    FEATURE_DEFAULTS = new Boolean[] {null, null, Boolean.FALSE, Boolean.FALSE};
    RECOGNIZED_PROPERTIES = new String[] {"http://apache.org/xml/properties/internal/symbol-table",
        "http://apache.org/xml/properties/internal/error-reporter", "http://apache.org/xml/properties/internal/entity-manager",
        "http://apache.org/xml/properties/internal/entity-resolver"};
    PROPERTY_DEFAULTS = new Object[] {null, null, null, null};
  }

  protected class FragmentContentDispatcher implements XMLDocumentFragmentScannerImpl.Dispatcher {

    protected FragmentContentDispatcher() {}

    public boolean dispatch(boolean complete) throws IOException, XNIException {
      try {
        boolean again;
        do {
          again = false;
          switch (XMLDocumentFragmentScannerImpl.this.fScannerState) {
            case 1:
              ++XMLDocumentFragmentScannerImpl.this.fMarkupDepth;
              if (XMLDocumentFragmentScannerImpl.this.fEntityScanner.skipChar(47)) {
                if (XMLDocumentFragmentScannerImpl.this.scanEndElement() == 0 && this.elementDepthIsZeroHook()) {
                  return true;
                }

                XMLDocumentFragmentScannerImpl.this.setScannerState(7);
              } else if (XMLDocumentFragmentScannerImpl.this
                  .isValidNameStartChar(XMLDocumentFragmentScannerImpl.this.fEntityScanner.peekChar())) {
                XMLDocumentFragmentScannerImpl.this.scanStartElement();
                XMLDocumentFragmentScannerImpl.this.setScannerState(7);
              } else if (XMLDocumentFragmentScannerImpl.this.fEntityScanner.skipChar(33)) {
                if (XMLDocumentFragmentScannerImpl.this.fEntityScanner.skipChar(45)) {
                  if (!XMLDocumentFragmentScannerImpl.this.fEntityScanner.skipChar(45)) {
                    XMLDocumentFragmentScannerImpl.this.reportFatalError("InvalidCommentStart", (Object[]) null);
                  }

                  XMLDocumentFragmentScannerImpl.this.setScannerState(2);
                  again = true;
                } else if (XMLDocumentFragmentScannerImpl.this.fEntityScanner.skipString("[CDATA[")) {
                  XMLDocumentFragmentScannerImpl.this.setScannerState(15);
                  again = true;
                } else if (!this.scanForDoctypeHook()) {
                  XMLDocumentFragmentScannerImpl.this.reportFatalError("MarkupNotRecognizedInContent", (Object[]) null);
                }
              } else if (XMLDocumentFragmentScannerImpl.this.fEntityScanner.skipChar(63)) {
                XMLDocumentFragmentScannerImpl.this.setScannerState(3);
                again = true;
              } else if (XMLDocumentFragmentScannerImpl.this
                  .isValidNameStartHighSurrogate(XMLDocumentFragmentScannerImpl.this.fEntityScanner.peekChar())) {
                XMLDocumentFragmentScannerImpl.this.scanStartElement();
                XMLDocumentFragmentScannerImpl.this.setScannerState(7);
              } else {
                XMLDocumentFragmentScannerImpl.this.reportFatalError("MarkupNotRecognizedInContent", (Object[]) null);
                XMLDocumentFragmentScannerImpl.this.setScannerState(7);
              }
              break;
            case 2:
              XMLDocumentFragmentScannerImpl.this.scanComment();
              XMLDocumentFragmentScannerImpl.this.setScannerState(7);
              break;
            case 3:
              XMLDocumentFragmentScannerImpl.this.scanPI();
              XMLDocumentFragmentScannerImpl.this.setScannerState(7);
              break;
            case 4:
              XMLDocumentFragmentScannerImpl.this.reportFatalError("DoctypeIllegalInContent", (Object[]) null);
              XMLDocumentFragmentScannerImpl.this.setScannerState(7);
            case 5:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            default:
              break;
            case 6:
              if (this.scanRootElementHook()) {
                return true;
              }

              XMLDocumentFragmentScannerImpl.this.setScannerState(7);
              break;
            case 7:
              if (XMLDocumentFragmentScannerImpl.this.fEntityScanner.skipChar(60)) {
                XMLDocumentFragmentScannerImpl.this.setScannerState(1);
                again = true;
              } else if (XMLDocumentFragmentScannerImpl.this.fEntityScanner.skipChar(38)) {
                XMLDocumentFragmentScannerImpl.this.setScannerState(8);
                again = true;
              } else {
                while (true) {
                  int c = XMLDocumentFragmentScannerImpl.this.scanContent();
                  if (c == 60) {
                    XMLDocumentFragmentScannerImpl.this.fEntityScanner.scanChar();
                    XMLDocumentFragmentScannerImpl.this.setScannerState(1);
                    break;
                  }

                  if (c == 38) {
                    XMLDocumentFragmentScannerImpl.this.fEntityScanner.scanChar();
                    XMLDocumentFragmentScannerImpl.this.setScannerState(8);
                    break;
                  }

                  if (c != -1 && XMLDocumentFragmentScannerImpl.this.isInvalidLiteral(c)) {
                    if (XMLChar.isHighSurrogate(c)) {
                      XMLDocumentFragmentScannerImpl.this.fStringBuffer.clear();
                      if (XMLDocumentFragmentScannerImpl.this.scanSurrogates(XMLDocumentFragmentScannerImpl.this.fStringBuffer)
                          && XMLDocumentFragmentScannerImpl.this.fDocumentHandler != null) {
                        XMLDocumentFragmentScannerImpl.this.fDocumentHandler
                            .characters(XMLDocumentFragmentScannerImpl.this.fStringBuffer, (Augmentations) null);
                      }
                    } else {
                      XMLDocumentFragmentScannerImpl.this.reportFatalError("InvalidCharInContent",
                                                                           new Object[] {Integer.toString(c, 16)});
                      XMLDocumentFragmentScannerImpl.this.fEntityScanner.scanChar();
                    }
                  }

                  if (!complete) {
                    break;
                  }
                }
              }
              break;
            case 8:
              ++XMLDocumentFragmentScannerImpl.this.fMarkupDepth;
              XMLDocumentFragmentScannerImpl.this.setScannerState(7);
              if (XMLDocumentFragmentScannerImpl.this.fEntityScanner.skipChar(35)) {
                XMLDocumentFragmentScannerImpl.this.scanCharReference();
              } else {
                XMLDocumentFragmentScannerImpl.this.scanEntityReference();
              }
              break;
            case 15:
              XMLDocumentFragmentScannerImpl.this.scanCDATASection(complete);
              XMLDocumentFragmentScannerImpl.this.setScannerState(7);
              break;
            case 16:
              if (XMLDocumentFragmentScannerImpl.this.fEntityScanner.skipString("<?xml")) {
                ++XMLDocumentFragmentScannerImpl.this.fMarkupDepth;
                if (!XMLDocumentFragmentScannerImpl.this
                    .isValidNameChar(XMLDocumentFragmentScannerImpl.this.fEntityScanner.peekChar())) {
                  XMLDocumentFragmentScannerImpl.this.scanXMLDeclOrTextDecl(true);
                } else {
                  XMLDocumentFragmentScannerImpl.this.fStringBuffer.clear();
                  XMLDocumentFragmentScannerImpl.this.fStringBuffer.append("xml");
                  if (XMLDocumentFragmentScannerImpl.this.fNamespaces) {
                    while (XMLDocumentFragmentScannerImpl.this
                        .isValidNCName(XMLDocumentFragmentScannerImpl.this.fEntityScanner.peekChar())) {
                      XMLDocumentFragmentScannerImpl.this.fStringBuffer
                          .append((char) XMLDocumentFragmentScannerImpl.this.fEntityScanner.scanChar());
                    }
                  } else {
                    while (XMLDocumentFragmentScannerImpl.this
                        .isValidNameChar(XMLDocumentFragmentScannerImpl.this.fEntityScanner.peekChar())) {
                      XMLDocumentFragmentScannerImpl.this.fStringBuffer
                          .append((char) XMLDocumentFragmentScannerImpl.this.fEntityScanner.scanChar());
                    }
                  }

                  String target = XMLDocumentFragmentScannerImpl.this.fSymbolTable
                      .addSymbol(XMLDocumentFragmentScannerImpl.this.fStringBuffer.ch,
                                 XMLDocumentFragmentScannerImpl.this.fStringBuffer.offset,
                                 XMLDocumentFragmentScannerImpl.this.fStringBuffer.length);
                  XMLDocumentFragmentScannerImpl.this.scanPIData(target, XMLDocumentFragmentScannerImpl.this.fTempString);
                }
              }

              XMLDocumentFragmentScannerImpl.this.fEntityManager.fCurrentEntity.mayReadChunks = true;
              XMLDocumentFragmentScannerImpl.this.setScannerState(7);
          }
        } while (complete || again);

        return true;
      } catch (MalformedByteSequenceException var4) {
        XMLDocumentFragmentScannerImpl.this.fErrorReporter.reportError(var4.getDomain(), var4.getKey(), var4.getArguments(),
                                                                       (short) 2, var4);
        return false;
      } catch (CharConversionException var5) {
        XMLDocumentFragmentScannerImpl.this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210",
                                                                       "CharConversionFailure", (Object[]) null, (short) 2, var5);
        return false;
      } catch (EOFException var6) {
        this.endOfFileHook(var6);
        return false;
      }
    }

    protected boolean scanForDoctypeHook() throws IOException, XNIException {
      return false;
    }

    protected boolean elementDepthIsZeroHook() throws IOException, XNIException {
      return false;
    }

    protected boolean scanRootElementHook() throws IOException, XNIException {
      return false;
    }

    protected void endOfFileHook(EOFException e) throws IOException, XNIException {
      if (XMLDocumentFragmentScannerImpl.this.fMarkupDepth != 0) {
        XMLDocumentFragmentScannerImpl.this.reportFatalError("PrematureEOF", (Object[]) null);
      }

    }
  }

  protected interface Dispatcher {

    boolean dispatch(boolean var1) throws IOException, XNIException;
  }

  protected static class ElementStack {

    protected QName[] fElements = new QName[10];
    protected int fSize;

    public ElementStack() {
      for (int i = 0; i < this.fElements.length; ++i) {
        this.fElements[i] = new QName();
      }

    }

    public QName pushElement(QName element) {
      if (this.fSize == this.fElements.length) {
        QName[] array = new QName[this.fElements.length * 2];
        System.arraycopy(this.fElements, 0, array, 0, this.fSize);
        this.fElements = array;

        for (int i = this.fSize; i < this.fElements.length; ++i) {
          this.fElements[i] = new QName();
        }
      }

      this.fElements[this.fSize].setValues(element);
      return this.fElements[this.fSize++];
    }

    public void popElement(QName element) {
      element.setValues(this.fElements[--this.fSize]);
    }

    public void clear() {
      this.fSize = 0;
    }
  }
}
