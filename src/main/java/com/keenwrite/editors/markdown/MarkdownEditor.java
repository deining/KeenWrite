/* Copyright 2020 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.editors.markdown;

import com.keenwrite.Constants;
import com.keenwrite.editors.TextEditor;
import com.keenwrite.io.File;
import com.keenwrite.processors.markdown.CaretPosition;
import com.keenwrite.spelling.impl.TextEditorSpeller;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.Nodes;

import java.nio.charset.Charset;
import java.text.BreakIterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.keenwrite.Constants.DEFAULT_DOCUMENT;
import static com.keenwrite.Constants.STYLESHEET_MARKDOWN;
import static com.keenwrite.StatusBarNotifier.clue;
import static java.lang.Character.isWhitespace;
import static java.lang.String.format;
import static javafx.scene.control.ScrollPane.ScrollBarPolicy.ALWAYS;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;

/**
 * Responsible for editing Markdown documents.
 */
public class MarkdownEditor extends BorderPane implements TextEditor {
  private static final String NEWLINE = System.lineSeparator();

  /**
   * Regular expression that matches the type of markup block. This is used
   * when Enter is pressed to continue the block environment.
   */
  private static final Pattern PATTERN_AUTO_INDENT = Pattern.compile(
      "(\\s*[*+-]\\s+|\\s*[0-9]+\\.\\s+|\\s+)(.*)" );

  /**
   * The text editor.
   */
  private final StyleClassedTextArea mTextArea =
      new StyleClassedTextArea( false );

  /**
   * Wraps the text editor in scrollbars.
   */
  private final VirtualizedScrollPane<StyleClassedTextArea> mScrollPane =
      new VirtualizedScrollPane<>( mTextArea );

  /**
   * File being edited by this editor instance.
   */
  private File mFile;

  /**
   * Set to {@code true} upon text or caret position changes. Value is {@code
   * false} by default.
   */
  private final BooleanProperty mDirty = new SimpleBooleanProperty();

  /**
   * Opened file's character encoding, or {@link Constants#DEFAULT_CHARSET} if
   * either no encoding could be determined or this is a new (empty) file.
   */
  private final Charset mEncoding;

  /**
   * Tracks whether the in-memory definitions have changed with respect to the
   * persisted definitions.
   */
  private final BooleanProperty mModified = new SimpleBooleanProperty();

  public MarkdownEditor() {
    this( DEFAULT_DOCUMENT );
  }

  public MarkdownEditor( final File file ) {
    mEncoding = open( mFile = file );

    initTextArea( mTextArea );
    initScrollPane( mScrollPane );
    initSpellchecker( mTextArea );
    initHotKeys();
    initUndoManager();
  }

  private void initTextArea( final StyleClassedTextArea textArea ) {
    textArea.setWrapText( true );
    textArea.getStyleClass().add( "markdown" );
    textArea.getStylesheets().add( STYLESHEET_MARKDOWN );
    textArea.requestFollowCaret();
    textArea.moveTo( 0 );

    textArea.textProperty().addListener( ( c, o, n ) -> {
      // Fire, regardless of whether the caret position has changed.
      mDirty.set( false );

      // Prevent a caret position change from raising the dirty bits.
      mDirty.set( true );
    } );
    textArea.caretPositionProperty().addListener( ( c, o, n ) -> {
      // Fire when the caret position has changed and the text has not.
      mDirty.set( true );
      mDirty.set( false );
    } );
  }

  private void initScrollPane(
      final VirtualizedScrollPane<StyleClassedTextArea> scrollpane ) {
    scrollpane.setVbarPolicy( ALWAYS );
    setCenter( scrollpane );
  }

  private void initSpellchecker( final StyleClassedTextArea textarea ) {
    final var speller = new TextEditorSpeller();
    speller.checkDocument( textarea );
    speller.checkParagraphs( textarea );
  }

  private void initHotKeys() {
    addListener( keyPressed( ENTER ), this::onEnterPressed );
    addListener( keyPressed( X, CONTROL_DOWN ), this::cut );
    addListener( keyPressed( TAB ), this::tab );
    addListener( keyPressed( TAB, SHIFT_DOWN ), this::untab );
  }

  private void initUndoManager() {
    final var undoManager = getUndoManager();
    final var markedPosition = undoManager.atMarkedPositionProperty();
    undoManager.forgetHistory();
    undoManager.mark();

    mModified.bind( Bindings.not( markedPosition ) );
  }

  /**
   * Delegate the focus request to the text area itself.
   */
  @Override
  public void requestFocus() {
    mTextArea.requestFocus();
  }

  @Override
  public void setText( final String text ) {
    mTextArea.clear();
    mTextArea.appendText( text );
    mTextArea.getUndoManager().mark();
  }

