/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.xml.parser.pool;

import static com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription.XML_SCHEMA;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.parsers.XMLGrammarPreparser;
import com.sun.org.apache.xerces.internal.util.XMLGrammarPoolImpl;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.mule.runtime.api.util.LazyValue;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MuleXMLGrammarPool {

  private static final Logger LOGGER = getLogger(MuleXMLGrammarPool.class);

  private static final String CUSTOM_SCHEMA_MAPPINGS_LOCATION = "META-INF/mule.schemas";
  private static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
  private static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

  private static final Object LOCK_OBJECT = new Object();
  private static MuleXMLGrammarPool INSTANCE;

  private final Map<String, String> muleSchemaMappings;
  private final XMLEntityResolver entityResolver;
  private final LazyValue<XMLGrammarPool> xmlGrammarPool;

  public static MuleXMLGrammarPool getInstance() {
    if (INSTANCE == null) {
      synchronized (LOCK_OBJECT) {
        if (INSTANCE == null) {
          INSTANCE = new MuleXMLGrammarPool();
        }
      }
    }
    return INSTANCE;
  }

  private MuleXMLGrammarPool() {
    this.muleSchemaMappings = getMuleSchemaMappings();
    this.entityResolver = new MuleXmlEntityResolver(muleSchemaMappings);
    this.xmlGrammarPool = new LazyValue<>(this::createXmlGrammarPool);
  }

  private Map<String, String> getMuleSchemaMappings() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Loading schema mappings from [" + CUSTOM_SCHEMA_MAPPINGS_LOCATION + "]");
    }
    try {
      Properties muleMappings =
          loadAllProperties(CUSTOM_SCHEMA_MAPPINGS_LOCATION, MuleXMLGrammarPool.class.getClassLoader());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Loaded Mule schema mappings: " + muleMappings);
      }
      Map<String, String> schemaMappings = new HashMap<>(muleMappings.size());

      mergePropertiesIntoMap(muleMappings, schemaMappings);
      return schemaMappings;
    } catch (IOException ex) {
      throw new IllegalStateException(
                                      "Unable to load schema mappings from location [" + CUSTOM_SCHEMA_MAPPINGS_LOCATION + "]",
                                      ex);
    }
  }

  private <K, V> void mergePropertiesIntoMap(Properties props, Map<K, V> map) {
    if (props != null) {
      for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements();) {
        String key = (String) en.nextElement();
        Object value = props.get(key);
        if (value == null) {
          // Allow for defaults fallback or potentially overridden accessor...
          value = props.getProperty(key);
        }
        map.put((K) key, (V) value);
      }
    }
  }

  private Properties loadAllProperties(String resourceName, ClassLoader classLoader) throws IOException {
    ClassLoader classLoaderToUse = classLoader;
    // TODO: Se necesita???
    /*
    if (classLoaderToUse == null) {
        classLoaderToUse = ClassUtils.getDefaultClassLoader();
    }
     */
    Enumeration<URL> urls =
        (classLoaderToUse != null ? classLoaderToUse.getResources(resourceName) : ClassLoader.getSystemResources(resourceName));
    Properties props = new Properties();
    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();
      URLConnection con = url.openConnection();
      // TODO: ML Se necesita ???
      // ResourceUtils.useCachesIfNecessary(con);
      InputStream is = con.getInputStream();
      try {
        if (resourceName != null && resourceName.endsWith(".xml")) {
          props.loadFromXML(is);
        } else {
          props.load(is);
        }
      } finally {
        is.close();
      }
    }
    return props;
  }

  private XMLGrammarPool createXmlGrammarPool() {
    XMLGrammarPool grammarPool = new XMLGrammarPoolImpl();

    // create grammar preparser
    XMLGrammarPreparser preparser = new XMLGrammarPreparser();
    preparser.setGrammarPool(grammarPool);

    preparser.registerPreparser(XML_SCHEMA, null);

    // set properties
    preparser.setFeature(NAMESPACES_FEATURE_ID, true);
    preparser.setFeature(VALIDATION_FEATURE_ID, true);

    preparser.setErrorHandler(new XMLErrorHandler() {

      @Override
      public void warning(String domain, String key, XMLParseException exception) throws XNIException {
        LOGGER.warn(format("Found warning parsing domain: %s, key: %s", domain, key), exception);
      }

      @Override
      public void error(String domain, String key, XMLParseException exception) throws XNIException {
        LOGGER.warn(format("Found error parsing domain: %s, key: %s", domain, key), exception);
      }

      @Override
      public void fatalError(String domain, String key, XMLParseException exception) throws XNIException {
        LOGGER.warn(format("Found fatal error parsing domain: %s, key: %s", domain, key), exception);
      }
    });

    preparser.setEntityResolver(entityResolver);

    // parse grammars
    // muleSchemaMappings.keySet().stream().filter(systemId -> systemId.contains("mule-core-common.xsd")).forEach(systemId -> loadMuleSchemas(preparser, systemId));
    muleSchemaMappings.keySet().forEach(systemId -> loadMuleSchemas(preparser, systemId));
    // schemasToLoad.forEach(systemId -> loadMuleSchemas(preparser, systemId));

    grammarPool.lockPool();
    Grammar[] grammars = grammarPool.retrieveInitialGrammarSet(XML_SCHEMA);
    return grammarPool;
  }

  private void loadMuleSchemas(XMLGrammarPreparser preparser, String systemId) {
    try {
      //XMLInputSource source = new XMLInputSource(null, systemId, null);
      XMLInputSource source = getInputSource(systemId, MuleXMLGrammarPool.class.getClassLoader());
      preparser.preparseGrammar(XML_SCHEMA, source);
    } catch (IOException e) {
      LOGGER.warn(format("Unable to load schema mappings [%s]", systemId), e);
    }
  }

  private XMLInputSource getInputSource(String systemId, ClassLoader cl) {
    String resourceLocation = muleSchemaMappings.get(systemId);
    if (resourceLocation != null) {
      InputStream is = cl.getResourceAsStream(resourceLocation);
      if (is == null) {
        LOGGER.debug("Couldn't find XML schema [" + systemId + "]: " + resourceLocation);
        return null;
      }
      XMLInputSource source = new XMLInputSource(null, systemId, null);
      source.setByteStream(is);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Found XML schema [" + systemId + "] in classpath: " + resourceLocation);
      }
      return source;
    } else {
      return null;
    }
  }

  public XMLGrammarPool getXmlGrammarPool() {
    return xmlGrammarPool.get();
  }
}
