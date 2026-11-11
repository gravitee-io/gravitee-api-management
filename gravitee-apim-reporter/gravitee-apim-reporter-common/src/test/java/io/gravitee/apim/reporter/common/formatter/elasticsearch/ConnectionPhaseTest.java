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
package io.gravitee.apim.reporter.common.formatter.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.reporter.api.v4.log.Log;
import io.gravitee.reporter.api.v4.metric.Metrics;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConnectionPhaseTest {

    @Nested
    class Metrics_ {

        @Test
        void should_tell_the_opening_of_a_message_connection() {
            Metrics metrics = Metrics.builder().requestId("req").apiType("message").build();

            assertThat(ConnectionPhase.isOpening(metrics)).isTrue();
            assertThat(ConnectionPhase.isEnd(metrics)).isFalse();
        }

        @Test
        void should_tell_the_end_of_a_message_connection() {
            Metrics metrics = Metrics.builder().requestId("req").apiType("message").requestEnded(true).build();

            assertThat(ConnectionPhase.isOpening(metrics)).isFalse();
            assertThat(ConnectionPhase.isEnd(metrics)).isTrue();
        }

        @Test
        void should_tell_no_phase_for_a_proxy_request() {
            Metrics proxy = Metrics.builder().requestId("req").apiType("proxy").requestEnded(true).build();

            assertThat(ConnectionPhase.isOpening(proxy)).isFalse();
            assertThat(ConnectionPhase.isEnd(proxy)).isFalse();
        }

        @Test
        void should_tell_no_phase_for_other_apis_even_when_not_ended() {
            Metrics nativeKafka = Metrics.builder().requestId("conn:1:connected").apiType("native").build();

            assertThat(ConnectionPhase.isOpening(nativeKafka)).isFalse();
            assertThat(ConnectionPhase.isEnd(nativeKafka)).isFalse();
        }
    }

    @Nested
    class Log_ {

        @Test
        void should_tell_the_opening_of_a_connection() {
            Log log = Log.builder().requestId("req").build();

            assertThat(ConnectionPhase.isOpening(log)).isTrue();
        }

        @Test
        void should_not_tell_an_ended_connection_as_an_opening() {
            Log log = Log.builder().requestId("req").requestEnded(true).build();

            assertThat(ConnectionPhase.isOpening(log)).isFalse();
        }
    }

    @Test
    void should_suffix_the_document_id_of_an_opening() {
        assertThat(ConnectionPhase.openingDocumentId("req")).isEqualTo("req:open");
    }
}
