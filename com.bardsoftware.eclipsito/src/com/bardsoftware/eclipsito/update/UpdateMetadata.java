// Copyright (C) 2019 BarD Software
package com.bardsoftware.eclipsito.update;

/**
 * @author dbarashev@bardsoftware.com
 */
public class UpdateMetadata {
  public final String version;
  public final String url;
  public final String description;

  public UpdateMetadata(String version, String url, String description) {
    this.version = version;
    this.url = url;
    this.description = description;
  }
}