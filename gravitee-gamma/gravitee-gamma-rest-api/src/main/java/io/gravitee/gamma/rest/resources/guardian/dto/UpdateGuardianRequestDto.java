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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import io.gravitee.gamma.rest.resources.RejectUnknownProperties;
import java.util.List;
import java.util.Set;

/**
 * What the console sends to update a Guardian: the whole definition again, plus the description.
 *
 * <p>A {@code PUT} replaces, it does not patch — a property this body leaves out is left out of the
 * stored Guardian too, so a client that edits one setting sends back everything it read.
 *
 * <p>Which is why the fields only the store owns are ignored rather than refused. Sending back what
 * {@code GET} returned is exactly what the paragraph above asks for, and that response carries
 * {@code id}, {@code lifecycleState}, {@code deployedAt}, {@code createdAt} and {@code updatedAt};
 * without this the obedient client got a 400 naming the first of them. They are read here and
 * dropped: the id comes from the path, the lifecycle and the deployment stamp belong to a deployment,
 * and the two timestamps are the store's. A genuinely unknown key is still refused, which is what
 * {@link RejectUnknownProperties} is for.
 *
 * <p>Constraints live on {@link UpdateGuardianCommand}, not here; see
 * {@link CreateGuardianRequestDto}.
 *
 * @author GraviteeSource Team
 */
@RejectUnknownProperties
@JsonIgnoreProperties({ "id", "lifecycleState", "deployedAt", "createdAt", "updatedAt" })
public record UpdateGuardianRequestDto(
    String name,
    String description,
    Guardian.Phase phase,
    Set<ApiType> apiTypes,
    List<Step> policies,
    List<Resource> resources
) {
    public UpdateGuardianCommand toCore() {
        return UpdateGuardianCommand.builder()
            .definition(Guardian.builder().name(name).phase(phase).apiTypes(apiTypes).policies(policies).resources(resources).build())
            .description(description)
            .build();
    }
}
