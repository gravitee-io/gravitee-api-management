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
package io.gravitee.apim.infra.environment;

import io.gravitee.apim.core.environment.service_provider.EnvironmentTemplateModelProvider;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.MetadataEntity;
import io.gravitee.rest.api.service.MetadataService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnvironmentTemplateModelProviderImpl implements EnvironmentTemplateModelProvider {

    private final MetadataService metadataService;

    @Override
    public Map<String, String> getEnvironmentMetadata(String environmentId) {
        List<MetadataEntity> metadataList = metadataService.findByReferenceTypeAndReferenceId(
            MetadataReferenceType.ENVIRONMENT,
            environmentId
        );
        if (metadataList == null) {
            return Map.of();
        }
        return metadataList.stream().collect(Collectors.toMap(MetadataEntity::getKey, MetadataEntity::getValue));
    }
}
