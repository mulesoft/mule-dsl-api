/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.dsl.internal;

public class SourcePosition implements Comparable<SourcePosition> {

  private int line;
  private int column;

  public SourcePosition() {
    this.line = 1;
    this.column = 1;
  }

  public SourcePosition(int line, int col) {
    this.line = line;
    this.column = col;
  }

  public int getLine() {
    return this.line;
  }

  public int getColumn() {
    return this.column;
  }

  public void setLine(int line) {
    this.line = line;
  }

  public void setColumn(int col) {
    this.column = col;
  }

  public int compareTo(SourcePosition o) {
    if (o.getLine() > this.getLine() ||
        (o.getLine() == this.getLine()
            && o.getColumn() > this.getColumn())) {
      return -1;
    } else if (o.getLine() == this.getLine() &&
        o.getColumn() == this.getColumn()) {
      return 0;
    } else {
      return 1;
    }
  }
}
