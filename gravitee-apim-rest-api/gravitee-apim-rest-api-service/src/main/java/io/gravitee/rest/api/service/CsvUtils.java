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
package io.gravitee.rest.api.service;

public class CsvUtils {

    /**
     * The delimiter used in the CSV file: comma, semicolon, etc.
     */
    private final char delimiter;

    public CsvUtils(char delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Sanitize a value to be used in a CSV file. It follows the OWASP best practices described in: https://owasp.org/www-community/attacks/CSV_Injection
     * @param value the value to sanitize
     * @return the sanitized value, an empty string if the value is null
     */
    public String sanitize(String value) {
        if (value == null) {
            return "";
        }

        // If value starts with equal, plus or minus, the equal sign character is prepended.
        if (value.startsWith("=") || value.startsWith("+") || value.startsWith("-")) {
            value = "'" + value;
        }

        // If the value contains double quotation marks, the quotation mark character is repeated twice.
        value = value.replace("\"", "\"\"");

        // If the value contains the delimiter, the single-quote character, the newline character, or the carriage-return character, then the value is enclosed in double quotation marks.
        if (value.contains(String.valueOf(delimiter)) || value.contains("'") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value + "\"";
        }

        return value;
    }
}
