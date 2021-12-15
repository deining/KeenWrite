/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.processors;

import com.keenwrite.typesetting.Typesetter;

import java.io.IOException;

import static com.keenwrite.Bootstrap.APP_TITLE_LOWERCASE;
import static com.keenwrite.events.StatusEvent.clue;
import static com.keenwrite.io.MediaType.TEXT_XML;
import static com.keenwrite.preferences.WorkspaceKeys.*;
import static com.keenwrite.typesetting.Typesetter.Mutator;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.writeString;

/**
 * Responsible for using a typesetting engine to convert an XHTML document
 * into a PDF file. This must not be run from the JavaFX thread.
 */
public final class PdfProcessor extends ExecutorProcessor<String> {
  private final ProcessorContext mContext;

  public PdfProcessor( final ProcessorContext context ) {
    assert context != null;
    mContext = context;
  }

  /**
   * Converts a document by calling a third-party library to typeset the given
   * XHTML document.
   *
   * @param xhtml The document to convert to a PDF file.
   * @return {@code null} because there is no valid return value from generating
   * a PDF file.
   */
  public String apply( final String xhtml ) {
    try {
      clue( "Main.status.typeset.create" );
      final var workspace = mContext.getWorkspace();
      final var document = TEXT_XML.createTemporaryFile( APP_TITLE_LOWERCASE );
      final var typesetter = Typesetter
        .builder()
        .with( Mutator::setInputPath,
               writeString( document, xhtml ) )
        .with( Mutator::setOutputPath,
               mContext.getOutputPath() )
        .with( Mutator::setThemePath,
               workspace.toFile( KEY_TYPESET_CONTEXT_THEMES_PATH ) )
        .with( Mutator::setThemeName,
               workspace.toString( KEY_TYPESET_CONTEXT_THEME_SELECTION ) )
        .with( Mutator::setAutoclean,
               workspace.toBoolean( KEY_TYPESET_CONTEXT_CLEAN ) )
        .build();

      typesetter.typeset();

      // Smote the temporary file after typesetting the document.
      if( typesetter.autoclean() ) {
        deleteIfExists( document );
      }
      else {
        document.toFile().deleteOnExit();
      }
    } catch( final IOException | InterruptedException ex ) {
      // Typesetter runtime exceptions will pass up the call stack.
      clue( "Main.status.typeset.failed", ex );
    }

    // Do not continue processing (the document was typeset into a binary).
    return null;
  }
}
