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

import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiStateService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@Slf4j
@AllArgsConstructor
public class ApiStateDomainServiceLegacyWrapper implements ApiStateDomainService {

    public static final ApiAdapter apiAdapter = ApiAdapter.INSTANCE;

    private final ApiStateService apiStateService;
    private final ApiService apiService;

    @Override
    public boolean isSynchronized(Api api, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        return switch (api.getDefinitionVersion()) {
            case V4 -> apiStateService.isSynchronized(executionContext, apiAdapter.toApiEntity(api));
            case V1, V2 -> apiService.isSynchronized(executionContext, api.getId());
            case FEDERATED_AGENT, FEDERATED -> true;
        };
    }

    @Override
    public Api deploy(Api api, String deploymentLabel, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        var deployed = apiStateService.deploy(
            executionContext,
            apiAdapter.toRepository(api),
            auditInfo.actor().userId(),
            new ApiDeploymentEntity(deploymentLabel)
        );
        return apiAdapter.fromApiEntity(deployed);
    }

    @Override
    public Api start(Api api, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var userId = auditInfo.actor().userId();
        var started = apiStateService.start(executionContext, api.getId(), userId);
        return ApiAdapter.INSTANCE.fromApiEntity(started);
    }

    @Override
    public Api stop(Api api, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var userId = auditInfo.actor().userId();
        var stopped = apiStateService.stop(executionContext, api.getId(), userId);
        return ApiAdapter.INSTANCE.fromApiEntity(stopped);
    }
}
