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
package io.gravitee.apim.reporter.common.formatter.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.reporter.api.v4.common.Message;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReportableSanitizationUtilTest {

    @Test
    void should_convert_byte_array_metadata_to_utf8_string() {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("key", "message".getBytes(StandardCharsets.UTF_8)); // raw Kafka record key
        metadata.put("topic", "demo");
        metadata.put("partition", 0);

        var message = new Message();
        message.setMetadata(metadata);

        ReportableSanitizationUtil.sanitizeMessageMetadata(message);

        assertThat(message.getMetadata()).containsEntry("key", "message").containsEntry("topic", "demo").containsEntry("partition", 0);
    }

    @Test
    void should_drop_null_metadata_values() {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("key", "message".getBytes(StandardCharsets.UTF_8));
        metadata.put("nullMetadata", null);

        var message = new Message();
        message.setMetadata(metadata);

        ReportableSanitizationUtil.sanitizeMessageMetadata(message);

        assertThat(message.getMetadata()).containsOnlyKeys("key");
    }

    @Test
    void should_be_noop_when_metadata_is_null_or_empty() {
        var message = new Message();
        message.setMetadata(null);
        ReportableSanitizationUtil.sanitizeMessageMetadata(message);
        assertThat(message.getMetadata()).isNull();

        var empty = new Message();
        empty.setMetadata(new LinkedHashMap<>());
        ReportableSanitizationUtil.sanitizeMessageMetadata(empty);
        assertThat(empty.getMetadata()).isEmpty();
    }

    @Test
    void should_not_reallocate_when_nothing_to_sanitize() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("topic", "demo");

        var message = new Message();
        message.setMetadata(metadata);

        ReportableSanitizationUtil.sanitizeMessageMetadata(message);

        assertThat(message.getMetadata()).isSameAs(metadata);
    }
}
