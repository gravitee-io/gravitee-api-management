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
package io.gravitee.apim.core.portal_notification.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves the current names of the APIs and applications notifications are about, with one lookup per type. An API
 * or an application deleted since has no entry.
 */
@DomainService
public class PortalNotificationNamesDomainService {

    private final ApiCrudService apiCrudService;
    private final ApplicationCrudService applicationCrudService;

    public PortalNotificationNamesDomainService(ApiCrudService apiCrudService, ApplicationCrudService applicationCrudService) {
        this.apiCrudService = apiCrudService;
        this.applicationCrudService = applicationCrudService;
    }

    public Map<String, String> apiNames(Collection<String> apiIds, String environmentId) {
        if (apiIds.isEmpty()) {
            return Map.of();
        }
        return apiCrudService
            .findByIds(apiIds.stream().distinct().toList(), environmentId)
            .stream()
            .collect(Collectors.toMap(Api::getId, Api::getName, (first, second) -> first));
    }

    public Map<String, String> applicationNames(Collection<String> applicationIds, String environmentId) {
        if (applicationIds.isEmpty()) {
            return Map.of();
        }
        List<BaseApplicationEntity> applications = applicationCrudService.findByIds(
            applicationIds.stream().distinct().toList(),
            environmentId
        );
        return applications
            .stream()
            .collect(Collectors.toMap(BaseApplicationEntity::getId, BaseApplicationEntity::getName, (first, second) -> first));
    }
}
