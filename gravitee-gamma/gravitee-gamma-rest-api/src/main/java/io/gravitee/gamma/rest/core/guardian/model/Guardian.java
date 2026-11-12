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
package io.gravitee.gamma.rest.core.guardian.model;

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.definition.model.v4.resource.Resource;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Guardian: a reusable sequence of guardrails and the resources they need, defined at environment
 * level, deployed on its own and attached to APIs.
 *
 * <p>What a Guardian <em>is</em> lives in its definition — the model the gateway reads — and this
 * class reads the rest through it rather than keeping a second copy, so the two cannot drift apart.
 *
 * <p>Four things are its own, because the gateway has no use for them: the description the console
 * shows, the lifecycle state, the deployment stamp, and the two timestamps. {@code deployedAt} sits
 * here rather than in the definition because only a deployment writes it.
 *
 * <p>The definition field is spelled out rather than imported: a domain class named {@code Guardian}
 * cannot import another one.
 *
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Guardian {

    private io.gravitee.definition.model.v4.guardian.Guardian definition;

    private String description;
    private GuardianLifecycleState lifecycleState;
    private Instant deployedAt;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * The Guardian a creation payload describes, scoped to the environment it is created in — which
     * comes from the path, never from the payload.
     */
    public static Guardian from(CreateGuardian createGuardian, String environmentId) {
        return Guardian.builder()
            .definition(createGuardian.toDefinition().toBuilder().environmentId(environmentId).build())
            .description(createGuardian.getDescription())
            .build();
    }

    /**
     * Replaces the definition and the description wholesale, the way a {@code PUT} does. The identity
     * comes from the path and the deployment stamp from a deployment, so neither is the caller's to
     * send: the first is carried over explicitly, the second simply survives in the builder.
     */
    public Guardian update(UpdateGuardianCommand command) {
        return this.toBuilder()
            .definition(command.getDefinition().toBuilder().id(getId()).environmentId(getEnvironmentId()).build())
            .description(command.getDescription())
            .updatedAt(TimeProvider.instantNow())
            .build();
    }

    public String getId() {
        return definition == null ? null : definition.getId();
    }

    public String getEnvironmentId() {
        return definition == null ? null : definition.getEnvironmentId();
    }

    public String getName() {
        return definition == null ? null : definition.getName();
    }

    public Phase getPhase() {
        return definition == null ? null : definition.getPhase();
    }

    public Set<ApiType> getApiTypes() {
        return definition == null ? null : definition.getApiTypes();
    }

    public List<Step> getPolicies() {
        return definition == null ? null : definition.getPolicies();
    }

    public List<Resource> getResources() {
        return definition == null ? null : definition.getResources();
    }

    public boolean isDeployed() {
        return lifecycleState == GuardianLifecycleState.DEPLOYED;
    }
}
