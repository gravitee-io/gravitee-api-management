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
package io.gravitee.apim.core.documentation.model;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.v4.ApiType;
import java.util.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ApiFreemarkerTemplate {

    private String id;
    private String name;
    private String description;
    private String version;
    private DefinitionVersion definitionVersion;

    @Builder.Default
    private ApiType type = ApiType.PROXY;

    private String picture;
    private PrimaryOwnerApiTemplateData primaryOwner;

    @Builder.Default
    private Api.Visibility visibility = Api.Visibility.PRIVATE;

    @Builder.Default
    private Api.LifecycleState state = Api.LifecycleState.STOPPED;

    @Builder.Default
    private Api.ApiLifecycleState lifecycleState = Api.ApiLifecycleState.CREATED;

    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    private Date deployedAt;
    private Date createdAt;
    private Date updatedAt;

    // V2 Attributes
    private Proxy proxy;

    public ApiFreemarkerTemplate(Api api, Map<String, String> metadata, PrimaryOwnerEntity primaryOwner) {
        if (api == null) {
            return;
        }
        if (DefinitionVersion.V4.equals(api.getDefinitionVersion())) {
            if (api.getApiDefinitionHttpV4() != null) {
                this.tags = api.getApiDefinitionHttpV4().getTags();
            }
        } else if (api.getApiDefinition() != null) {
            this.tags = api.getApiDefinition().getTags();
            this.proxy = api.getApiDefinition().getProxy();
        }

        this.state = api.getLifecycleState();
        this.lifecycleState = api.getApiLifecycleState();
        this.id = api.getId();
        this.name = api.getName();
        this.description = api.getDescription();
        this.version = api.getVersion();
        this.definitionVersion = api.getDefinitionVersion();
        this.type = api.getType();
        this.picture = api.getPicture();
        this.visibility = api.getVisibility();
        if (api.getDeployedAt() != null) {
            this.deployedAt = Date.from(api.getDeployedAt().toInstant());
        }
        if (api.getCreatedAt() != null) {
            this.createdAt = Date.from(api.getCreatedAt().toInstant());
        }
        if (api.getUpdatedAt() != null) {
            this.updatedAt = Date.from(api.getUpdatedAt().toInstant());
        }

        this.metadata = metadata;
        this.primaryOwner = PrimaryOwnerApiTemplateData.from(primaryOwner);
    }
}
