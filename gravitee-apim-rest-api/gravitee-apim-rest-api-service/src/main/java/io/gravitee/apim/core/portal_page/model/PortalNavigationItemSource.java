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
package io.gravitee.apim.core.portal_page.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class PortalNavigationItemSource {

    private static final ObjectMapper CONFIGURATION_READER = new ObjectMapper();

    @Nonnull
    private String sourceType;

    @Nonnull
    private String sourceConfiguration;

    @Builder.Default
    private boolean useAutoFetch = false;

    @Nullable
    private String fetchCron;

    @Nullable
    private Instant lastFetchedAt;

    @Nullable
    private String lastFetchError;

    /** Two sources share an origin when they point at the same thing, whatever the fetch state around them. */
    public boolean sameOriginAs(@Nullable PortalNavigationItemSource other) {
        return (
            other != null &&
            Objects.equals(sourceType, other.sourceType) &&
            sameConfiguration(sourceConfiguration, other.sourceConfiguration)
        );
    }

    // JSON comparison: formatting and key order must not count as an origin change
    private static boolean sameConfiguration(String current, String updated) {
        if (Objects.equals(current, updated)) {
            return true;
        }
        if (current == null || updated == null) {
            return false;
        }
        try {
            return CONFIGURATION_READER.readTree(current).equals(CONFIGURATION_READER.readTree(updated));
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
