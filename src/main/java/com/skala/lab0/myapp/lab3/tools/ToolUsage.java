package com.skala.lab0.myapp.lab3.tools;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ToolUsage {
  public static final String CONTEXT_KEY = "toolUsage";
  private final AtomicBoolean used = new AtomicBoolean();
  private final AtomicBoolean writeUsed = new AtomicBoolean();

  public void markUsed() { used.set(true); }
  public boolean wasUsed() { return used.get(); }
  public void markWriteUsed() { used.set(true); writeUsed.set(true); }
  public boolean wasWriteUsed() { return writeUsed.get(); }
}
