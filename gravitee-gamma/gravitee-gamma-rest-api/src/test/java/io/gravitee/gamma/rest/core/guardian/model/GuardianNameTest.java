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
package io.gravitee.gamma.rest.core.guardian.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * What {@link Guardian#withoutOuterWhitespace} claims, pinned. The rule is not {@code trim} and not
 * {@code strip}: the first stops at {@code U+0020}, the second follows {@link Character#isWhitespace},
 * which excludes the non-breaking spaces a pasted name carries.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianNameTest {

    // Built rather than written as \\uXXXX escapes: Java expands those before lexing, so a line separator
    // would end the string literal itself, and the formatter rewrites the others as raw characters.
    private static final String NBSP = Character.toString(0x00A0);
    private static final String NARROW_NBSP = Character.toString(0x202F);
    private static final String FIGURE_SPACE = Character.toString(0x2007);
    private static final String LINE_SEPARATOR = Character.toString(0x2028);
    private static final String PARAGRAPH_SEPARATOR = Character.toString(0x2029);
    private static final String IDEOGRAPHIC_SPACE = Character.toString(0x3000);
    private static final String ZERO_WIDTH_SPACE = Character.toString(0x200B);
    private static final String GRINNING_FACE = Character.toString(0x1F600);
    private static final String LONE_SURROGATE = Character.toString(0xD83D);

    private static final String NAME = "Credential leakage";

    @ParameterizedTest(name = "case {index}")
    @MethodSource("names")
    void should_keep_a_name_without_the_whitespace_around_it(String sent, String kept) {
        assertThat(Guardian.withoutOuterWhitespace(sent)).isEqualTo(kept);
    }

    private static Stream<Arguments> names() {
        return Stream.of(
            Arguments.of(NAME, NAME),
            Arguments.of("  " + NAME + "  ", NAME),
            Arguments.of("\t" + NAME + "\n", NAME),
            // the non-breaking spaces String.strip leaves in place
            Arguments.of(NBSP + NAME + NBSP, NAME),
            Arguments.of(NARROW_NBSP + NAME + FIGURE_SPACE, NAME),
            // the separators isWhitespace does accept
            Arguments.of(LINE_SEPARATOR + NAME + PARAGRAPH_SEPARATOR, NAME),
            Arguments.of(IDEOGRAPHIC_SPACE + NAME + IDEOGRAPHIC_SPACE, NAME),
            Arguments.of("", ""),
            Arguments.of("   ", ""),
            Arguments.of(NBSP + IDEOGRAPHIC_SPACE, "")
        );
    }

    /** Only the outside: a name is allowed to breathe in the middle. */
    @Test
    void should_keep_the_whitespace_inside_a_name() {
        assertThat(Guardian.withoutOuterWhitespace("  Credential   leakage  ")).isEqualTo("Credential   leakage");
    }

    /**
     * Zero-width and other format characters are not spaces. A name carrying one is a different name, not a
     * padded one, so it is left alone rather than silently merged with its neighbour.
     */
    @Test
    void should_keep_a_zero_width_character() {
        var padded = ZERO_WIDTH_SPACE + NAME + ZERO_WIDTH_SPACE;

        assertThat(Guardian.withoutOuterWhitespace(padded)).isEqualTo(padded);
    }

    /** The loop walks code points, so a surrogate pair at either end survives whole. */
    @Test
    void should_not_split_a_surrogate_pair() {
        assertThat(Guardian.withoutOuterWhitespace("  " + GRINNING_FACE + " guard " + GRINNING_FACE + "  ")).isEqualTo(
            GRINNING_FACE + " guard " + GRINNING_FACE
        );
    }

    /** A lone surrogate is not whitespace either, and must not throw on the way past. */
    @Test
    void should_leave_a_lone_surrogate_alone() {
        assertThat(Guardian.withoutOuterWhitespace(" " + LONE_SURROGATE + " ")).isEqualTo(LONE_SURROGATE);
    }

    @Test
    void should_answer_null_for_a_name_there_is_none_of() {
        assertThat(Guardian.withoutOuterWhitespace(null)).isNull();
    }
}
