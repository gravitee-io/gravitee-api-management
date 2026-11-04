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

import java.util.Date;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;

/**
 * A Guardian is a reusable, independently deployed sequence of guardrails and the resources they need,
 * defined at environment level and attached to APIs.
 *
 * <p>It follows the {@link SharedPolicyGroup} model: the Guardian definition the gateway reads is kept
 * serialized in {@link #definition}, while the fields the console searches, filters and sorts on stay
 * columns. The API types a Guardian is designed for are not among them: the definition carries them as a
 * set, nothing queries them, and sorting on a set has no defined order.</p>
 *
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
@ToString
@With
public class Guardian {

    /**
     * Identifier of the Guardian, which is also its EntityID.
     */
    private String id;

    private String environmentId;

    private String name;

    /**
     * Free text the console shows beside the name. Control-plane only: the gateway never reads it, so
     * it is a column of its own rather than part of the serialized definition.
     */
    private String description;

    private Phase phase;

    /**
     * The guardrail sequence and the resources, serialized.
     */
    private String definition;

    private Date createdAt;

    private Date updatedAt;

    private Date deployedAt;

    private GuardianLifecycleState lifecycleState;

    public Guardian(Guardian cloned) {
        this.id = cloned.id;
        this.environmentId = cloned.environmentId;
        this.name = cloned.name;
        this.description = cloned.description;
        this.phase = cloned.phase;
        this.definition = cloned.definition;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
        this.deployedAt = cloned.deployedAt;
        this.lifecycleState = cloned.lifecycleState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Guardian guardian = (Guardian) o;
        return Objects.equals(id, guardian.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean isDeployed() {
        return lifecycleState == GuardianLifecycleState.DEPLOYED;
    }

    public enum Phase {
        REQUEST,
        RESPONSE,
    }
}
