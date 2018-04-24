/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.mule.api.annotation.NoInstantiate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ConfigResource holds the url description (or location) and the url stream. It is useful to associate the two for error
 * reporting when the stream cannot be read.
 */
@NoInstantiate
public class ConfigResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigResource.class);

  protected String resourceName;
  private URL url;
  private InputStream inputStream;

  public ConfigResource(String resourceName) throws IOException {
    this.resourceName = resourceName;
    url = getResourceAsUrl(resourceName, getClass(), true, true);
    if (url == null) {
      throw new FileNotFoundException(resourceName);
    }
  }

  public ConfigResource(URL url) {
    this.url = url;
    this.resourceName = url.toExternalForm();
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

  public int hashCode() {
    int result = 17;
    result = 31 * result + (resourceName != null ? resourceName.hashCode() : 0);
    return result;
  }


  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ConfigResource");
    sb.append("{resourceName='").append(resourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }

  /**
   * TODO method copied from IOUtils, see what to do with this.
   */
  private static URL getResourceAsUrl(final String resourceName, final Class callingClass, boolean tryAsFile, boolean tryAsUrl) {
    if (resourceName == null) {
      throw new IllegalArgumentException();
    }
    URL url = null;

    // Try to load the resource from the file system.
    if (tryAsFile) {
      try {
        File file = new File(resourceName);
        if (file.exists()) {
          url = file.getAbsoluteFile().toURL();
        } else {
          LOGGER.debug("Unable to load resource from the file system: " + file.getAbsolutePath());
        }
      } catch (Exception e) {
        LOGGER.debug("Unable to load resource from the file system: " + e.getMessage());
      }
    }

    // Try to load the resource from the classpath.
    if (url == null) {
      try {
        url = (URL) AccessController.doPrivileged((PrivilegedAction) () -> getResource(resourceName, callingClass));
        if (url == null) {
          LOGGER.debug("Unable to load resource " + resourceName + " from the classpath");
        }
      } catch (Exception e) {
        LOGGER.debug("Unable to load resource " + resourceName + " from the classpath: " + e.getMessage());
      }
    }

    if (url == null) {
      try {
        url = new URL(resourceName);
      } catch (MalformedURLException e) {
        // ignore
      }
    }
    return url;
  }

  /**
   * TODO copied from ClassUtils
   */
  public static URL getResource(final String resourceName, final Class<?> callingClass) {
    URL url = AccessController.doPrivileged((PrivilegedAction<URL>) () -> {
      final ClassLoader cl = Thread.currentThread().getContextClassLoader();
      return cl != null ? cl.getResource(resourceName) : null;
    });

    if (url == null) {
      url = AccessController
          .doPrivileged((PrivilegedAction<URL>) () -> ConfigResource.class.getClassLoader().getResource(resourceName));
    }

    if (url == null) {
      url = AccessController.doPrivileged((PrivilegedAction<URL>) () -> callingClass.getClassLoader().getResource(resourceName));
    }

    return url;
  }
}
