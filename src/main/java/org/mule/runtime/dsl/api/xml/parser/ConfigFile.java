/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml.parser;

import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;

import java.util.List;

/**
 * Represents an artifact configuration file and it's content in hierarchical format.
 *
 * A {@code ConfigFile} has a set of {@code ConfigLine} which represents the global definitions in the configuration file.
 *
 * Each {@code ConfigLine} may have nested {@code ConfigLine}s inside.
 *
 * @since 4.0
 * @deprecated From 4.4 onwards, use the {@code mule-artifact-ast-xml-parser} module.
 */
@NoExtend
@NoInstantiate
@Deprecated
public final class ConfigFile implements Comparable<ConfigFile> {

  private final String filename;
  private final List<ConfigLine> configLines;

  public ConfigFile(String filename, List<ConfigLine> configLines) {
    checkState(filename != null, "A config file must have a name");
    checkState(configLines != null, "A config file cannot have config lines");
    this.filename = filename;
    this.configLines = configLines;
  }

  /**
   * @return the configuration file name
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @return the configuration file lines as a list in the same order as they appear in the file.
   */
  public List<ConfigLine> getConfigLines() {
    return configLines;
  }

  @Override
  public int compareTo(ConfigFile o) {
    return filename.compareTo(o.filename);
  }
}
