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
package io.gravitee.repository.management.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@EqualsAndHashCode
public class Event implements Serializable {

    /**
     * The event ID
     */
    private String id;

    /**
     * The list of ids of the environments the event is attached to
     */
    private Set<String> environments;

    /**
     * The event Type
     */
    private EventType type;

    /**
     * The event payload
     */
    private String payload;

    /**
     * The event parent
     */
    private String parentId;

    /**
     * The event properties
     */
    private Map<String, String> properties;

    /**
     * The event creation date
     */
    private Date createdAt;

    /**
     * The event last updated date
     */
    private Date updatedAt;

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
        ORGANIZATIONS_HRIDS_PROPERTY("organizations_hrids");

        private final String value;
    }
}
