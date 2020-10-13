/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml.parser;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.dsl.api.xml.XmlDslConstants.IMPORT_ELEMENT;
import static org.mule.runtime.dsl.internal.xerces.xni.parser.DefaultXmlGrammarPoolManager.getGrammarPool;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.dsl.internal.xml.parser.XmlApplicationParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.ArrayUtils;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class XmlConfigurationProcessor {


  public static List<ConfigFile> processXmlConfiguration(XmlParsingConfiguration parsingConfiguration) {
    List<ConfigFile> configFiles = new ArrayList<>();
    if (!ArrayUtils.isEmpty(parsingConfiguration.getArtifactConfigResources())) {
      List<Pair<String, Supplier<InputStream>>> initialConfigFiles = new ArrayList<>();
      for (ConfigResource artifactConfigResource : parsingConfiguration.getArtifactConfigResources()) {
        initialConfigFiles.add(new Pair<>(artifactConfigResource.getResourceName(), () -> {
          try {
            return artifactConfigResource.getInputStream();
          } catch (IOException e) {
            throw new MuleRuntimeException(e);
          }
        }));
      }


      recursivelyResolveConfigFiles(initialConfigFiles, configFiles, parsingConfiguration)
          .forEach(configFiles::add);
    }
    return unmodifiableList(configFiles);
  }

  private static List<ConfigFile> recursivelyResolveConfigFiles(List<Pair<String, Supplier<InputStream>>> configFilesToResolve,
                                                                List<ConfigFile> alreadyResolvedConfigFiles,
                                                                XmlParsingConfiguration parsingConfiguration) {

    ImmutableList.Builder<ConfigFile> resolvedConfigFilesBuilder =
        ImmutableList.<ConfigFile>builder().addAll(alreadyResolvedConfigFiles);
    configFilesToResolve.stream()
        .filter(fileNameInputStreamPair -> !alreadyResolvedConfigFiles.stream()
            .anyMatch(configFile -> configFile.getFilename().equals(fileNameInputStreamPair.getFirst())))
        .forEach(fileNameInputStreamPair -> {
          InputStream is = null;
          try {
            is = fileNameInputStreamPair.getSecond().get();
            Document document = parsingConfiguration.getXmlConfigurationDocumentLoader()
                .loadDocument(parsingConfiguration.getSaxParserFactory(), parsingConfiguration.getEntityResolver(),
                              fileNameInputStreamPair.getFirst(), fileNameInputStreamPair.getSecond().get(),
                              getGrammarPool().orElse(null));
            ConfigLine mainConfigLine = new XmlApplicationParser(parsingConfiguration.getXmlNamespaceInfoProvider())
                .parse(document.getDocumentElement()).get();
            ConfigFile configFile = new ConfigFile(fileNameInputStreamPair.getFirst(), asList(mainConfigLine));
            resolvedConfigFilesBuilder.add(configFile);
          } finally {
            if (is != null) {
              try {
                is.close();
              } catch (IOException e) {
                throw new MuleRuntimeException(e);
              }
            }
          }
        });

    ImmutableSet.Builder<String> importedFiles = ImmutableSet.builder();
    for (ConfigFile configFile : resolvedConfigFilesBuilder.build()) {
      List<ConfigLine> rootConfigLines = configFile.getConfigLines();
      ConfigLine muleRootElementConfigLine = rootConfigLines.get(0);
      importedFiles.addAll(muleRootElementConfigLine.getChildren().stream()
          .filter(configLine -> CORE_NAMESPACE.equals(configLine.getNamespaceUri())
              && IMPORT_ELEMENT.equals(configLine.getIdentifier()))
          .map(configLine -> {
            SimpleConfigAttribute fileConfigAttribute = configLine.getConfigAttributes().get("file");
            if (fileConfigAttribute == null) {
              throw new MuleRuntimeException(
                                             createStaticMessage(format("<import> does not have a file attribute defined. At file '%s', at line %s",
                                                                        configFile.getFilename(),
                                                                        configLine.getLineNumber())));
            }
            return fileConfigAttribute.getValue();
          })
          .map(value -> parsingConfiguration.getParsingPropertyResolver().resolveProperty(value))
          .filter(fileName -> !alreadyResolvedConfigFiles.stream()
              .anyMatch(solvedConfigFile -> solvedConfigFile.getFilename().equals(fileName)))
          .collect(toList()));
    }

    Set<String> importedConfigurationFiles = importedFiles.build();

    if (importedConfigurationFiles.isEmpty()) {
      return resolvedConfigFilesBuilder.build();
    }

    List<Pair<String, Supplier<InputStream>>> newConfigFilesToResolved = importedConfigurationFiles.stream()
        .map(importedFileName -> {
          Supplier<InputStream> inputStreamSupplier = () -> {
            try {
              return parsingConfiguration
                  .getResourceLocator()
                  .find(importedFileName, new XmlConfigurationProcessor())
                  .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format("Could not find imported resource '%s'",
                                                                                         importedFileName))))
                  .openStream();
            } catch (IOException e) {
              throw new MuleRuntimeException(e);
            }
          };
          return (Pair<String, Supplier<InputStream>>) new Pair(importedFileName,
                                                                inputStreamSupplier);
        })
        .collect(toList());

    return recursivelyResolveConfigFiles(newConfigFilesToResolved, resolvedConfigFilesBuilder.build(), parsingConfiguration);
  }

}
