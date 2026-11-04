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
package io.gravitee.apim.core.guardian.model;

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Guardian: a reusable sequence of guardrails and the resources they need, defined at environment
 * level, deployed on its own and attached to APIs.
 *
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Guardian {

    private String id;
    private String environmentId;
    private String name;
    private String description;
    private String version;
    private ApiType apiType;
    private GuardianPhase phase;

    /**
     * The guardrail sequence and the resources, serialized. Opaque here: it is written and read by
     * the Design tab, and carried through unchanged by the general settings update.
     */
    private String definition;

    private ZonedDateTime deployedAt;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private GuardianLifecycleState lifecycleState;

    public static Guardian from(CreateGuardian createGuardian) {
        return Guardian.builder()
            .id(createGuardian.getId())
            .name(createGuardian.getName())
            .description(createGuardian.getDescription())
            .version(createGuardian.getVersion())
            .apiType(createGuardian.getApiType())
            .phase(createGuardian.getPhase())
            .build();
    }

    /**
     * Applies the general settings on this Guardian. Everything else, the definition included, is
     * carried over untouched.
     */
    public Guardian update(UpdateGuardian updateGuardian) {
        return this.toBuilder()
            .name(updateGuardian.getName() == null ? this.getName() : updateGuardian.getName())
            .description(updateGuardian.getDescription() == null ? this.getDescription() : updateGuardian.getDescription())
            .version(updateGuardian.getVersion() == null ? this.getVersion() : updateGuardian.getVersion())
            .updatedAt(TimeProvider.now())
            .build();
    }

    public boolean isDeployed() {
        return lifecycleState == GuardianLifecycleState.DEPLOYED;
    }
}
