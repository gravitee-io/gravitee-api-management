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
package io.gravitee.repository.elasticsearch.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Text typed by a user, made safe for the value of a {@code wildcard} query.
 *
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WildcardValues {

    /**
     * {@code *} and {@code ?} are wildcard syntax: unescaped, one typed in a filter turns it into no filter at all
     * while the caller still sees it active. Backslash first, or the escapes inserted after it would be escaped in turn.
     */
    public static String escape(String text) {
        return text.replace("\\", "\\\\").replace("*", "\\*").replace("?", "\\?");
    }

    /** Matches the text anywhere in the value. */
    public static String containing(String text) {
        return "*" + escape(text) + "*";
    }
}
