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
package io.gravitee.gamma.rest.core.guardrail.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailPublicationDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailRevision;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListGuardrailDeploymentsUseCase {

    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    // Each revision read carries a whole Guardrail definition.
    private static final int MAX_PAGE_SIZE = 100;

    private final GuardrailRepository guardrailRepository;
    private final GuardrailPublicationDomainService publications;

    public Output execute(Input input) {
        guardrailRepository
            .findByEnvironmentId(input.environmentId(), input.guardrailId())
            .orElseThrow(() -> new GuardrailNotFoundException(input.guardrailId()));

        var page = input.pageNumber() > 0 ? input.pageNumber() : DEFAULT_PAGE_NUMBER;
        var size = input.pageSize() > 0 ? Math.min(input.pageSize(), MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        return new Output(publications.revisions(input.environmentId(), input.guardrailId(), page, size), size);
    }

    /** @param pageNumber 1-based; the first page when not positive */
    public record Input(String environmentId, String guardrailId, int pageNumber, int pageSize) {}

    /** @param perPage the page size actually served */
    public record Output(Page<GuardrailRevision> deployments, int perPage) {}
}
