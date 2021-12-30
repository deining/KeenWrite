/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.sigils;

/**
 * Brackets variable names between {@link #PREFIX} and {@link #SUFFIX} sigils.
 */
public final class RSigilOperator extends SigilOperator {
  public static final String PREFIX = "`r#";
  public static final char SUFFIX = '`';

  private static final char KEY_SEPARATOR_DEF = '.';
  private static final char KEY_SEPARATOR_R = '$';

  /**
   * Definition variables are inserted into the document before R variables,
   * so this is required to reformat the definition variable suitable for R.
   */
  private final SigilOperator mAntecedent;

  /**
   * Constructs a new {@link RSigilOperator} capable of wrapping tokens around
   * variable names (keys).
   *
   * @param sigils     The starting and ending tokens.
   * @param antecedent The operator to use to undo any previous entokenizing.
   */
  public RSigilOperator( final Sigils sigils, final SigilOperator antecedent ) {
    super( sigils );

    mAntecedent = antecedent;
  }

  /**
   * Returns the given string with backticks prepended and appended. The
   *
   * @param key The string to adorn with R token delimiters.
   * @return PREFIX + delimiterBegan + variableName + delimiterEnded + SUFFIX.
   */
  @Override
  public String apply( final String key ) {
    assert key != null;

    return PREFIX + getBegan() + key + getEnded() + SUFFIX;
  }

  /**
   * Transforms a definition key (bracketed by token delimiters) into the
   * expected format for an R variable key name.
   * <p>
   * The algorithm to entoken a definition name is faster than
   * {@link String#replace(char, char)}. Faster still would be to cache the
   * values, but that would mean managing the cache when the user changes
   * the beginning and ending of the R delimiters. This code gives about a
   * 2% performance boost when scrolling using cursor keys. After the JIT
   * warms up, this super-minor bottleneck vanishes.
   * </p>
   *
   * @param key The variable name to transform, can be empty but not null.
   * @return The transformed variable name.
   */
  public String entoken( final String key ) {
    final var detokened = new StringBuilder( key.length() );

    detokened.append( "v$" );
    detokened.append( mAntecedent.detoken( key ) );

    // The 3 is for "v$X" where X cannot be a period.
    for( int i = detokened.length() - 1; i >= 3; i-- ) {
      if( detokened.charAt( i ) == KEY_SEPARATOR_DEF ) {
        detokened.setCharAt( i, KEY_SEPARATOR_R );
      }
    }

    return detokened.toString();
  }
}
