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

package io.gravitee.apim.infra.domain_service.api;

import static io.gravitee.repository.management.model.Api.AuditEvent.API_UPDATED;

import io.gravitee.apim.core.api.domain_service.UpdateFederatedApiDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.user.domain_service.UserPrimaryOwnerDomainService;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
public class UpdateFederatedApiDomainServiceImpl implements UpdateFederatedApiDomainService {

    private final ApiRepository apiRepository;

    private final SearchEngineService searchEngineService;

    private final UserPrimaryOwnerDomainService userPrimaryOwnerDomainService;

    private final AuditService auditService;

    public UpdateFederatedApiDomainServiceImpl(
        @Lazy ApiRepository apiRepository,
        SearchEngineService searchEngineService,
        UserPrimaryOwnerDomainService userPrimaryOwnerDomainService,
        AuditService auditService
    ) {
        this.apiRepository = apiRepository;
        this.searchEngineService = searchEngineService;
        this.userPrimaryOwnerDomainService = userPrimaryOwnerDomainService;
        this.auditService = auditService;
    }

    @Override
    public Api update(Api api, AuditInfo auditInfo) {
        try {
            var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

            PrimaryOwnerEntity primaryOwnerEntity = userPrimaryOwnerDomainService.getUserPrimaryOwner(auditInfo.actor().userId());

            var apiToUpdate = this.apiRepository.findById(api.getId()).orElseThrow(() -> new ApiNotFoundException(api.getId()));

            var repoApi = ApiAdapter.INSTANCE.toRepository(api);

            // Copy fields from existing values
            repoApi.setEnvironmentId(apiToUpdate.getEnvironmentId());
            repoApi.setDeployedAt(apiToUpdate.getDeployedAt());
            repoApi.setCreatedAt(apiToUpdate.getCreatedAt());
            repoApi.setLifecycleState(apiToUpdate.getLifecycleState());
            if (api.getCrossId() == null) {
                repoApi.setCrossId(apiToUpdate.getCrossId());
            }
            repoApi.setDefinition(apiToUpdate.getDefinition());

            // Keep existing picture as picture update has dedicated service
            repoApi.setPicture(apiToUpdate.getPicture());
            repoApi.setBackground(apiToUpdate.getBackground());

            if (api.getGroups() == null) {
                repoApi.setGroups(apiToUpdate.getGroups());
            }
            if (api.getLabels() == null && apiToUpdate.getLabels() != null) {
                repoApi.setLabels(new ArrayList<>(new HashSet<>(apiToUpdate.getLabels())));
            }
            if (api.getCategories() == null) {
                repoApi.setCategories(apiToUpdate.getCategories());
            }
            repoApi.setUpdatedAt(new Date());

            var updatedApi = this.apiRepository.update(repoApi);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                updatedApi.getId(),
                Collections.emptyMap(),
                API_UPDATED,
                updatedApi.getUpdatedAt(),
                apiToUpdate,
                updatedApi
            );

            searchEngineService.index(executionContext, ApiAdapter.INSTANCE.toFederatedApiEntity(updatedApi, primaryOwnerEntity), false);

            return ApiAdapter.INSTANCE.toCoreModel(updatedApi);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("", e);
        }
    }
}
