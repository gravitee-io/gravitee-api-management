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
package io.gravitee.apim.core.api.model;

import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ApiWithFlows extends Api {

    private List<? extends AbstractFlow> flows;

    public ApiWithFlows(Api api, List<? extends AbstractFlow> flows) {
        super(
            api.getId(),
            api.getEnvironmentId(),
            api.getCrossId(),
            api.getName(),
            api.getDescription(),
            api.getVersion(),
            api.getOriginContext(),
            api.getDefinitionVersion(),
            api.getApiDefinitionV4(),
            api.getApiDefinition(),
            api.getFederatedApiDefinition(),
            api.getNativeApiDefinition(),
            api.getType(),
            api.getDeployedAt(),
            api.getCreatedAt(),
            api.getUpdatedAt(),
            api.getVisibility(),
            api.getLifecycleState(),
            api.getPicture(),
            api.getGroups(),
            api.getCategories(),
            api.getLabels(),
            api.isDisableMembershipNotifications(),
            api.getApiLifecycleState(),
            api.getBackground()
        );
        this.flows = flows;
    }

    public Api toApi() {
        return super.toBuilder().build();
    }
}
