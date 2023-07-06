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
package io.gravitee.apim.gateway.tests.sdk.assertions;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.message.DefaultMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageAssertTest {

    @Test
    void testHasContent() {
        var message = DefaultMessage.builder().content(Buffer.buffer("content")).build();
        MessageAssert.assertThat(message).hasContent("content");
    }

    @Test
    void testHasCorrelationId() {
        var message = DefaultMessage.builder().correlationId("correlationId").build();
        MessageAssert.assertThat(message).hasCorrelationId("correlationId");
    }

    @Test
    void testHasId() {
        var message = DefaultMessage.builder().id("id").build();
        MessageAssert.assertThat(message).hasId("id");
    }

    @Test
    void testHasHeaders() {
        var headers = HttpHeaders.create();
        headers.add("header1", "value1");

        var message = DefaultMessage.builder().headers(headers).build();
        MessageAssert.assertThat(message).hasHeaders(Map.of("header1", "value1"));
    }

    @Test
    void testHasMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("metadata1", "value1");
        var message = DefaultMessage.builder().metadata(metadata).build();

        MessageAssert.assertThat(message).hasMetadata(metadata);
    }

    @Test
    void testHasOnlyMetadataKey() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("metadata1", "value1");
        var message = DefaultMessage.builder().metadata(metadata).build();

        MessageAssert.assertThat(message).hasOnlyMetadataKey(List.of("metadata1", "sourceTimestamp"));
    }
}
