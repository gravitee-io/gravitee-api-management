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

package io.gravitee.rest.api.model.federation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Read model for Federated API.
 *
 * <p>This model should only be used for read use cases (search, getById)</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FederatedApiEntity implements GenericApiEntity {

    private String id;

    private String crossId;

    private String name;

    private String apiVersion;

    @Builder.Default
    private DefinitionVersion definitionVersion = DefinitionVersion.FEDERATED;

    private Date deployedAt;

    private Date createdAt;

    private Date updatedAt;

    private String description;

    private Set<String> groups;

    private Visibility visibility;

    private PrimaryOwnerEntity primaryOwner;

    private String picture;

    private String pictureUrl;

    private Set<String> categories;

    private List<String> labels;

    private OriginContext.Integration originContext;

    private ApiLifecycleState lifecycleState;

    private boolean disableMembershipNotifications;

    private String background;

    private String backgroundUrl;

    private String provider;

    private String integrationName;

    @JsonIgnore
    private String referenceType;

    @JsonIgnore
    private String referenceId;

    @Override
    public Lifecycle.State getState() {
        return null;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return null;
    }

    @Override
    public void setMetadata(Map<String, Object> metadata) {
        // Federated APIs have no metadata for now
    }

    @Override
    public Set<String> getTags() {
        return null;
    }

    @Override
    public WorkflowState getWorkflowState() {
        return null;
    }

    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static final class OriginContextView extends OriginContext.Integration {

        private final String provider;
        private final String integrationName;

        public OriginContextView(OriginContext.Integration baseCtx, String provider, String integrationName) {
            super(baseCtx.integrationId());
            this.provider = provider;
            this.integrationName = integrationName;
        }

        public String provider() {
            return provider;
        }

        public String integrationName() {
            return integrationName;
        }
    }
}
