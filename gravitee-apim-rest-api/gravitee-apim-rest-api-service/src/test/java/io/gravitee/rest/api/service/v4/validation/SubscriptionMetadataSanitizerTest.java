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
package io.gravitee.rest.api.service.v4.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.gravitee.rest.api.service.v4.exception.SubscriptionMetadataInvalidException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionMetadataSanitizerTest {

    private SubscriptionMetadataSanitizer cut;

    @BeforeEach
    void setUp() {
        cut = new SubscriptionMetadataSanitizer();
    }

    @Test
    void should_return_empty_map_when_metadata_is_null() {
        assertThat(cut.sanitizeAndValidate(null)).isEmpty();
    }

    @Test
    void should_throw_when_metadata_key_is_invalid() {
        Map<String, String> invalidKeyMetadata = Map.of("bad key", "value");

        var throwable = assertThrows(SubscriptionMetadataInvalidException.class, () -> cut.sanitizeAndValidate(invalidKeyMetadata));

        assertThat(throwable.getTechnicalCode()).isEqualTo("subscription.metadata.key.invalid");
        assertThat(throwable.getMessage()).isEqualTo("Invalid metadata key: bad key");
    }

    @Test
    void should_strip_html_tags_and_omit_empty_values() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("field_a", "<script>alert(1)</script>");
        metadata.put("field_b", null);
        metadata.put("field_c", "   ");

        var result = cut.sanitizeAndValidate(metadata);

        assertThat(result).containsEntry("field_a", "alert(1)").doesNotContainKeys("field_b", "field_c");
    }

    @Test
    void should_preserve_special_chars_like_at_sign_in_email() {
        var result = cut.sanitizeAndValidate(Map.of("email", "my@company.com"));

        assertThat(result).containsEntry("email", "my@company.com");
    }

    @Test
    void should_preserve_plain_text_special_chars_without_encoding() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("field_a", "key=value+1");
        metadata.put("field_b", "it's a \"test\"");
        metadata.put("field_c", "code`snippet");

        var result = cut.sanitizeAndValidate(metadata);

        assertThat(result)
            .containsEntry("field_a", "key=value+1")
            .containsEntry("field_b", "it's a \"test\"")
            .containsEntry("field_c", "code`snippet");
    }

    @Test
    void should_accept_values_with_less_than_or_greater_than_after_strip() {
        var result = cut.sanitizeAndValidate(Map.of("key", "x < 5"));
        assertThat(result).containsEntry("key", "x < 5");

        var result2 = cut.sanitizeAndValidate(Map.of("key", "y > 0"));
        assertThat(result2).containsEntry("key", "y > 0");
    }

    @Test
    void should_strip_html_tags_and_keep_text_content() {
        var result = cut.sanitizeAndValidate(Map.of("content", "<b>bold</b>"));

        assertThat(result).containsEntry("content", "bold");
    }
}
