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
package io.gravitee.gamma.rest.core.guardian.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianPublicationDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.GuardianRevision;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListGuardianDeploymentsUseCase {

    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    // Each revision read carries a whole Guardian definition.
    private static final int MAX_PAGE_SIZE = 100;

    private final GuardianRepository guardianRepository;
    private final GuardianPublicationDomainService publications;

    public Output execute(Input input) {
        guardianRepository
            .findByEnvironmentId(input.environmentId(), input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));

        var page = input.pageNumber() > 0 ? input.pageNumber() : DEFAULT_PAGE_NUMBER;
        var size = input.pageSize() > 0 ? Math.min(input.pageSize(), MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        return new Output(publications.revisions(input.environmentId(), input.guardianId(), page, size), size);
    }

    /** @param pageNumber 1-based; the first page when not positive */
    public record Input(String environmentId, String guardianId, int pageNumber, int pageSize) {}

    /** @param perPage the page size actually served */
    public record Output(Page<GuardianRevision> deployments, int perPage) {}
}
