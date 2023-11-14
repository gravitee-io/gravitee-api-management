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

import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.model.UpdateApiMetadataEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@Slf4j
public class ApiMetadataDomainServiceImpl implements ApiMetadataDomainService {

    private final ApiMetadataService metadataService;

    public ApiMetadataDomainServiceImpl(ApiMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @Override
    public void saveApiMetadata(String apiId, List<ApiMetadata> metadata, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        metadata
            .stream()
            .map(apiMetadata -> {
                UpdateApiMetadataEntity updateApiMetadataEntity = new UpdateApiMetadataEntity();
                updateApiMetadataEntity.setApiId(apiId);
                updateApiMetadataEntity.setDefaultValue(apiMetadata.getDefaultValue());
                updateApiMetadataEntity.setFormat(apiMetadata.getFormat());
                updateApiMetadataEntity.setKey(apiMetadata.getKey());
                updateApiMetadataEntity.setName(apiMetadata.getName());
                updateApiMetadataEntity.setValue(apiMetadata.getValue());
                return updateApiMetadataEntity;
            })
            .forEach(entity -> {
                try {
                    metadataService.update(executionContext, entity);
                } catch (Exception e) {
                    log.warn("Unable to save metadata {} for API {}' due to : {}", entity.getName(), apiId, e.getMessage());
                }
            });
        log.debug("Metadata successfully created for api {}", apiId);
    }
}
