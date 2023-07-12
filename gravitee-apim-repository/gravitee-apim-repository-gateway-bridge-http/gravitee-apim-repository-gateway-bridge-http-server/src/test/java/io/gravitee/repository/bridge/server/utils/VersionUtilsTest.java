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
package io.gravitee.repository.bridge.server.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class VersionUtilsTest {

    private static Stream<String> validVersions() {
        return Stream.of(
            "1.2.3",
            "1.2.3-SNAPSHOT",
            "1.2.3-alpha.1",
            "1.2.3-alpha.1-SNAPSHOT",
            "1.2.3-beta.1",
            "1.2.3-beta.1-SNAPSHOT",
            "1.2.3-rc.1",
            "1.2.3-rc.1-SNAPSHOT"
        );
    }

    @ParameterizedTest
    @MethodSource("validVersions")
    void parseValidVersion(String expression) {
        VersionUtils.Version version = VersionUtils.parse(expression);
        assertNotNull(version);
        assertEquals(1, version.major());
        assertEquals(2, version.minor());
        assertEquals(3, version.patch());
    }

    private static Stream<String> invalidVersions() {
        return Stream.of("1.2", "1.2.SNAPSHOT", "1.2.3test");
    }

    @ParameterizedTest
    @MethodSource("invalidVersions")
    void parseInvalidVersion(String expression) {
        VersionUtils.Version version = VersionUtils.parse(expression);
        assertNull(version);
    }
}
