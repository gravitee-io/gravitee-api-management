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

import io.gravitee.apim.core.api.domain_service.DeployApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiStateService;
import org.springframework.stereotype.Service;

@Service
public class DeployApiDomainServiceLegacyWrapper implements DeployApiDomainService {

    private final ApiStateService apiStateService;

    public DeployApiDomainServiceLegacyWrapper(ApiStateService apiStateService) {
        this.apiStateService = apiStateService;
    }

    @Override
    public Api deploy(Api apiToDeploy, String deploymentLabel, AuditInfo auditInfo) {
        var deployed = apiStateService.deploy(
            new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId()),
            apiToDeploy.getId(),
            auditInfo.actor().userId(),
            new ApiDeploymentEntity(deploymentLabel)
        );
        return ApiAdapter.INSTANCE.fromApiEntity(deployed);
    }
}
