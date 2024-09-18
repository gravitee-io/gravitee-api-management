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
package io.gravitee.apim.infra.query_service.integration;

import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.query_service.IntegrationQueryService;
import io.gravitee.apim.infra.adapter.IntegrationAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IntegrationQueryServiceImpl extends AbstractService implements IntegrationQueryService {

    private final IntegrationRepository integrationRepository;
    private final MembershipService membershipService;

    public IntegrationQueryServiceImpl(@Lazy IntegrationRepository integrationRepository, MembershipService membershipService) {
        this.integrationRepository = integrationRepository;
        this.membershipService = membershipService;
    }

    @Override
    public Page<Integration> findByEnvironment(String environmentId, Pageable pageable) {
        try {
            return integrationRepository.findAllByEnvironment(environmentId, convert(pageable)).map(IntegrationAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            log.error("An error occurred while finding Integrations by environment", e);
            throw new TechnicalManagementException("An error occurred while finding Integrations by environment id: " + environmentId, e);
        }
    }

    @Override
    public Page<Integration> findByEnvironmentAndContext(
        String environmentId,
        String userId,
        Collection<String> groups,
        boolean isAdmin,
        Pageable pageable
    ) {
        try {
            Page<io.gravitee.repository.management.model.Integration> integrations;
            if (isAdmin) {
                integrations = integrationRepository.findAllByEnvironment(environmentId, convert(pageable));
            } else {
                var integrationAccessibleDirectlyByUser = membershipService
                    .getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.INTEGRATION)
                    .stream()
                    .map(MembershipEntity::getReferenceId)
                    .collect(Collectors.toSet());
                integrations =
                    integrationRepository.findAllByEnvironmentAndGroups(
                        environmentId,
                        integrationAccessibleDirectlyByUser,
                        groups,
                        convert(pageable)
                    );
            }
            return integrations.map(IntegrationAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            log.error("An error occurred while finding Integrations by environment", e);
            throw new TechnicalManagementException("An error occurred while finding Integrations by environment id: " + environmentId, e);
        }
    }
}
