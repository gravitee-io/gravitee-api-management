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

import io.gravitee.rest.api.service.v4.exception.SubscriptionMetadataInvalidException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Sanitizes subscription metadata: validates key format and strips HTML tags from values.
 * Values are plain text; HTML tags are stripped to prevent XSS when metadata is rendered.
 * No HTML encoding is applied, so characters like {@code @}, {@code +}, {@code =} are stored as-is.
 *
 * <p>Business-level limits (max value length, max entry count) are enforced separately by
 * {@link io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSubmissionValidator}
 * for subscriptions that go through a subscription form.</p>
 *
 * @author GraviteeSource Team
 */
@Component
public class SubscriptionMetadataSanitizer {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    /**
     * Validates metadata key format, strips HTML tags from each value, and omits blank values.
     * Any remaining characters (including {@code <}, {@code >}, non-Latin, etc.) are stored as-is.
     *
     * @param metadata raw metadata from the client
     * @return sanitized metadata, or empty map if input is null
     * @throws SubscriptionMetadataInvalidException if a key does not match the required format
     */
    public Map<String, String> sanitizeAndValidate(Map<String, String> metadata) {
        if (metadata == null) {
            return Collections.emptyMap();
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

            String sanitizedValue = stripHtmlTags(entry.getValue());
            if (sanitizedValue == null || sanitizedValue.isBlank()) {
                continue;
            }
            sanitizedMetadata.put(key, sanitizedValue);
        }

        return sanitizedMetadata;
    }

    /**
     * Removes HTML tags from plain-text metadata. Used instead of OWASP HTML Sanitizer so that
     * special characters (e.g. {@code @}, {@code +}, {@code =}) are not encoded.
     */
    private static String stripHtmlTags(String content) {
        if (content == null) {
            return null;
        }
        return HTML_TAG.matcher(content).replaceAll("").trim();
    }
}
