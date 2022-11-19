/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.editors.definition.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.keenwrite.editors.definition.DefinitionTreeItem;
import com.keenwrite.editors.definition.TreeTransformer;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.Map.Entry;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static com.keenwrite.events.StatusEvent.clue;

/**
 * Transforms a JsonNode hierarchy into a tree that can be displayed in a user
 * interface and vice-versa.
 */
public final class YamlTreeTransformer implements TreeTransformer {
  private static final YAMLFactory sFactory;
  private static final YAMLMapper sMapper;

  static {
    sFactory = new YAMLFactory();
    sFactory.configure( MINIMIZE_QUOTES, true );
    sFactory.configure( SPLIT_LINES, false );
    sMapper = new YAMLMapper( sFactory );
  }

  /**
   * Constructs a new instance that will use the given path to read the object
   * hierarchy from a data source.
   */
  public YamlTreeTransformer() {
  }

  @Override
  public String transform( final TreeItem<String> treeItem ) {
    try {
      final var root = sMapper.createObjectNode();

      // Iterate over the root item's children. The root item is used by the
      // application to ensure definitions can always be added to a tree, as
      // such it is not meant to be exported, only its children.
      for( final var child : treeItem.getChildren() ) {
        transform( child, root );
      }

      return sMapper.writeValueAsString( root );
    } catch( final Exception ex ) {
      clue( ex );
      throw new RuntimeException( ex );
    }
  }

  /**
   * Converts a YAML document to a {@link TreeItem} based on the document
   * keys.
   *
   * @param document The YAML document to convert to a hierarchy of
   *                 {@link TreeItem} instances.
   * @throws StackOverflowError If infinite recursion is encountered.
   */
  @Override
  public TreeItem<String> transform( final String document ) {
    final var jsonNode = toJson( document );
    final var rootItem = createTreeItem( "root" );

    transform( jsonNode, rootItem );

    return rootItem;
  }

  private JsonNode toJson( final String yaml ) {
    try {
      return new ObjectMapper( sFactory ).readTree( yaml );
    } catch( final Exception ex ) {
      // Ensure that a document root node exists.
      return new ObjectMapper().createObjectNode();
    }
  }

  /**
   * Recursive method to generate an object hierarchy that represents the
   * given {@link TreeItem} hierarchy.
   *
   * @param item The {@link TreeItem} to reproduce as an object hierarchy.
   * @param node The {@link ObjectNode} to update to reflect the
   *             {@link TreeItem} hierarchy.
   */
  private void transform( final TreeItem<String> item, ObjectNode node ) {
    final var children = item.getChildren();

    // If the current item has more than one non-leaf child, it's an
    // object node and must become a new nested object.
    if( !(children.size() == 1 && children.get( 0 ).isLeaf()) ) {
      node = node.putObject( item.getValue() );
    }

    for( final var child : children ) {
      if( child.isLeaf() ) {
        node.put( item.getValue(), child.getValue() );
      }
      else {
        transform( child, node );
      }
    }
  }

  /**
   * Iterate over a given root node (at any level of the tree) and adapt each
   * leaf node.
   *
   * @param node A JSON node (YAML node) to adapt.
   * @param item The tree item to use as the root when processing the node.
   * @throws StackOverflowError If infinite recursion is encountered.
   */
  private void transform( final JsonNode node, final TreeItem<String> item ) {
    node.fields().forEachRemaining( leaf -> transform( leaf, item ) );
  }

  /**
   * Recursively adapt each rootNode to a corresponding rootItem.
   *
   * @param node The node to adapt.
   * @param item The item to adapt using the node's key.
   * @throws StackOverflowError If infinite recursion is encountered.
   */
  private void transform(
    final Entry<String, JsonNode> node, final TreeItem<String> item ) {
    final var leafNode = node.getValue();
    final var key = node.getKey();
    final var leaf = createTreeItem( key );

    if( leafNode.isValueNode() ) {
      leaf.getChildren().add( createTreeItem( node.getValue().asText() ) );
    }

    item.getChildren().add( leaf );

    if( leafNode.isObject() ) {
      transform( leafNode, leaf );
    }
  }

  /**
   * Creates a new {@link TreeItem} that can be added to the {@link TreeView}.
   *
   * @param value The node's value.
   * @return A new {@link TreeItem}, never {@code null}.
   */
  private TreeItem<String> createTreeItem( final String value ) {
    return new DefinitionTreeItem<>( value );
  }
}
