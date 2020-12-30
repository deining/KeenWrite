/* Copyright 2020 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.io;

import java.io.File;
import java.nio.file.Path;

import static com.keenwrite.io.MediaType.TypeName.*;
import static com.keenwrite.io.MediaTypeExtensions.getMediaType;
import static org.apache.commons.io.FilenameUtils.getExtension;

/**
 * Defines various file formats and format contents.
 *
 * @see
 * <a href="https://www.iana.org/assignments/media-types/media-types.xhtml">IANA
 * Media Types</a>
 */
public enum MediaType {
  APP_JAVA_OBJECT(
    APPLICATION, "x-java-serialized-object"
  ),

  FONT_OTF( "otf" ),
  FONT_TTF( "ttf" ),

  IMAGE_APNG( "apng" ),
  IMAGE_ACES( "aces" ),
  IMAGE_AVCI( "avci" ),
  IMAGE_AVCS( "avcs" ),
  IMAGE_BMP( "bmp" ),
  IMAGE_CGM( "cgm" ),
  IMAGE_DICOM_RLE( "dicom_rle" ),
  IMAGE_EMF( "emf" ),
  IMAGE_EXAMPLE( "example" ),
  IMAGE_FITS( "fits" ),
  IMAGE_G3FAX( "g3fax" ),
  IMAGE_GIF( "gif" ),
  IMAGE_HEIC( "heic" ),
  IMAGE_HEIF( "heif" ),
  IMAGE_HEJ2K( "hej2k" ),
  IMAGE_HSJ2( "hsj2" ),
  IMAGE_X_ICON( "x-icon" ),
  IMAGE_JLS( "jls" ),
  IMAGE_JP2( "jp2" ),
  IMAGE_JPEG( "jpeg" ),
  IMAGE_JPH( "jph" ),
  IMAGE_JPHC( "jphc" ),
  IMAGE_JPM( "jpm" ),
  IMAGE_JPX( "jpx" ),
  IMAGE_JXR( "jxr" ),
  IMAGE_JXRA( "jxrA" ),
  IMAGE_JXRS( "jxrS" ),
  IMAGE_JXS( "jxs" ),
  IMAGE_JXSC( "jxsc" ),
  IMAGE_JXSI( "jxsi" ),
  IMAGE_JXSS( "jxss" ),
  IMAGE_KTX( "ktx" ),
  IMAGE_KTX2( "ktx2" ),
  IMAGE_NAPLPS( "naplps" ),
  IMAGE_PNG( "png" ),
  IMAGE_SVG_XML( "svg+xml" ),
  IMAGE_T38( "t38" ),
  IMAGE_TIFF( "tiff" ),
  IMAGE_WEBP( "webp" ),
  IMAGE_WMF( "wmf" ),

  TEXT_HTML( TEXT, "html" ),
  TEXT_MARKDOWN( TEXT, "markdown" ),
  TEXT_PLAIN( TEXT, "plain" ),
  TEXT_R_MARKDOWN( TEXT, "R+markdown" ),
  TEXT_R_XML( TEXT, "R+xml" ),
  TEXT_YAML( TEXT, "yaml" ),

  UNDEFINED( TypeName.UNDEFINED, "undefined" );

  /**
   * The IANA-defined types.
   */
  public enum TypeName {
    APPLICATION,
    IMAGE,
    TEXT,
    UNDEFINED
  }

  /**
   * The fully qualified IANA-defined media type.
   */
  private final String mMediaType;

  /**
   * The IANA-defined type name.
   */
  private final TypeName mTypeName;

  /**
   * The IANA-defined subtype name.
   */
  private final String mSubtype;

  /**
   * Constructs an instance using the default type name of "image".
   *
   * @param subtype The image subtype name.
   */
  MediaType( final String subtype ) {
    this( IMAGE, subtype );
  }

  /**
   * Constructs an instance using an IANA-defined type and subtype pair.
   *
   * @param typeName The media type's type name.
   * @param subtype  The media type's subtype name.
   */
  MediaType( final TypeName typeName, final String subtype ) {
    mTypeName = typeName;
    mSubtype = subtype;
    mMediaType = typeName.toString().toLowerCase() + '/' + subtype;
  }

  /**
   * Returns the {@link MediaType} associated with the given file.
   *
   * @param file Has a file name that may contain an extension associated with
   *             a known {@link MediaType}.
   * @return {@link MediaType#UNDEFINED} if the extension has not been
   * assigned, otherwise the {@link MediaType} associated with this
   * {@link File}'s file name extension.
   */
  public static MediaType valueFrom( final File file ) {
    return valueFrom( file.getName() );
  }

  /**
   * Returns the {@link MediaType} associated with the path to a file.
   *
   * @param path Has a file name that may contain an extension associated with
   *             a known {@link MediaType}.
   * @return {@link MediaType#UNDEFINED} if the extension has not been
   * assigned, otherwise the {@link MediaType} associated with this
   * {@link File}'s file name extension.
   */
  public static MediaType valueFrom( final Path path ) {
    return valueFrom( path.toFile() );
  }

  /**
   * Returns the {@link MediaType} associated with the given file name.
   *
   * @param filename The file name that may contain an extension associated
   *                 with a known {@link MediaType}.
   * @return {@link MediaType#UNDEFINED} if the extension has not been
   * assigned, otherwise the {@link MediaType} associated with this
   * URL's file name extension.
   */
  public static MediaType valueFrom( final String filename ) {
    return getMediaType( getExtension( filename ) );
  }

  /**
   * Returns the {@link MediaType} for the given type and subtype names.
   *
   * @param type    The IANA-defined type name.
   * @param subtype The IANA-defined subtype name.
   * @return {@link MediaType#UNDEFINED} if there is no {@link MediaType} that
   * matches the given type and subtype names.
   */
  public static MediaType valueFrom(
    final String type, final String subtype ) {
    for( final var mediaType : MediaType.values() ) {
      if( mediaType.equals( type, subtype ) ) {
        return mediaType;
      }
    }

    return UNDEFINED;
  }

  /**
   * Answers whether the given type and subtype names equal this enumerated
   * value. This performs a case-insensitive comparison.
   *
   * @param type    The type name to compare against this {@link MediaType}.
   * @param subtype The subtype name to compare against this {@link MediaType}.
   * @return {@code true} when the type and subtype name match.
   */
  public boolean equals( final String type, final String subtype ) {
    return mTypeName.name().equalsIgnoreCase( type ) &&
      mSubtype.equalsIgnoreCase( subtype );
  }

  /**
   * Answers whether the given {@link TypeName} matches this type name.
   *
   * @param typeName The {@link TypeName} to compare against the internal value.
   * @return {@code true} if the given value is the same IANA-defined type name.
   */
  public boolean isType( final TypeName typeName ) {
    return mTypeName == typeName;
  }

  /**
   * Returns the IANA-defined type and sub-type.
   *
   * @return The unique media type identifier.
   */
  public String toString() {
    return mMediaType;
  }

  /**
   * Used by {@link MediaTypeExtensions} to initialize associations where the
   * subtype name and the file name extension have a 1:1 mapping.
   *
   * @return The IANA subtype value.
   */
  String getSubtype() {
    return mSubtype;
  }
}
