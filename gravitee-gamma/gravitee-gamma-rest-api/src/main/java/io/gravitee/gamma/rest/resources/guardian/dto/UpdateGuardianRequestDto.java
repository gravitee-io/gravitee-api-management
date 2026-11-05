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
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import io.gravitee.gamma.rest.resources.RejectUnknownProperties;
import java.util.List;
import java.util.Set;

/**
 * What the console sends to update a Guardian: the whole definition again, plus the description.
 *
 * <p>A {@code PUT} replaces, it does not patch — a property this body leaves out is left out of the
 * stored Guardian too, so a client that edits one setting sends back everything it read. The id is
 * absent on purpose: it comes from the path, and a body carrying a second one could only disagree
 * with it.
 *
 * <p>Constraints live on {@link UpdateGuardianCommand}, not here; see
 * {@link CreateGuardianRequestDto}.
 *
 * @author GraviteeSource Team
 */
@RejectUnknownProperties
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
