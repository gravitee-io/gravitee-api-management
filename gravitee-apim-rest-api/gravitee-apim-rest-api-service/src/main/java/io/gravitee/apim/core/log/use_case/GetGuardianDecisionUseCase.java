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
package io.gravitee.apim.core.log.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.log.model.GuardianDecision;
import io.gravitee.apim.core.log.query_service.GuardianDecisionQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;
import java.util.Set;

/**
 * Reads one decision of a Guardian by its id, in the environment of the execution context. A decision of an API out of
 * the given scope reads as absent, as an unknown one does, so that the caller cannot tell the two apart.
 *
 * @author GraviteeSource Team
 */
@UseCase
public class GetGuardianDecisionUseCase {

    private final GuardianDecisionQueryService guardianDecisionQueryService;

    public GetGuardianDecisionUseCase(GuardianDecisionQueryService guardianDecisionQueryService) {
        this.guardianDecisionQueryService = guardianDecisionQueryService;
    }

    /**
     * @param decisionId the event id of the decision
     * @param apiIds the APIs the read may cover; {@code null} for every API of the environment, empty for none
     */
    public record Input(ExecutionContext executionContext, String decisionId, Set<String> apiIds) {}

    public record Output(Optional<GuardianDecision> decision) {}

    public Output execute(Input input) {
        return new Output(
            guardianDecisionQueryService
                .findById(input.executionContext(), input.decisionId())
                .filter(decision -> input.apiIds() == null || input.apiIds().contains(decision.apiId()))
        );
    }
}