  @Override
  public String getText() {
    return mTextArea.getText();
  }

  @Override
  public Charset getEncoding() {
    return mEncoding;
  }

  @Override
  public File getFile() {
    return mFile;
  }

  @Override
  public void rename( final File file ) {
    mFile = file;
  }

  @Override
  public void undo() {
    final var manager = getUndoManager();
    doit( manager::isUndoAvailable, manager::undo, "Main.status.error.undo" );
  }

  @Override
  public void redo() {
    final var manager = getUndoManager();
    doit( manager::isRedoAvailable, manager::redo, "Main.status.error.redo" );
  }

  /**
   * Performs an undo or redo action, if possible, otherwise displays an error
   * message to the user.
   *
   * @param ready  Answers whether the action can be executed.
   * @param action The action to execute.
   * @param key    The informational message key having a value to display if
   *               the {@link Supplier} is not ready.
   */
  private void doit(
      final Supplier<Boolean> ready, final Runnable action, final String key ) {
    if( ready.get() ) {
      action.run();
    }
    else {
      clue( key );
    }
  }

  @Override
  public void cut() {
    final var selected = mTextArea.getSelectedText();

    if( selected == null || selected.isEmpty() ) {
      mTextArea.selectLine();
    }

    mTextArea.cut();
  }

  @Override
  public void copy() {
    mTextArea.copy();
  }

  @Override
  public void paste() {
    mTextArea.paste();
  }

  @Override
  public void selectAll() {
    mTextArea.selectAll();
  }

  @Override
  public void bold() {
    enwrap( "**" );
  }

  @Override
  public void italic() {
    enwrap( "*" );
  }

  @Override
  public void superscript() {
    enwrap( "^" );
  }

  @Override
  public void subscript() {
    enwrap( "~" );
  }

  @Override
  public void strikethrough() {
    enwrap( "~~" );
  }

  @Override
  public void blockquote() {
    block( "> " );
  }

  @Override
  public void code() {
    enwrap( "`" );
  }

  @Override
  public void fencedCodeBlock() {
    final var key = "App.action.insert.fenced_code_block.prompt.text";

    // TODO: Introduce sample text if nothing is selected.
    //enwrap( "\n\n```\n", "\n```\n\n", get( key ) );
  }

  @Override
  public void heading( final int level ) {
    final var hashes = new String( new char[ level ] ).replace( "\0", "#" );
    final var markup = format( "%s ", hashes );

    block( markup );
  }

  @Override
  public void unorderedList() {
    block( "* " );
  }

  @Override
  public void orderedList() {
    block( "1. " );
  }

  @Override
  public void horizontalRule() {
    block( format( "---%n%n" ) );
  }

  @Override
  public Node getNode() {
    return this;
  }

  @Override
  public ReadOnlyBooleanProperty modifiedProperty() {
    return mModified;
  }

  @Override
  public void clearModifiedProperty() {
    getUndoManager().mark();
  }

  @Override
  public VirtualizedScrollPane<StyleClassedTextArea> getScrollPane() {
    return mScrollPane;
  }

  /**
   * This method adds listeners to editor events.
   *
   * @param <T>      The event type.
   * @param <U>      The consumer type for the given event type.
   * @param event    The event of interest.
   * @param consumer The method to call when the event happens.
   */
  private <T extends Event, U extends T> void addListener(
      final EventPattern<? super T, ? extends U> event,
      final Consumer<? super U> consumer ) {
    Nodes.addInputMap( mTextArea, consume( event, consumer ) );
  }

  @SuppressWarnings("unused")
  private void onEnterPressed( final KeyEvent event ) {
    final var currentLine = getCaretParagraph();
    final var matcher = PATTERN_AUTO_INDENT.matcher( currentLine );

    // By default, insert a new line by itself.
    String newText = NEWLINE;

    // If the pattern was matched then determine what block type to continue.
    if( matcher.matches() ) {
      if( matcher.group( 2 ).isEmpty() ) {
        final var pos = mTextArea.getCaretPosition();
        mTextArea.selectRange( pos - currentLine.length(), pos );
      }
      else {
        // Indent the new line with the same whitespace characters and
        // list markers as current line. This ensures that the indentation
        // is propagated.
        newText = newText.concat( matcher.group( 1 ) );
      }
    }

    mTextArea.replaceSelection( newText );
  }

  private void cut( final KeyEvent event ) {
    cut();
  }

  private void tab( final KeyEvent event ) {
    final var range = mTextArea.selectionProperty().getValue();
    final var sb = new StringBuilder( 1024 );

    if( range.getLength() > 0 ) {
      final var selection = mTextArea.getSelectedText();

      selection.lines().forEach(
          ( l ) -> sb.append( "\t" ).append( l ).append( NEWLINE )
      );
    }
    else {
      sb.append( "\t" );
    }

    mTextArea.replaceSelection( sb.toString() );
  }

