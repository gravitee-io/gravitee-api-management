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
package io.gravitee.apim.infra.query_service.credential;

import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialQuery;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.credential.query_service.CredentialQueryService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.CredentialAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CredentialRepository;
import io.gravitee.repository.management.api.search.CredentialCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class CredentialQueryServiceImpl implements CredentialQueryService {

    private final CredentialRepository credentialRepository;

    public CredentialQueryServiceImpl(@Lazy CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Override
    public Page<Credential> search(String organizationId, String environmentId, CredentialQuery query, Pageable pageable) {
        var criteria = CredentialCriteria.builder()
            .query(query.name())
            .strategies(
                query.strategies() == null ? null : query.strategies().stream().map(CredentialStrategy::name).collect(Collectors.toSet())
            )
            .serviceId(query.serviceId())
            .build();
        try {
            var page = credentialRepository.search(
                organizationId,
                environmentId,
                criteria,
                new PageableBuilder().pageNumber(pageable.getPageNumber() - 1).pageSize(pageable.getPageSize()).build()
            );
            // The repository counts pages from 0 and core from 1, so the page keeps the number it was asked for.
            var content = page.getContent().stream().map(CredentialAdapter.INSTANCE::toCoreModel).toList();
            return new Page<>(content, pageable.getPageNumber(), content.size(), page.getTotalElements());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to search the credentials of environment " + environmentId, e);
        }
    }
}
