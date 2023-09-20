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
package io.gravitee.apim.core.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PathTest {

    public static Stream<Arguments> sanitizeParams() {
        return Stream.of(
            Arguments.of(null, "/"),
            Arguments.of("", "/"),
            Arguments.of("path", "/path/"),
            Arguments.of("/path", "/path/"),
            Arguments.of("path/", "/path/"),
            Arguments.of("//path/", "/path/"),
            Arguments.of("path//subpath", "/path/subpath/")
        );
    }

    @ParameterizedTest
    @MethodSource("sanitizeParams")
    void should_add_initial_slash(String input, String expected) {
        assertThat(Path.sanitizePath(input)).isEqualTo(expected);
    }
}
