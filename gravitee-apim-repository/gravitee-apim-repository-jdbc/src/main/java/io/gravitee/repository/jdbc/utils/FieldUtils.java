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
package io.gravitee.repository.jdbc.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FieldUtils {

    public static String toSnakeCase(final String string) {
        // null or empty String
        if (string == null || string.isEmpty()) {
            return string;
        }
        StringBuilder result = new StringBuilder();

        // Append first character(in lower case)
        char c = string.charAt(0);
        result.append(Character.toLowerCase(c));

        // Traverse the string
        for (int i = 1; i < string.length(); i++) {
            char ch = string.charAt(i);

            // Check if the character is upper case
            // then append '_' and such character (in lower case)
            if (Character.isUpperCase(ch)) {
                result.append('_').append(Character.toLowerCase(ch));
            }
            // otherwise append already lower case character
            else {
                result.append(ch);
            }
        }

        // return the result
        return result.toString();
    }
}
