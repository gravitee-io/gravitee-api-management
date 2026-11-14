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
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianPublicationDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.GuardianRevision;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * The deployment history of a Guardian: the revisions deployed to the gateways, newest first.
 */
@UseCase
@RequiredArgsConstructor
public class ListGuardianDeploymentsUseCase {

    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    // Each revision read carries a whole Guardian definition.
    private static final int MAX_PAGE_SIZE = 100;

    private final GuardianRepository guardianRepository;
    private final GuardianPublicationDomainService publications;
    private final UserCrudService userCrudService;

    public Output execute(Input input) {
        guardianRepository
            .findByEnvironmentId(input.environmentId(), input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));

        var page = input.pageNumber() > 0 ? input.pageNumber() : DEFAULT_PAGE_NUMBER;
        var size = input.pageSize() > 0 ? Math.min(input.pageSize(), MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        var revisions = publications.revisions(input.environmentId(), input.guardianId(), page, size);

        var deployers = deployers(revisions.getContent());
        var deployments = revisions
            .getContent()
            .stream()
            .map(revision -> new Deployment(revision, deployer(deployers, revision.deployerId())))
            .toList();
        return new Output(new Page<>(deployments, revisions.getPageNumber(), deployments.size(), revisions.getTotalElements()), size);
    }

    private Map<String, BaseUserEntity> deployers(List<GuardianRevision> revisions) {
        var ids = revisions.stream().map(GuardianRevision::deployerId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userCrudService.findBaseUsersByIds(ids).stream().collect(Collectors.toMap(BaseUserEntity::getId, Function.identity()));
    }

    private static BaseUserEntity deployer(Map<String, BaseUserEntity> deployers, String deployerId) {
        if (deployerId == null) {
            return null;
        }
        // a deleted user still shows by id rather than vanishing from the history
        return deployers.getOrDefault(deployerId, BaseUserEntity.builder().id(deployerId).build());
    }

    /** @param pageNumber 1-based; the first page when not positive */
    public record Input(String environmentId, String guardianId, int pageNumber, int pageSize) {}

    /** @param deployer {@code null} when the revision does not record who deployed it */
    public record Deployment(GuardianRevision revision, BaseUserEntity deployer) {}

    /** @param perPage the page size actually served */
    public record Output(Page<Deployment> deployments, int perPage) {}
}
