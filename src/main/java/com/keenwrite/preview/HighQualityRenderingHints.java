/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.preview;

import java.util.HashMap;
import java.util.Map;

import static java.awt.RenderingHints.*;
import static java.awt.Toolkit.getDefaultToolkit;

/**
 * Responsible for initializing settings to produce high-quality image
 * transformations.
 */
@SuppressWarnings( "rawtypes" )
public class HighQualityRenderingHints {
  /**
   * Default hints for high-quality rendering that may be changed by
   * the system's rendering hints.
   */
  private static final Map<Object, Object> DEFAULT_HINTS = Map.of(
    KEY_ANTIALIASING, VALUE_ANTIALIAS_ON,
    KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY,
    KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY,
    KEY_DITHERING, VALUE_DITHER_DISABLE,
    KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON,
    KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC,
    KEY_RENDERING, VALUE_RENDER_QUALITY,
    KEY_STROKE_CONTROL, VALUE_STROKE_PURE,
    KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON
  );

  /**
   * Shared hints for high-quality rendering.
   */
  static final Map<Object, Object> RENDERING_HINTS = new HashMap<>(
    DEFAULT_HINTS
  );

  static {
    initializeRenderingHints();
  }

  private static void initializeRenderingHints() {
    final var toolkit = getDefaultToolkit();
    final var hints = toolkit.getDesktopProperty( "awt.font.desktophints" );

    if( hints instanceof final Map map ) {
      for( final var key : map.keySet() ) {
        final var hint = map.get( key );
        RENDERING_HINTS.put( key, hint );
      }
    }
  }

  /**
   * Defines a reusable constant, nothing more.
   */
  private HighQualityRenderingHints() {
  }
}
