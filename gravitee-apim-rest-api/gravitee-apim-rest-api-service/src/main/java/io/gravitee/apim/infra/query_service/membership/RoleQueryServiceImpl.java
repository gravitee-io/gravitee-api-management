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
package io.gravitee.apim.infra.query_service.membership;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.infra.adapter.RoleAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class RoleQueryServiceImpl implements RoleQueryService {

    private final RoleRepository roleRepository;

    public RoleQueryServiceImpl(@Lazy RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Optional<Role> findApiRole(String name, ReferenceContext referenceContext) {
        try {
            return roleRepository
                .findByScopeAndNameAndReferenceIdAndReferenceType(
                    RoleScope.API,
                    name,
                    referenceContext.getReferenceId(),
                    RoleReferenceType.valueOf(referenceContext.getReferenceType().name())
                )
                .map(RoleAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find api role", e);
        }
    }

    @Override
    public Optional<Role> findApplicationRole(String name, ReferenceContext referenceContext) {
        try {
            return roleRepository
                .findByScopeAndNameAndReferenceIdAndReferenceType(
                    RoleScope.APPLICATION,
                    name,
                    referenceContext.getReferenceId(),
                    RoleReferenceType.valueOf(referenceContext.getReferenceType().name())
                )
                .map(RoleAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find application role", e);
        }
    }

    @Override
    public Optional<Role> findIntegrationRole(String name, ReferenceContext referenceContext) {
        try {
            return roleRepository
                .findByScopeAndNameAndReferenceIdAndReferenceType(
                    RoleScope.INTEGRATION,
                    name,
                    referenceContext.getReferenceId(),
                    RoleReferenceType.valueOf(referenceContext.getReferenceType().name())
                )
                .map(RoleAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find integration role", e);
        }
    }

    @Override
    public Set<Role> findByIds(Set<String> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Set.of();
        }
        try {
            return roleRepository.findAllByIdIn(ids).stream().map(RoleAdapter.INSTANCE::toEntity).collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to find role by list of ids", e);
        }
    }
}
