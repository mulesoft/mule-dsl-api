/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

import static org.mule.runtime.dsl.AllureConstants.ConfigResources.CONFIG_RESOURCES;
import static org.mule.runtime.dsl.AllureConstants.ConfigResources.LastModified.LAST_MODIFIED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

import org.mule.runtime.dsl.api.ConfigResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Story(CONFIG_RESOURCES)
@Feature(LAST_MODIFIED)
public class ConfigResourceTestCase {

  @Test
  public void regularFileLastUpdated() throws URISyntaxException {
    URL resource = this.getClass().getResource("/simple_application.xml");
    long expected = new File(resource.toURI()).lastModified();

    ConfigResource configResource = new ConfigResource(resource);

    assertThat(configResource.getLastModified(), is(expected));
  }

  @Test
  public void jarResourceLastUpdated() throws IOException, URISyntaxException {
    URL resource = this.getClass().getResource("/simple_application.jar");
    long expected = new File(resource.toURI()).lastModified();

    try (URLClassLoader simpleApplicationJarClassLoader =
        new URLClassLoader(new URL[] {resource})) {
      ConfigResource configResource =
          new ConfigResource(simpleApplicationJarClassLoader.getResource("simple_application_within_jar.xml"));

      assertThat(configResource.getLastModified(), is(expected));
    }
  }

  @Test
  public void noUrlLastUpdated() {
    ConfigResource configResource = new ConfigResource("simple_application", mock(InputStream.class));

    assertThat(configResource.getLastModified(), is(0L));
  }
}

