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
package io.gravitee.repository.redis.distributedsync;

/**
 * RediSearch TAG field value escaping for query strings.
 */
final class RediSearchTagValues {

    private static final String TAG_QUERY_SPECIAL_CHARACTERS = ",.<>{}[]\"':;!@#$%^&*()-+=~\\/?| ";

    private RediSearchTagValues() {}

    static String escapeTagValue(final String value) {
        if (value == null) {
            return null;
        }
        var escaped = new StringBuilder(value.length() * 2);
        for (char character : value.toCharArray()) {
            if (TAG_QUERY_SPECIAL_CHARACTERS.indexOf(character) >= 0) {
                escaped.append('\\');
            }
            escaped.append(character);
        }
        return escaped.toString();
    }
}
