/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.editors;

import com.keenwrite.editors.definition.DefinitionEditor;
import com.keenwrite.editors.definition.DefinitionTreeItem;
import com.keenwrite.editors.markdown.MarkdownEditor;
import com.keenwrite.sigils.Sigils;
import javafx.scene.control.TreeItem;

import java.util.Map;

/**
 * Differentiates an instance of {@link TextResource} from an instance of
 * {@link DefinitionEditor} or {@link MarkdownEditor}.
 */
public interface TextDefinition extends TextResource {
  /**
   * Converts the definitions into a map, ready for interpolation.
   *
   * @return The list of key value pairs delimited with tokens.
   */
  Map<String, String> toMap();

  /**
   * Performs string interpolation on the values in the given map. This will
   * change any value in the map that contains a variable that matches
   * the definition regex pattern against the given {@link Sigils}.
   *
   * @param map Contains values that represent references to keys.
   * @param sigils The beginning and ending tokens that delimit variables.
   */
  Map<String, String> interpolate( Map<String, String> map, Sigils sigils );

  /**
   * Requests that the visual representation be expanded to the given
   * node.
   *
   * @param node Request expansion to this node.
   */
  <T> void expand( TreeItem<T> node );

  /**
   * Adds a new item to the definition hierarchy.
   */
  void createDefinition();

  /**
   * Edits the currently selected definition in the hierarchy.
   */
  void renameDefinition();

  /**
   * Removes the currently selected definition in the hierarchy.
   */
  void deleteDefinitions();

  /**
   * Finds the definition that exact matches the given text.
   *
   * @param text The value to find, never {@code null}.
   * @return The leaf that contains the given value.
   */
  DefinitionTreeItem<String> findLeafExact( String text );

  /**
   * Finds the definition that starts with the given text.
   *
   * @param text The value to find, never {@code null}.
   * @return The leaf that starts with the given value.
   */
  DefinitionTreeItem<String> findLeafStartsWith( String text );

  /**
   * Finds the definition that contains the given text, matching case.
   *
   * @param text The value to find, never {@code null}.
   * @return The leaf that contains the exact given value.
   */
  DefinitionTreeItem<String> findLeafContains( String text );

  /**
   * Finds the definition that contains the given text, ignoring case.
   *
   * @param text The value to find, never {@code null}.
   * @return The leaf that contains the given value, regardless of case.
   */
  DefinitionTreeItem<String> findLeafContainsNoCase( String text );

  /**
   * Answers whether there are any definitions written.
   *
   * @return {@code true} when there are no definitions.
   */
  boolean isEmpty();
}
