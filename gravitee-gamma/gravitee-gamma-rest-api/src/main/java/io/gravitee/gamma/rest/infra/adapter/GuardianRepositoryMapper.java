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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;

/**
 * The three translations between a stored Guardian row and the core model: the serialized definition, which is the
 * record of truth, the phase and lifecycle enums the SPI declares as its own, and the timestamps it declares as
 * {@link Date}.
 *
 * <p>Kept apart from {@link GammaGuardianRepositoryAdapter} so that calling the store and reading the fields of
 * another object stay two jobs rather than one class.
 *
 * @author GraviteeSource Team
 */
final class GuardianRepositoryMapper {

    private GuardianRepositoryMapper() {}

    static Guardian toCore(io.gravitee.repository.management.model.Guardian source) {
        if (source == null) {
            return null;
        }
        return Guardian.builder()
            .definition(deserialize(source.getDefinition(), source.getId()))
            .description(source.getDescription())
            .lifecycleState(source.getLifecycleState() == null ? null : GuardianLifecycleState.valueOf(source.getLifecycleState().name()))
            .deployedAt(toInstant(source.getDeployedAt()))
            .createdAt(toInstant(source.getCreatedAt()))
            .updatedAt(toInstant(source.getUpdatedAt()))
            .build();
    }

    static io.gravitee.repository.management.model.Guardian toRepository(Guardian source) {
        var target = new io.gravitee.repository.management.model.Guardian();
        target.setDefinition(serialize(source.getDefinition(), source.getId()));
        target.setId(source.getId());
        target.setEnvironmentId(source.getEnvironmentId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setPhase(
            source.getPhase() == null ? null : io.gravitee.repository.management.model.Guardian.Phase.valueOf(source.getPhase().name())
        );
        target.setLifecycleState(
            source.getLifecycleState() == null
                ? null
                : io.gravitee.repository.management.model.GuardianLifecycleState.valueOf(source.getLifecycleState().name())
        );
        target.setDeployedAt(toDate(source.getDeployedAt()));
        target.setCreatedAt(toDate(source.getCreatedAt()));
        target.setUpdatedAt(toDate(source.getUpdatedAt()));
        return target;
    }

    /**
     * Reads the stored definition, or fails. A row whose definition cannot be read carries no name, no
     * phase and no guardrail sequence, so returning a half-built Guardian would hand the console an
     * entry it can neither render nor repair; the search maps every row, so one such row fails the
     * whole page rather than itself, which is the point.
     */
    private static io.gravitee.definition.model.v4.guardian.Guardian deserialize(String definition, String guardianId) {
        if (definition == null || definition.isBlank()) {
            throw new TechnicalDomainException("Guardian '%s' has no stored definition.".formatted(guardianId));
        }
        try {
            return GraviteeJacksonMapper.getInstance().readValue(definition, io.gravitee.definition.model.v4.guardian.Guardian.class);
        } catch (IOException e) {
            throw new TechnicalDomainException("Failed to read the definition of guardian '%s'.".formatted(guardianId), e);
        }
    }

    private static String serialize(io.gravitee.definition.model.v4.guardian.Guardian definition, String guardianId) {
        try {
            return GraviteeJacksonMapper.getInstance().writeValueAsString(definition);
        } catch (IOException e) {
            throw new TechnicalDomainException("Failed to write the definition of guardian '%s'.".formatted(guardianId), e);
        }
    }

    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    private static Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }
}
