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
package io.gravitee.apim.core.log.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeConnectionStatusTest {

    @Test
    void fromString_returns_value_for_known_status() {
        assertThat(NativeConnectionStatus.fromString("CONNECTED")).contains(NativeConnectionStatus.CONNECTED);
        assertThat(NativeConnectionStatus.fromString("CONNECTION_ERROR")).contains(NativeConnectionStatus.CONNECTION_ERROR);
        assertThat(NativeConnectionStatus.fromString("SESSION_ERROR")).contains(NativeConnectionStatus.SESSION_ERROR);
        assertThat(NativeConnectionStatus.fromString("INTERNAL_ERROR")).contains(NativeConnectionStatus.INTERNAL_ERROR);
    }

    @Test
    void fromString_returns_empty_when_value_unknown() {
        assertThat(NativeConnectionStatus.fromString("UNKNOWN_FUTURE_STATUS")).isEmpty();
    }

    @Test
    void fromString_returns_empty_when_value_null() {
        assertThat(NativeConnectionStatus.fromString(null)).isEmpty();
    }
}
