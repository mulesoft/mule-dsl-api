/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api;

import static org.mule.runtime.api.util.IOUtils.getResourceAsUrl;

import static java.lang.System.getProperty;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * A ConfigResource holds the url description (or location) and the url stream. It is useful to associate the two for error
 * reporting when the stream cannot be read.
 */
@NoExtend
@NoInstantiate
public final class ConfigResource {

  private static final List<String> CLASS_PATH_ENTRIES;
  private static final boolean isWindows = getProperty("os.name").toLowerCase().contains("windows");
  private static Pattern JAR_FILE_FROM_RESOURCE_PATTERN = compile("^jar:file:(.*[.]jar)!/.*");

  static {
    String classPath = getProperty("java.class.path");
    String modulePath = getProperty("jdk.module.path");
    String pathSeparator = getProperty("path.separator");

    List<String> allClassPathEntries = (modulePath != null
        ? concat(Stream.of(classPath.split(pathSeparator)),
                 Stream.of(modulePath.split(pathSeparator)))
        : Stream.of(classPath.split(pathSeparator)))
            .filter(StringUtils::isNotBlank)
            .collect(toList());
    CLASS_PATH_ENTRIES = allClassPathEntries.stream().map(line -> line.replace("\\", "/")).collect(toList());
  }

  protected String resourceName;
  private URL url;
  private InputStream inputStream;
  private long lastModifiedDate = 0L;

  public ConfigResource(String resourceName) throws IOException {
    this(resourceName, getResourceAsUrl(resourceName, ConfigResource.class, true, true));
  }

  public ConfigResource(URL url) {
    this.url = url;

    if (url.getProtocol().equals("jar")) {
      this.resourceName = url.toExternalForm().split("!/")[1];
    } else if (url.getProtocol().equals("file")) {
      String updatedUrl = isWindows && url.getPath().startsWith("/") ? url.getPath().substring(1) : url.getPath();
      this.resourceName = CLASS_PATH_ENTRIES.stream()
          .filter(cp -> updatedUrl.startsWith(cp)).findAny()
          .map(cp -> updatedUrl.substring(cp.length() + 1))
          .orElse(url.toExternalForm());
    } else {
      this.resourceName = url.toExternalForm();
    }
  }

  /**
   * @since 1.5
   */
  public ConfigResource(String resourceName, URL url) throws IOException {
    this.resourceName = resourceName;
    if (url == null) {
      throw new FileNotFoundException(resourceName);
    }
    this.url = url;
  }

  public ConfigResource(String resourceName, InputStream inputStream) {
    this(resourceName, inputStream, 0L);
  }

  public ConfigResource(String resourceName, InputStream inputStream, long lastModifiedDate) {
    this.resourceName = resourceName;
    this.inputStream = inputStream;
    this.lastModifiedDate = lastModifiedDate;
  }

  public InputStream getInputStream() throws IOException {
    if (inputStream == null && url != null) {
      URLConnection urlConnection = url.openConnection();
      // It's necessary to disable connection caching when working with jar files
      // in order to avoid file leaks in Windows environments
      if (urlConnection instanceof JarURLConnection) {
        urlConnection.setUseCaches(false);
      }
      inputStream = urlConnection.getInputStream();
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

  /**
   * @return the {@link File#lastModified()} of the file from where this resource is loaded, or {@code 0L} if there is no file.
   * 
   * @since 1.8
   */
  public long getLastModified() {
    if (getUrl() == null) {
      return lastModifiedDate;
    }

    if (getUrl().toString().startsWith("jar:file:")) {
      Matcher matcher = JAR_FILE_FROM_RESOURCE_PATTERN.matcher(getUrl().toString());
      if (matcher.matches()) {
        String path = matcher.group(1);
        return new File(path).lastModified();
      } else {
        return 0L;
      }
    }

    try {
      return new File(getUrl().toURI()).lastModified();
    } catch (URISyntaxException e) {
      return 0L;
    }
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
