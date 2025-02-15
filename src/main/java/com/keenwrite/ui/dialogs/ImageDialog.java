/*
 * Copyright 2015 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.keenwrite.ui.dialogs;

import static com.keenwrite.Messages.get;
import com.keenwrite.ui.controls.BrowseFileButton;
import com.keenwrite.ui.controls.EscapeTextField;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ButtonBar.ButtonData;
import static javafx.scene.control.ButtonType.OK;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import org.tbee.javafx.scene.layout.fxml.MigPane;

/**
 * Dialog to enter a Markdown image.
 */
public class ImageDialog extends AbstractDialog<String> {

  private final StringProperty image = new SimpleStringProperty();

  public ImageDialog( final Window owner, final Path basePath ) {
    super(owner, "Dialog.image.title" );
    
    final DialogPane dialogPane = getDialogPane();
    dialogPane.setContent( pane );

    linkBrowseFileButton.setBasePath( basePath );
    linkBrowseFileButton.addExtensionFilter( new ExtensionFilter( get( "Dialog.image.chooser.imagesFilter" ), "*.png", "*.gif", "*.jpg" ) );
    linkBrowseFileButton.urlProperty().bindBidirectional( urlField.escapedTextProperty() );

    dialogPane.lookupButton( OK ).disableProperty().bind(
      urlField.escapedTextProperty().isEmpty()
      .or( textField.escapedTextProperty().isEmpty() ) );

    image.bind( Bindings.when( titleField.escapedTextProperty().isNotEmpty() )
      .then( Bindings.format( "![%s](%s \"%s\")", textField.escapedTextProperty(), urlField.escapedTextProperty(), titleField.escapedTextProperty() ) )
      .otherwise( Bindings.format( "![%s](%s)", textField.escapedTextProperty(), urlField.escapedTextProperty() ) ) );
    previewField.textProperty().bind( image );

    setResultConverter( dialogButton -> {
      ButtonData data = dialogButton != null ? dialogButton.getButtonData() : null;
      return data == ButtonData.OK_DONE ? image.get() : null;
    } );

    Platform.runLater( () -> {
      urlField.requestFocus();

      if( urlField.getText().startsWith( "http://" ) ) {
        urlField.selectRange( "http://".length(), urlField.getLength() );
      }
    } );
  }

  @Override
  protected void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    pane = new MigPane();
    Label urlLabel = new Label();
    urlField = new EscapeTextField();
    linkBrowseFileButton = new BrowseFileButton();
    Label textLabel = new Label();
    textField = new EscapeTextField();
    Label titleLabel = new Label();
    titleField = new EscapeTextField();
    Label previewLabel = new Label();
    previewField = new Label();

    //======== pane ========
    {
      pane.setCols( "[shrink 0,fill][300,grow,fill][fill]" );
      pane.setRows( "[][][][]" );

      //---- urlLabel ----
      urlLabel.setText( get( "Dialog.image.urlLabel.text" ) );
      pane.add( urlLabel, "cell 0 0" );

      //---- urlField ----
      urlField.setEscapeCharacters( "()" );
      urlField.setText( "https://yourlink.com" );
      urlField.setPromptText( "https://yourlink.com" );
      pane.add( urlField, "cell 1 0" );
      pane.add( linkBrowseFileButton, "cell 2 0" );

      //---- textLabel ----
      textLabel.setText( get( "Dialog.image.textLabel.text" ) );
      pane.add( textLabel, "cell 0 1" );

      //---- textField ----
      textField.setEscapeCharacters( "[]" );
      pane.add( textField, "cell 1 1 2 1" );

      //---- titleLabel ----
      titleLabel.setText( get( "Dialog.image.titleLabel.text" ) );
      pane.add( titleLabel, "cell 0 2" );
      pane.add( titleField, "cell 1 2 2 1" );

      //---- previewLabel ----
      previewLabel.setText( get( "Dialog.image.previewLabel.text" ) );
      pane.add( previewLabel, "cell 0 3" );
      pane.add( previewField, "cell 1 3 2 1" );
    }
    // JFormDesigner - End of component initialization  //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
  private MigPane pane;
  private EscapeTextField urlField;
  private BrowseFileButton linkBrowseFileButton;
  private EscapeTextField textField;
  private EscapeTextField titleField;
  private Label previewField;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