  private void untab( final KeyEvent event ) {
    final var range = mTextArea.selectionProperty().getValue();

    if( range.getLength() > 0 ) {
      final var selection = mTextArea.getSelectedText();
      final var sb = new StringBuilder( selection.length() );

      selection.lines().forEach(
          ( l ) -> sb.append( l.startsWith( "\t" ) ? l.substring( 1 ) : l )
                     .append( NEWLINE )
      );

      mTextArea.replaceSelection( sb.toString() );
    }
    else {
      final var p = getCaretParagraph();

      if( p.startsWith( "\t" ) ) {
        mTextArea.selectParagraph();
        mTextArea.replaceSelection( p.substring( 1 ) );
      }
    }
  }

  /**
   * Observers may listen for changes to the property returned from this method
   * to receive notifications when either the text or caret have changed.
   * This should not be used to track whether the text has been modified.
   */
  public void addDirtyListener( ChangeListener<Boolean> listener ) {
    mDirty.addListener( listener );
  }

  public CaretPosition createCaretPosition() {
    return CaretPosition
        .builder()
        .with( CaretPosition.Mutator::setEditor, mTextArea )
        .build();
  }

  /**
   * Surrounds the selected text or word under the caret in Markdown markup.
   *
   * @param token The beginning and ending token for enclosing the text.
   */
  private void enwrap( final String token ) {
    enwrap( token, token );
  }

  /**
   * Surrounds the selected text or word under the caret in Markdown markup.
   *
   * @param began The beginning token for enclosing the text.
   * @param ended The ending token for enclosing the text.
   */
  private void enwrap( final String began, String ended ) {
    final var range = mTextArea.getSelection();
    String text = mTextArea.getText(
        range.getLength() == 0 ? getCaretWord() : range
    );

    int length = range.getLength();
    text = stripStart( text, null );
    final int beganIndex = range.getStart() + (length - text.length());

    length = text.length();
    text = stripEnd( text, null );
    final int endedIndex = range.getEnd() - (length - text.length());

    mTextArea.replaceText( beganIndex, endedIndex, began + text + ended );
  }

  /**
   * Inserts the given block-level markup at the current caret position
   * within the document. This will prepend two blank lines to ensure that
   * the block element begins at the start of a new line.
   *
   * @param markup The text to insert at the caret.
   */
  private void block( final String markup ) {
    final int pos = mTextArea.getCaretPosition();
    mTextArea.insertText( pos, format( "%n%n%s", markup ) );
  }

  /**
   * Returns the caret position within the current paragraph.
   *
   * @return A value from 0 to the length of the current paragraph.
   */
  private int getCaretColumn() {
    return mTextArea.getCaretColumn();
  }

  /**
   * Finds the start and end indexes for the word in the current paragraph
   * where the caret is located. There are a few different scenarios, where
   * the caret can be at: the start, end, or middle of a word; also, the
   * caret can be at the end or beginning of a punctuated word; as well, the
   * caret could be at the beginning or end of the line or document.
   *
   * @return The
   */
  private IndexRange getCaretWord() {
    final var paragraph = getCaretParagraph();
    final var length = paragraph.length();
    final var column = getCaretColumn();

    var began = column;
    var ended = column;

    while( began > 0 && !isWhitespace( paragraph.charAt( began - 1 ) ) ) {
      began--;
    }

    while( ended < length && !isWhitespace( paragraph.charAt( ended ) ) ) {
      ended++;
    }

    final var iterator = BreakIterator.getWordInstance();
    iterator.setText( paragraph );

    while( began < length && iterator.isBoundary( began + 1 ) ) {
      began++;
    }

    while( ended > 0 && iterator.isBoundary( ended - 1 ) ) {
      ended--;
    }

    final var offset = getCaretDocumentOffset( column );

    return IndexRange.normalize( began + offset, ended + offset );
  }

  private int getCaretDocumentOffset( final int column ) {
    return mTextArea.getCaretPosition() - column;
  }

  /**
   * Returns the index of the paragraph where the caret resides.
   *
   * @return A number greater than or equal to 0.
   */
  private int getCurrentParagraph() {
    return mTextArea.getCurrentParagraph();
  }

  /**
   * Returns the text for the paragraph that contains the caret.
   *
   * @return A non-null string, possibly empty.
   */
  private String getCaretParagraph() {
    return getText( getCurrentParagraph() );
  }

  private String getText( final int paragraph ) {
    return mTextArea.getText( paragraph );
  }

  private UndoManager<?> getUndoManager() {
    return mTextArea.getUndoManager();
  }
}
