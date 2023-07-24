/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * Module with the API to define how to build runtime objects from configuration files.
 * 
 * @moduleGraph
 * @since 1.5
 */
module org.mule.runtime.dsl.api {

  requires org.mule.runtime.api;

  requires java.xml;
  
  requires com.google.common;
  requires org.apache.commons.io;
  requires org.apache.commons.lang3;
  requires org.mule.apache.xerces;
  
  exports org.mule.runtime.dsl.api;
  exports org.mule.runtime.dsl.api.component;
  exports org.mule.runtime.dsl.api.component.config;
  exports org.mule.runtime.dsl.api.xml;
  exports org.mule.runtime.dsl.api.xml.parser;

  uses org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
  uses org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

  exports org.mule.runtime.dsl.internal.component.config to
      org.mule.runtime.metadata.support,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.dsl.internal.util to
      org.mule.runtime.artifact.ast.xmlParser,
      org.mule.runtime.artifact.ast.xmlParser.test;
  exports org.mule.runtime.dsl.internal.xerces.xni.parser to
      org.mule.runtime.artifact.ast.xmlParser;
  exports org.mule.runtime.dsl.internal.xml.parser to
      org.mule.runtime.artifact.ast.xmlParser;

  // Location objects referenced in events
  opens org.mule.runtime.dsl.api.component.config to
      kryo.shaded;

}
