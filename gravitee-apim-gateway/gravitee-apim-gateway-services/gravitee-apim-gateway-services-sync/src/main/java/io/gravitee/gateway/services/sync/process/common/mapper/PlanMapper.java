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
package io.gravitee.gateway.services.sync.process.common.mapper;

import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.List;
import java.util.Set;

/**
 * Maps repository Plan to definition Plan for use in the gateway security chain.
 *
 * @author GraviteeSource Team
 */
public final class PlanMapper {

    private PlanMapper() {}

    public static Plan toDefinition(io.gravitee.repository.management.model.Plan repoPlan) {
        if (repoPlan == null) {
            return null;
        }

        PlanSecurity security = PlanSecurity.builder()
            .type(repoPlan.getSecurity() != null ? repoPlan.getSecurity().name() : "api-key")
            .configuration(repoPlan.getSecurityDefinition())
            .build();

        PlanStatus status = toPlanStatus(repoPlan.getStatus());
        PlanMode mode = repoPlan.getMode() != null ? PlanMode.valueOf(repoPlan.getMode().name()) : PlanMode.STANDARD;

        return Plan.builder()
            .id(repoPlan.getId())
            .name(repoPlan.getName())
            .security(security)
            .mode(mode)
            .selectionRule(repoPlan.getSelectionRule())
            .tags(repoPlan.getTags() != null ? Set.copyOf(repoPlan.getTags()) : null)
            .status(status)
            .flows(List.of())
            .build();
    }

    private static PlanStatus toPlanStatus(io.gravitee.repository.management.model.Plan.Status repoStatus) {
        if (repoStatus == null) {
            return PlanStatus.PUBLISHED;
        }
        return switch (repoStatus) {
            case PUBLISHED -> PlanStatus.PUBLISHED;
            case DEPRECATED -> PlanStatus.DEPRECATED;
            case STAGING -> PlanStatus.STAGING;
            case CLOSED -> PlanStatus.CLOSED;
            default -> PlanStatus.PUBLISHED;
        };
    }
}
