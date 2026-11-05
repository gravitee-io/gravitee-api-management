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
package io.gravitee.gamma.rest.resources.guardian.dto;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.guardian.model.CreateGuardian;
import io.gravitee.gamma.rest.resources.RejectUnknownProperties;
import java.util.List;
import java.util.Set;

/**
 * What the console sends to create a Guardian: the whole definition, guardrail sequence included,
 * plus the description the control plane keeps beside it.
 *
 * <p>Two fields are absent on purpose. The environment, already in the path. And the deployment
 * stamp, which only a deployment may write.
 *
 * <p>This record carries no validation constraints: they live on {@link CreateGuardian}, so they hold
 * for every caller of the use case and not only for one arriving here. The refusal is the same 400
 * either way — the host maps {@code ConstraintViolationException} too.
 *
 * @author GraviteeSource Team
 */
@RejectUnknownProperties
public record CreateGuardianRequestDto(
    String id,
    String name,
    String description,
    Guardian.Phase phase,
    Set<ApiType> apiTypes,
    List<Step> policies,
    List<Resource> resources
) {
    public CreateGuardian toCore() {
        return CreateGuardian.builder()
            .id(id)
            .name(name)
            .description(description)
            .phase(phase)
            .apiTypes(apiTypes)
            .policies(policies)
            .resources(resources)
            .build();
    }
}
