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
package io.gravitee.gamma.rest.core.observability.catalog.model;

/**
 * How a metric definition treats the characters of a key name that the custom grammar accepts but a record refuses:
 * ISO control characters (CR, LF, NUL…) and UTF-16 surrogates without their pair.
 *
 * @author GraviteeSource Team
 */
public final class MetricDefinitionNames {

    /** The rule a record's name breaks when it holds such a character, as refusals word it. */
    public static final String UNPRINTABLE_RULE = "a metric definition name holds no control character and no unpaired UTF-16 surrogate";

    private MetricDefinitionNames() {}

    /** Whether {@code name} holds a control character or an unpaired surrogate. */
    public static boolean holdsUnprintable(String name) {
        return name != null && name.codePoints().anyMatch(MetricDefinitionNames::unprintable);
    }

    /**
     * {@code text} with each control character and unpaired surrogate written as a {@code \}{@code uXXXX} escape, so a
     * name read from a request or a store never puts a raw line break or NUL into a message or a log line.
     */
    public static String printable(String text) {
        if (text == null || !holdsUnprintable(text)) {
            return text;
        }
        StringBuilder printable = new StringBuilder(text.length() + 8);
        text
            .codePoints()
            .forEach(codePoint -> {
                if (unprintable(codePoint)) {
                    printable.append("\\u%04X".formatted(codePoint));
                } else {
                    printable.appendCodePoint(codePoint);
                }
            });
        return printable.toString();
    }

    // codePoints() yields an unpaired surrogate as its own code point, in the surrogate range.
    private static boolean unprintable(int codePoint) {
        return Character.isISOControl(codePoint) || (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE);
    }
}
