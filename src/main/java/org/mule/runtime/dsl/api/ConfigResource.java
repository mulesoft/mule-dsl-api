/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api;

import static java.util.Arrays.asList;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.util.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * A ConfigResource holds the url description (or location) and the url stream. It is useful to associate the two for error
 * reporting when the stream cannot be read.
 */
@NoExtend
@NoInstantiate
public final class ConfigResource {

  private static final List<String> CLASS_PATH_ENTRIES;

  static {
    CLASS_PATH_ENTRIES = asList(System.getProperty("java.class.path").split(":"));
  }

  protected String resourceName;
  private URL url;
  private InputStream inputStream;

  public ConfigResource(String resourceName) throws IOException {
    this.resourceName = resourceName;
    url = IOUtils.getResourceAsUrl(resourceName, getClass(), true, true);
    if (url == null) {
      throw new FileNotFoundException(resourceName);
    }
  }

  public ConfigResource(URL url) {
    this.url = url;

    if (url.getProtocol().equals("jar")) {
      this.resourceName = url.toExternalForm().split("!/")[1];
    } else if (url.getProtocol().equals("file")) {
      this.resourceName = CLASS_PATH_ENTRIES.stream()
          .filter(cp -> url.getPath().startsWith(cp)).findAny()
          .map(cp -> url.getPath().substring(cp.length() + 1))
          .orElse(url.toExternalForm());
    } else {
      this.resourceName = url.toExternalForm();
    }
  }

  public ConfigResource(String resourceName, InputStream inputStream) {
    this.inputStream = inputStream;
    this.resourceName = resourceName;
  }

  public InputStream getInputStream() throws IOException {
    if (inputStream == null && url != null) {
      inputStream = url.openStream();
    }
    return inputStream;
  }

  public URL getUrl() {
    return url;
  }

  public String getResourceName() {
    return resourceName;
  }

  public boolean isStreamOpen() {
    return inputStream != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConfigResource that = (ConfigResource) o;

    if (resourceName != null ? !resourceName.equals(that.resourceName) : that.resourceName != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + (resourceName != null ? resourceName.hashCode() : 0);
    return result;
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ConfigResource");
    sb.append("{resourceName='").append(resourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
