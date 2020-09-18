package org.mule.runtime.dsl.internal.xni.parser;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.mule.runtime.dsl.api.xni.parser.XmlGathererErrorHandler;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class DefaultXmlGathererErrorHandler implements XmlGathererErrorHandler {

  private static final Logger LOGGER = getLogger(DefaultXmlGathererErrorHandler.class);

  private List<XMLParseException> errors = new ArrayList<>();

  @Override
  public void warning(String domain, String key, XMLParseException e) throws XNIException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found warning parsing domain: %s, key: %s", domain, key), e);
    }
  }

  @Override
  public void error(String domain, String key, XMLParseException e) throws XNIException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found error parsing domain: %s, key: %s", domain, key), e);
    }
    errors.add(e);
  }

  @Override
  public void fatalError(String domain, String key, XMLParseException e) throws XNIException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found fatal error parsing domain: %s, key: %s", domain, key), e);
    }
    throw e;
  }

  @Override
  public List<XMLParseException> getErrors() {
    return errors;
  }
}
