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
package io.gravitee.apim.infra.crud_service.application_metadata;

import io.gravitee.apim.core.application_metadata.crud_service.ApplicationMetadataCrudService;
import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import io.gravitee.rest.api.model.UpdateApplicationMetadataEntity;
import io.gravitee.rest.api.service.ApplicationMetadataService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Service
public class ApplicationMetadataCrudServiceLegacyWrapper implements ApplicationMetadataCrudService {

    private final ApplicationMetadataService applicationMetadataService;

    @Override
    public ApplicationMetadataEntity create(NewApplicationMetadataEntity metadata) {
        var executionContext = GraviteeContext.getExecutionContext();
        return applicationMetadataService.create(executionContext, metadata);
    }

    @Override
    public ApplicationMetadataEntity update(UpdateApplicationMetadataEntity metadata) {
        var executionContext = GraviteeContext.getExecutionContext();
        return applicationMetadataService.update(executionContext, metadata);
    }

    @Override
    public void delete(ApplicationMetadataEntity metadata) {
        var executionContext = GraviteeContext.getExecutionContext();
        applicationMetadataService.delete(executionContext, metadata.getKey(), metadata.getApplicationId());
    }
}
