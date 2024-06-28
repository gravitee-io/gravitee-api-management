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
package io.gravitee.apim.infra.query_service.application_metadata;

import io.gravitee.apim.core.application_metadata.query_service.ApplicationMetadataQueryService;
import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.service.ApplicationMetadataService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApplicationMetadataQueryServiceLegacyWrapper implements ApplicationMetadataQueryService {

    private final ApplicationMetadataService applicationMetadataService;

    public ApplicationMetadataQueryServiceLegacyWrapper(ApplicationMetadataService applicationMetadataService) {
        this.applicationMetadataService = applicationMetadataService;
    }

    @Override
    public List<ApplicationMetadataEntity> findAllByApplication(String applicationId) {
        return applicationMetadataService.findAllByApplication(applicationId);
    }
}
