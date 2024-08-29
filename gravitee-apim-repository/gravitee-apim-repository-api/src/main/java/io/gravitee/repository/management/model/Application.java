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
package io.gravitee.repository.management.model;

import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.definition.model.Origin;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Application {

    public static final String METADATA_CLIENT_ID = "client_id";
    public static final String METADATA_TYPE = "type";
    public static final String METADATA_REGISTRATION_PAYLOAD = "registration_payload";
    public static final String METADATA_CLIENT_CERTIFICATE = "client_certificate";
    public static final String METADATA_ADDITIONAL_CLIENT_METADATA = "additional_client_metadata";

    public enum AuditEvent implements Audit.AuditEvent {
        APPLICATION_CREATED,
        APPLICATION_UPDATED,
        APPLICATION_ARCHIVED,
        APPLICATION_RESTORED,
    }

    /**
     * The application ID.
     */
    @EqualsAndHashCode.Include
    private String id;

    /**
     * The ID of the environment the application is attached to
     */
    private String environmentId;

    /**
     * The application name
     */
    private String name;

    /**
     * The application description
     */
    private String description;

    /**
     * The application picture
     */
    private String picture;

    /**
     * Domain used by the application, if relevant
     */
    private String domain;

    /**
     * The application creation date
     */
    private Date createdAt;

    /**
     * The application last updated date
     */
    private Date updatedAt;

    /**
     * the application group
     */
    private Set<String> groups = new HashSet<>();

    private ApplicationStatus status;

    private ApplicationType type;

    private Map<String, String> metadata;

    private boolean disableMembershipNotifications;

    private String background;

    private ApiKeyMode apiKeyMode = ApiKeyMode.UNSPECIFIED;

    private Origin origin;

    public Application(Application cloned) {
        this.id = cloned.id;
        this.environmentId = cloned.environmentId;
        this.name = cloned.name;
        this.description = cloned.description;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
        if (!isEmpty(cloned.groups)) {
            this.groups.addAll(cloned.groups);
        }
        this.status = cloned.status;
        this.disableMembershipNotifications = cloned.disableMembershipNotifications;
        this.background = cloned.background;
        this.domain = cloned.domain;
        this.apiKeyMode = cloned.apiKeyMode;
        this.picture = cloned.picture;
        this.type = cloned.type;
        this.origin = cloned.origin;
        this.metadata = cloned.metadata != null ? new HashMap<>(cloned.metadata) : null;
    }

    public boolean addGroup(String group) {
        if (groups == null) {
            groups = new HashSet<>();
        }
        return groups.add(group);
    }

    public Set<String> getGroups() {
        return groups != null ? new HashSet<>(groups) : new HashSet<>();
    }

    public void setGroups(Collection<String> groups) {
        this.groups = groups != null ? new HashSet<>(groups) : null;
    }
}
