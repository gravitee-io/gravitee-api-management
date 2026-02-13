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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.service.sanitizer.HtmlSanitizer;
import io.gravitee.rest.api.service.v4.exception.SubscriptionMetadataInvalidException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionMetadataSanitizerTest {

    @Mock
    private HtmlSanitizer htmlSanitizer;

    private SubscriptionMetadataSanitizer cut;

    @BeforeEach
    void setUp() {
        cut = new SubscriptionMetadataSanitizer(htmlSanitizer);
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
    void should_throw_when_metadata_value_is_too_long() {
        Map<String, String> tooLongValue = Map.of("valid_key", "a".repeat(1025));

        var throwable = assertThrows(SubscriptionMetadataInvalidException.class, () -> cut.sanitizeAndValidate(tooLongValue));

        assertThat(throwable.getTechnicalCode()).isEqualTo("subscription.metadata.value.too_long");
        assertThat(throwable.getMessage()).isEqualTo("Metadata value for key 'valid_key' is too long (max 1024 characters).");
    }

    @Test
    void should_throw_when_metadata_count_exceeds_maximum() {
        Map<String, String> tooMany = new HashMap<>();
        for (int i = 0; i < 26; i++) {
            tooMany.put("key_" + i, "value");
        }

        var throwable = assertThrows(SubscriptionMetadataInvalidException.class, () -> cut.sanitizeAndValidate(tooMany));

        assertThat(throwable.getTechnicalCode()).isEqualTo("subscription.metadata.too_many");
        assertThat(throwable.getMessage()).isEqualTo("Too many metadata entries. Maximum is 25.");
    }

    @Test
    void should_accept_metadata_at_maximum_count() {
        when(htmlSanitizer.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));
        Map<String, String> maxAllowed = new HashMap<>();
        for (int i = 0; i < 25; i++) {
            maxAllowed.put("key_" + i, "value");
        }

        var result = cut.sanitizeAndValidate(maxAllowed);

        assertThat(result).hasSize(25);
    }

    @Test
    void should_sanitize_values_and_omit_empty_values() {
        when(htmlSanitizer.sanitize("<script>alert(1)</script>")).thenReturn("alert(1)");
        when(htmlSanitizer.sanitize("   ")).thenReturn("   ");
        when(htmlSanitizer.sanitize(isNull())).thenReturn(null);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("field_a", "<script>alert(1)</script>");
        metadata.put("field_b", null);
        metadata.put("field_c", "   ");

        var result = cut.sanitizeAndValidate(metadata);

        assertThat(result).containsEntry("field_a", "alert(1)").doesNotContainKeys("field_b", "field_c");
    }
}
