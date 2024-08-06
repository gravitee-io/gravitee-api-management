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
package io.gravitee.apim.core.event.model;

import io.gravitee.rest.api.model.EventType;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private String id;
    private EventType type;
    private String payload;
    private String parentId;

    @Builder.Default
    private EnumMap<EventProperties, String> properties = new EnumMap<>(EventProperties.class);

    @Builder.Default
    private Set<String> environments = Set.of();

    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    @RequiredArgsConstructor
    @Getter
    public enum EventProperties {
        ID("id"),
        API_ID("api_id"),
        DICTIONARY_ID("dictionary_id"),
        ORIGIN("origin"),
        USER("user"),
        DEPLOYMENT_LABEL("deployment_label"),
        DEPLOYMENT_NUMBER("deployment_number"),
        ORGANIZATION_ID("organization_id"),
        API_DEBUG_STATUS("api_debug_status"),
        GATEWAY_ID("gateway_id"),
        ENVIRONMENTS_HRIDS_PROPERTY("environments_hrids"),
        ORGANIZATIONS_HRIDS_PROPERTY("organizations_hrids"),
        SHARED_POLICY_GROUP_ID("shared_policy_group_id");

        private static final Map<String, EventProperties> LABELS_MAP;

        static {
            LABELS_MAP = Arrays.stream(EventProperties.values()).collect(Collectors.toMap(entry -> entry.label, Function.identity()));
        }

        private final String label;

        public static EventProperties fromLabel(final String label) {
            if (label != null) {
                return LABELS_MAP.get(label);
            }
            return null;
        }
    }
}
