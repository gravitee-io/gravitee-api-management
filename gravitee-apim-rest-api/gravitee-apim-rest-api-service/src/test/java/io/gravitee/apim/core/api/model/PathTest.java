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

import static org.assertj.core.api.Assertions.*;

import io.gravitee.apim.core.api.exception.InvalidPathsException;
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
            Arguments.of("path//subpath", "/path/subpath/"),
            Arguments.of("path?param=value", "/path?param=value/"),
            Arguments.of("path?param=value%20s", "/path?param=value%20s/")
        );
    }

    public static Stream<Arguments> invalidPaths() {
        return Stream.of(Arguments.of("invalid%path"), Arguments.of("invalid path"), Arguments.of("invalid>path"));
    }

    @ParameterizedTest
    @MethodSource("sanitizeParams")
    void should_sanitize_path(String input, String expected) {
        assertThat(Path.sanitizePath(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("invalidPaths")
    void should_throw_exception_if_path_is_invalid(String invalidPath) {
        var throwable = catchThrowable(() -> Path.sanitizePath(invalidPath));
        assertThat(throwable).isInstanceOf(InvalidPathsException.class);
    }
}
