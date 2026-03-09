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

import io.gravitee.rest.api.service.sanitizer.HtmlSanitizer;
import io.gravitee.rest.api.service.v4.exception.SubscriptionMetadataInvalidException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates and sanitizes subscription form metadata (keys, value length, HTML).
 *
 * @author GraviteeSource Team
 */
@Component
public class SubscriptionMetadataSanitizer {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");
    private static final int MAX_VALUE_LENGTH = 1024;
    private static final int MAX_METADATA_COUNT = 25;

    private final HtmlSanitizer htmlSanitizer;

    public SubscriptionMetadataSanitizer(HtmlSanitizer htmlSanitizer) {
        this.htmlSanitizer = htmlSanitizer;
    }

    /**
     * Validates metadata keys and value lengths, then sanitizes each value with HTML sanitizer.
     *
     * @param metadata raw metadata from the client
     * @return sanitized metadata, or empty map if input is null
     * @throws SubscriptionMetadataInvalidException if a key is invalid or a value exceeds max length
     */
    public Map<String, String> sanitizeAndValidate(Map<String, String> metadata) {
        if (metadata == null) {
            return Collections.emptyMap();
        }

        if (metadata.size() > MAX_METADATA_COUNT) {
            throw new SubscriptionMetadataInvalidException(
                SubscriptionMetadataInvalidException.Reason.TOO_MANY,
                "Too many metadata entries. Maximum is " + MAX_METADATA_COUNT + "."
            );
        }

        Map<String, String> sanitizedMetadata = new HashMap<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (key == null || !KEY_PATTERN.matcher(key).matches()) {
                throw new SubscriptionMetadataInvalidException(
                    SubscriptionMetadataInvalidException.Reason.KEY_INVALID,
                    "Invalid metadata key: " + key
                );
            }

            String value = entry.getValue();
            if (value != null && value.length() > MAX_VALUE_LENGTH) {
                throw new SubscriptionMetadataInvalidException(
                    SubscriptionMetadataInvalidException.Reason.VALUE_TOO_LONG,
                    "Metadata value for key '" + key + "' is too long (max " + MAX_VALUE_LENGTH + " characters)."
                );
            }

            String sanitizedValue = htmlSanitizer.sanitize(value);
            if (sanitizedValue == null || sanitizedValue.isBlank()) {
                continue;
            }
            sanitizedMetadata.put(key, sanitizedValue);
        }

        return sanitizedMetadata;
    }
}
