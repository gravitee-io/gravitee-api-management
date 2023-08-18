/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.reporter.common.formatter.csv;

import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.common.formatter.AbstractFormatter;
import io.vertx.core.buffer.Buffer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class SingleValueFormatter<T extends Reportable>
  extends AbstractFormatter<T> {

  private static final String EMPTY_VALUE = "";

  private static final char CSV_DELIMITER = ';';

  private static final char CSV_QUOTE = '"';

  /**
   * {@code \u000a} linefeed LF ('\n').
   *
   * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
   *      for Character and String Literals</a>
   * @since 2.2
   */
  private static final char LF = '\n';

  /**
   * {@code \u000d} carriage return CR ('\r').
   *
   * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
   *      for Character and String Literals</a>
   * @since 2.2
   */
  private static final char CR = '\r';

  static final byte[] END_OF_LINE = new byte[] { CR, LF };

  private static final byte FIELD_SEPARATOR = (byte) CSV_DELIMITER;

  private static final byte FIELD_QUOTE = (byte) CSV_QUOTE;

  private static final String CSV_QUOTE_STR = String.valueOf(CSV_QUOTE);

  private static final char[] CSV_SEARCH_CHARS = {
    CSV_DELIMITER,
    CSV_QUOTE,
    CR,
    LF,
  };

  protected void appendEmpty(Buffer buffer) {
    appendString(buffer, null, false);
  }

  protected void appendString(Buffer buffer, String value) {
    appendString(buffer, value, false);
  }

  protected void appendString(Buffer buffer, String value, boolean last) {
    appendString(buffer, value, false, last);
  }

  protected void appendString(
    Buffer buffer,
    String value,
    boolean escape,
    boolean last
  ) {
    buffer.appendByte(FIELD_QUOTE);

    if (!escape || value == null || containsNone(value, CSV_SEARCH_CHARS)) {
      buffer.appendString(value != null ? value : EMPTY_VALUE);
    } else {
      buffer.appendString(
        value.replace(CSV_QUOTE_STR, CSV_QUOTE_STR + CSV_QUOTE_STR)
      );
    }

    buffer.appendByte(FIELD_QUOTE);

    if (!last) {
      buffer.appendByte(FIELD_SEPARATOR);
    }
  }

  void appendShort(Buffer buffer, short value) {
    appendShort(buffer, value, false);
  }

  void appendShort(Buffer buffer, short value, boolean last) {
    buffer.appendString(Short.toString(value));

    if (!last) {
      buffer.appendByte(FIELD_SEPARATOR);
    }
  }

  protected void appendInt(Buffer buffer, int value) {
    appendInt(buffer, value, false);
  }

  void appendInt(Buffer buffer, int value, boolean last) {
    buffer.appendString(Integer.toString(value));

    if (!last) {
      buffer.appendByte(FIELD_SEPARATOR);
    }
  }

  protected void appendLong(Buffer buffer, long value) {
    appendLong(buffer, value, false);
  }

  void appendLong(Buffer buffer, long value, boolean last) {
    buffer.appendString(Long.toString(value));

    if (!last) {
      buffer.appendByte(FIELD_SEPARATOR);
    }
  }

  protected void appendBoolean(Buffer buffer, boolean value) {
    appendBoolean(buffer, value, false);
  }

  void appendBoolean(Buffer buffer, boolean value, boolean last) {
    buffer.appendString(value ? "true" : "false");

    if (!last) {
      buffer.appendByte(FIELD_SEPARATOR);
    }
  }

  void appendEndOfLine(Buffer buffer) {
    buffer.appendBytes(END_OF_LINE);
  }

  private static boolean containsNone(
    final CharSequence cs,
    final char... searchChars
  ) {
    if (cs == null || searchChars == null) {
      return true;
    }

    final int csLen = cs.length();
    final int csLast = csLen - 1;
    final int searchLen = searchChars.length;
    final int searchLast = searchLen - 1;
    for (int i = 0; i < csLen; i++) {
      final char ch = cs.charAt(i);
      for (int j = 0; j < searchLen; j++) {
        if (searchChars[j] == ch) {
          if (Character.isHighSurrogate(ch)) {
            if (j == searchLast) {
              return false;
            }
            if (i < csLast && searchChars[j + 1] == cs.charAt(i + 1)) {
              return false;
            }
          } else {
            return false;
          }
        }
      }
    }
    return true;
  }
}
