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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Mongo model for Api
 *
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Getter
@ToString
@EqualsAndHashCode(of = { "id" }, callSuper = false)
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}apis")
public class ApiMongo extends Auditable {

    @Id
    private String id;

    /**
     * The api crossId uniquely identifies an API across environments.
     * Apis promoted between environments will share the same crossId.
     */
    private String crossId;

    /**
     * The origin of the api (management, kubernetes, ...).
     */
    private String origin;

    /**
     * How the api is managed by the origin (fully, api_definition_only, ...)
     */
    private String mode;

    /**
     * Source of sync for the API definition (either kubernetes or management)
     */
    private String syncFrom;

    /** The integration id for Federated API */
    private String integrationId;

    @Field("name")
    private String name;

    private String environmentId;

    private String version;

    private String description;

    private String definitionVersion;

    private String definition;

    private String type;

    private String lifecycleState;

    private String visibility;

    private Date deployedAt;

    private String picture;

    private Set<String> groups;

    private Set<String> categories;

    private List<String> labels;

    private List<ApiMetadataMongo> metadatas;

    private String apiLifecycleState;

    private boolean disableMembershipNotifications;

    private String background;

    public ApiMongo setIntegrationId(String integrationId) {
        this.integrationId = integrationId;
        return this;
    }

    public ApiMongo setDefinitionVersion(final String definitionVersion) {
        this.definitionVersion = definitionVersion;
        return this;
    }

    public ApiMongo setType(final String type) {
        this.type = type;
        return this;
    }
}
