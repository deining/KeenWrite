/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.io;

import com.keenwrite.typesetting.container.impl.StreamGobbler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.System.getenv;
import static java.nio.file.Files.isExecutable;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

/**
 * Responsible for file-related functionality.
 */
public class SysFile extends java.io.File {
  /**
   * For finding executable programs.
   */
  private static final String[] EXTENSIONS = new String[]
    {"", ".com", ".exe", ".bat", ".cmd"};

  //@formatter:off
  private static final String SYS_KEY =
    "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment";
  private static final String USR_KEY =
    "HKEY_CURRENT_USER\\Environment";
  //@formatter:on

  /**
   * Regular expression pattern for matching %VARIABLE% names.
   */
  private static final String VAR_REGEX = "%.*?%";
  private static final Pattern VAR_PATTERN = compile( VAR_REGEX );

  private static final String REG_REGEX = "\\s*path\\s+REG_EXPAND_SZ\\s+(.*)";
  private static final Pattern REG_PATTERN = compile( REG_REGEX );

  /**
   * Creates a new instance for a given file name.
   *
   * @param filename Filename to query existence as executable.
   */
  public SysFile( final String filename ) {
    super( filename );
  }

  /**
   * Answers whether the path returned from {@link #locate()} is an executable
   * that can be run using a {@link ProcessBuilder}.
   */
  public boolean canRun() {
    return locate().isPresent();
  }

  /**
   * For a file name that represents an executable (without an extension)
   * file, this determines the first matching executable found in the PATH
   * environment variable. This will search the PATH each time the method
   * is invoked, triggering a full directory scan for all paths listed in
   * the environment variable. The result is not cached, so avoid calling
   * this in a critical loop.
   * <p>
   * After installing software, the software might be located in the PATH,
   * but not available to run by its name alone. In such cases, we need the
   * absolute path to the executable to run it. This will always return
   * the fully qualified path, otherwise an empty result.
   *
   * @param map The mapping function of registry variable names to values.
   * @return The fully qualified {@link Path} to the executable filename
   * provided at construction time.
   */
  public Optional<Path> locate( final Function<String, String> map ) {
    final var exe = getName();
    final var paths = paths( map ).split( quote( pathSeparator ) );

    for( final var path : paths ) {
      final var p = Path.of( path ).resolve( exe );

      for( final var extension : EXTENSIONS ) {
        final var filename = Path.of( p + extension );

        if( isExecutable( filename ) ) {
          return Optional.of( filename );
        }
      }
    }

    return Optional.empty();
  }

  /**
   * Convenience method that locates a binary executable file in the path
   * by using {@link System#getenv(String)} to retrieve environment variables
   * that are expanded when parsing the PATH.
   *
   * @see #locate(Function)
   */
  public Optional<Path> locate() {
    return locate( System::getenv );
  }

  /**
   * Changes to the PATH environment variable aren't reflected for the
   * currently running task. The registry, however, contains the updated
   * value. Reading the registry is a hack.
   *
   * @param map The mapping function of registry variable names to values.
   * @return The revised PATH variables as stored in the registry.
   */
  private String paths( final Function<String, String> map ) {
    return IS_OS_WINDOWS ? pathsWindows( map ) : pathsSane();
  }

  private String pathsSane() {
    return getenv( "PATH" );
  }

  private String pathsWindows( final Function<String, String> map ) {
    try {
      final var hklm = query( SYS_KEY );
      final var hkcu = query( USR_KEY );

      return expand( hklm, map ) + pathSeparator + expand( hkcu, map );
    } catch( final IOException ex ) {
      // Return the PATH environment variable if the registry query fails.
      return pathsSane();
    }
  }

  /**
   * Queries a registry key PATH value.
   *
   * @param key The registry key name to look up.
   * @return The value for the registry key.
   */
  private String query( final String key ) throws IOException {
    final var regVarName = "path";
    final var args = new String[]{"reg", "query", key, "/v", regVarName};
    final var process = Runtime.getRuntime().exec( args );
    final var stream = process.getInputStream();
    final var regValue = new StringBuffer( 1024 );
    final var gobbler = new StreamGobbler( stream, text -> {
      if( text.contains( regVarName ) ) {
        regValue.append( parseRegEntry( text ) );
      }
    } );

    // Populate the buffer.
    gobbler.call();

    return regValue.toString();
  }

  String parseRegEntry( final String text ) {
    assert text != null;

    final var matcher = REG_PATTERN.matcher( text );
    return matcher.find() ? matcher.group( 1 ) : text.trim();
  }

  /**
   * PATH environment variables returned from the registry have unexpanded
   * variables of the form %VARIABLE%. This method will expand those values,
   * if possible, from the environment. This will only perform a single
   * expansion, which should be adequate for most needs.
   *
   * @param s The %VARIABLE%-encoded value to expand.
   * @return The given value with all encoded values expanded.
   */
  String expand( final String s, final Function<String, String> map ) {
    // Assigned to the unexpanded string, initially.
    String expanded = s;

    final var matcher = VAR_PATTERN.matcher( expanded );

    while( matcher.find() ) {
      final var match = matcher.group( 0 );
      String value = map.apply( match );

      if( value == null ) {
        value = "";
      }
      else {
        value = value.replace( "\\", "\\\\" );
      }

      final var subexpr = compile( quote( match ) );
      expanded = subexpr.matcher( expanded ).replaceAll( value );
    }

    return expanded;
  }
}
