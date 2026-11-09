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
package io.gravitee.apim.infra.query_service.secret;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.query_service.SecretQueryService;
import io.gravitee.apim.infra.adapter.SecretAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.common.Pageable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class SecretQueryServiceImpl implements SecretQueryService {

    private final SecretRepository secretRepository;
    private final SecretAdapter secretAdapter;

    public SecretQueryServiceImpl(@Lazy SecretRepository secretRepository, SecretAdapter secretAdapter) {
        this.secretRepository = secretRepository;
        this.secretAdapter = secretAdapter;
    }

    @Override
    public Page<Secret> search(String organizationId, String environmentId, Pageable pageable) {
        try {
            var page = secretRepository.search(
                organizationId,
                environmentId,
                new PageableBuilder().pageNumber(pageable.getPageNumber() - 1).pageSize(pageable.getPageSize()).build()
            );
            // The repository counts pages from 0 and core from 1, so the page keeps the number it was asked for.
            var content = page.getContent().stream().map(secretAdapter::fromRepository).toList();
            return new Page<>(content, pageable.getPageNumber(), content.size(), page.getTotalElements());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to search the secrets of environment " + environmentId, e);
        }
    }
}
