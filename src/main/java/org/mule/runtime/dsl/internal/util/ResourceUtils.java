/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal.util;

import java.net.URLConnection;

/**
 * Utility methods for resolving resource locations to files in the file system
 *
 * @since 1.4.0
 */
public class ResourceUtils {

  private ResourceUtils() {}

  /**
   * Set the {@link URLConnection#setUseCaches "useCaches"} flag on the given connection, preferring {@code false} but leaving the
   * flag at {@code true} for JNLP based resources.
   * 
   * @param con the URLConnection to set the flag on
   */
  public static void useCachesIfNecessary(URLConnection con) {
    con.setUseCaches(con.getClass().getSimpleName().startsWith("JNLP"));
  }
}
