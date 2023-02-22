/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.response.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.response.message.MessageLogEndpointResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MessageLogEndpointResponseTest {

    @Mock
    LoggingContext loggingContext;

    @BeforeEach
    public void beforeEach() {}

    @Nested
    class LogPayload {

        @Test
        void should_log_payload_when_payload_logging_is_enabled() {
            when(loggingContext.endpointResponseMessagePayload()).thenReturn(true);
            DefaultMessage message = new DefaultMessage("1");
            MessageLogEndpointResponse cut = new MessageLogEndpointResponse(loggingContext, message);
            assertThat(cut.getPayload()).isEqualTo("1");
        }

        @Test
        void should_not_log_payload_when_payload_logging_is_disabled() {
            when(loggingContext.endpointResponseMessagePayload()).thenReturn(false);
            DefaultMessage message = new DefaultMessage("1");
            MessageLogEndpointResponse cut = new MessageLogEndpointResponse(loggingContext, message);
            assertThat(cut.getPayload()).isNull();
        }
    }

    @Nested
    class LogHeader {

        @Test
        void should_log_headers_when_headers_logging_is_enabled() {
            when(loggingContext.endpointResponseMessageHeaders()).thenReturn(true);
            HttpHeaders headers = HttpHeaders.create();
            DefaultMessage message = DefaultMessage.builder().headers(headers).build();
            MessageLogEndpointResponse cut = new MessageLogEndpointResponse(loggingContext, message);
            assertThat(cut.getHeaders()).isEqualTo(headers);
        }

        @Test
        void should_not_log_headers_when_headers_logging_is_disabled() {
            when(loggingContext.endpointResponseMessageHeaders()).thenReturn(false);
            HttpHeaders headers = HttpHeaders.create();
            DefaultMessage message = DefaultMessage.builder().headers(headers).build();
            MessageLogEndpointResponse cut = new MessageLogEndpointResponse(loggingContext, message);
            assertThat(cut.getHeaders()).isNull();
        }
    }

    @Nested
    class LogMetadata {

        @Test
        void should_log_metadata_when_metadata_logging_is_enabled() {
            when(loggingContext.endpointResponseMessageMetadata()).thenReturn(true);
            Map<String, Object> metadata = new HashMap<>();
            DefaultMessage message = DefaultMessage.builder().metadata(metadata).build();
            MessageLogEndpointResponse cut = new MessageLogEndpointResponse(loggingContext, message);
            assertThat(cut.getMetadata()).isEqualTo(metadata);
        }

        @Test
        void should_not_log_metadata_when_metadata_logging_is_disabled() {
            when(loggingContext.endpointResponseMessageMetadata()).thenReturn(false);
            Map<String, Object> metadata = new HashMap<>();
            DefaultMessage message = DefaultMessage.builder().metadata(metadata).build();
            MessageLogEndpointResponse cut = new MessageLogEndpointResponse(loggingContext, message);
            assertThat(cut.getMetadata()).isNull();
        }
    }
}
