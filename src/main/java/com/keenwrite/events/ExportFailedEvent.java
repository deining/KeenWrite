/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.events;

/**
 * Responsible for kicking off an alert message when exporting (e.g., to PDF)
 * fails. This can happen when the executable to typeset the document cannot
 * be found.
 */
public class ExportFailedEvent implements AppEvent {
  public static void fire() {
    new ExportFailedEvent().publish();
  }
}
