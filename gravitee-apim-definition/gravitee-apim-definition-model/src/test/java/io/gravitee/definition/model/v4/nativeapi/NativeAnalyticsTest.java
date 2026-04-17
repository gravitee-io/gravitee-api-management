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
package io.gravitee.definition.model.v4.nativeapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeAnalyticsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_default_connection_log_when_absent_in_json() throws Exception {
        var analytics = objectMapper.readValue("{\"enabled\":true}", NativeAnalytics.class);

        assertThat(analytics.getConnectionLog())
            .isNotNull()
            .extracting(ConnectionLog::isEnabled, ConnectionLog::isDebugEnabled)
            .containsExactly(true, false);
    }

    @Test
    void should_round_trip_connection_log() throws Exception {
        var original = NativeAnalytics.builder().connectionLog(ConnectionLog.builder().enabled(true).debugEnabled(true).build()).build();

        var deserialized = objectMapper.readValue(objectMapper.writeValueAsString(original), NativeAnalytics.class);

        assertThat(deserialized).isEqualTo(original);
    }
}
