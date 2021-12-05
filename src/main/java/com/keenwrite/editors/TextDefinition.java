/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.editors;

import com.keenwrite.editors.definition.DefinitionEditor;
import com.keenwrite.editors.definition.DefinitionTreeItem;
import com.keenwrite.editors.markdown.MarkdownEditor;
import com.keenwrite.sigils.SigilOperator;
import javafx.scene.control.TreeItem;

import java.util.Map;

/**
 * Differentiates an instance of {@link TextResource} from an instance of
 * {@link DefinitionEditor} or {@link MarkdownEditor}.
 */
public interface TextDefinition extends TextResource {

  /**
   * Requests that the definitions be interpolated.
   *
   * @param sigilOperator Encapsulates how to encode variable names with
   *                      sigils.
   * @return The definition map with all variables interpolated.
   */
  Map<String, String> interpolate( SigilOperator sigilOperator );

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
