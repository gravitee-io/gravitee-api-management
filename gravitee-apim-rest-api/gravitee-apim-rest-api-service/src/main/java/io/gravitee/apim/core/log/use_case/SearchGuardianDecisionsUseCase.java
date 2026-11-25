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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.log.model.GuardianDecision;
import io.gravitee.apim.core.log.model.GuardianDecisionFilters;
import io.gravitee.apim.core.log.query_service.GuardianDecisionQueryService;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;

/**
 * Searches the decisions Guardians reported in the environment of the execution context, newest first. Callers
 * authorize the read and pass the APIs it may cover in {@link GuardianDecisionFilters#apiIds()}.
 *
 * @author GraviteeSource Team
 */
@UseCase
public class SearchGuardianDecisionsUseCase {

    public static final int MAX_PAGE_SIZE = 1_000;

    private final GuardianDecisionQueryService guardianDecisionQueryService;

    public SearchGuardianDecisionsUseCase(GuardianDecisionQueryService guardianDecisionQueryService) {
        this.guardianDecisionQueryService = guardianDecisionQueryService;
    }

    /**
     * @param page the page, from 1
     * @param size the page size, from 1 to {@link #MAX_PAGE_SIZE}
     */
    public record Input(ExecutionContext executionContext, GuardianDecisionFilters filters, int page, int size) {}

    /** @param total the count of every matching decision, across every page */
    public record Output(long total, List<GuardianDecision> decisions) {}

    public Output execute(Input input) {
        validate(input);
        var response = guardianDecisionQueryService.search(
            input.executionContext(),
            input.filters(),
            new PageableImpl(input.page(), input.size())
        );
        return new Output(response.total(), response.logs());
    }

    private static void validate(Input input) {
        if (input.page() < 1) {
            throw new ValidationDomainException("The page must be 1 or more, was " + input.page());
        }
        if (input.size() < 1 || input.size() > MAX_PAGE_SIZE) {
            throw new ValidationDomainException("The page size must be between 1 and " + MAX_PAGE_SIZE + ", was " + input.size());
        }
        var filters = input.filters();
        if (filters.from() != null && filters.to() != null && filters.from() > filters.to()) {
            throw new ValidationDomainException(
                "The time range must start before it ends, was from " + filters.from() + " to " + filters.to()
            );
        }
    }
}
