/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.MetadataReferenceType.APPLICATION;
import static java.util.stream.Collectors.toList;

import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import io.gravitee.rest.api.model.ReferenceMetadataEntity;
import io.gravitee.rest.api.model.UpdateApplicationMetadataEntity;
import io.gravitee.rest.api.service.ApplicationMetadataService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApplicationMetadataServiceImpl extends AbstractReferenceMetadataService implements ApplicationMetadataService {

    @Override
    public List<ApplicationMetadataEntity> findAllByApplication(final String applicationId) {
        final List<ReferenceMetadataEntity> allMetadata = findAllByReference(APPLICATION, applicationId, false);
        return allMetadata.stream().map(m -> convert(m, applicationId)).collect(toList());
    }

    @Override
    public ApplicationMetadataEntity findByIdAndApplication(final String metadataId, final String applicationId) {
        return convert(findByIdAndReference(metadataId, APPLICATION, applicationId, false), applicationId);
    }

    @Override
    public void delete(final String metadataId, final String applicationId) {
        delete(metadataId, APPLICATION, applicationId);
    }

    @Override
    public ApplicationMetadataEntity create(final NewApplicationMetadataEntity metadataEntity) {
        return convert(create(metadataEntity, APPLICATION, metadataEntity.getApplicationId(), false), metadataEntity.getApplicationId());
    }

    @Override
    public ApplicationMetadataEntity update(final UpdateApplicationMetadataEntity metadataEntity) {
        return convert(update(metadataEntity, APPLICATION, metadataEntity.getApplicationId(), false), metadataEntity.getApplicationId());
    }

    private ApplicationMetadataEntity convert(ReferenceMetadataEntity m, String applicationId) {
        final ApplicationMetadataEntity applicationMetadataEntity = new ApplicationMetadataEntity();
        applicationMetadataEntity.setKey(m.getKey());
        applicationMetadataEntity.setName(m.getName());
        applicationMetadataEntity.setFormat(m.getFormat());
        applicationMetadataEntity.setValue(m.getValue());
        applicationMetadataEntity.setDefaultValue(m.getDefaultValue());
        applicationMetadataEntity.setApplicationId(applicationId);
        return applicationMetadataEntity;
    }
}
