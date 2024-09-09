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
package io.gravitee.apim.infra.query_service.audit;

import static io.gravitee.rest.api.service.impl.MetadataServiceImpl.getDefaultReferenceId;

import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.query_service.AuditMetadataQueryService;
import io.gravitee.apim.infra.adapter.UserAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.Plan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditMetadataQueryServiceImpl implements AuditMetadataQueryService {

    private final ApiRepository apiRepository;
    private final ApplicationRepository applicationRepository;
    private final GroupRepository groupRepository;
    private final MetadataRepository metadataRepository;
    private final PageRepository pageRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    public AuditMetadataQueryServiceImpl(
        @Lazy ApiRepository apiRepository,
        @Lazy ApplicationRepository applicationRepository,
        @Lazy GroupRepository groupRepository,
        @Lazy MetadataRepository metadataRepository,
        @Lazy PageRepository pageRepository,
        @Lazy PlanRepository planRepository,
        @Lazy UserRepository userRepository
    ) {
        this.apiRepository = apiRepository;
        this.applicationRepository = applicationRepository;
        this.groupRepository = groupRepository;
        this.metadataRepository = metadataRepository;
        this.pageRepository = pageRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String fetchUserNameMetadata(String userId) {
        try {
            return userRepository.findById(userId).map(user -> UserAdapter.INSTANCE.fromUser(user).displayName()).orElse(userId);
        } catch (TechnicalException e) {
            log.error("Error finding user audit metadata {}", userId);
            return userId;
        }
    }

    @Override
    public String fetchApiNameMetadata(String apiId) {
        try {
            return apiRepository.findById(apiId).map(Api::getName).orElse(apiId);
        } catch (TechnicalException e) {
            log.error("Error finding api audit metadata {}", apiId);
            return apiId;
        }
    }

    @Override
    public String fetchPropertyMetadata(AuditEntity audit, String propertyName, String propertyValue) {
        try {
            switch (Audit.AuditProperties.valueOf(propertyName)) {
                case API:
                    return apiRepository.findById(propertyValue).map(Api::getName).orElse(propertyValue);
                case APPLICATION:
                    return applicationRepository.findById(propertyValue).map(Application::getName).orElse(propertyValue);
                case GROUP:
                    return groupRepository.findById(propertyValue).map(Group::getName).orElse(propertyValue);
                case METADATA:
                    var refType = MetadataReferenceType.parse(audit.getReferenceType().name());
                    return metadataRepository
                        .findById(propertyValue, audit.getReferenceId(), refType)
                        .map(Metadata::getName)
                        .orElse(propertyValue);
                case PAGE:
                    return pageRepository.findById(propertyValue).map(Page::getName).orElse(propertyValue);
                case PLAN:
                    return planRepository.findById(propertyValue).map(Plan::getName).orElse(propertyValue);
                case USER:
                    return userRepository
                        .findById(propertyValue)
                        .map(user -> UserAdapter.INSTANCE.fromUser(user).displayName())
                        .orElse(propertyValue);
                default:
                    return propertyValue;
            }
        } catch (TechnicalException e) {
            log.error("Error finding Audit metadata {}:{}", propertyName, propertyValue);
            return propertyValue;
        }
    }
}
