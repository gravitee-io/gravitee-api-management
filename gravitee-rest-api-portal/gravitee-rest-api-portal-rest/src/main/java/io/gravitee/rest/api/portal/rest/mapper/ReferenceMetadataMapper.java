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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import io.gravitee.rest.api.model.UpdateApplicationMetadataEntity;
import io.gravitee.rest.api.portal.rest.model.ReferenceMetadata;
import io.gravitee.rest.api.portal.rest.model.ReferenceMetadataFormatType;
import io.gravitee.rest.api.portal.rest.model.ReferenceMetadataInput;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ReferenceMetadataMapper {

    public ReferenceMetadata convert(ApplicationMetadataEntity applicationMetadataEntity) {
        final ReferenceMetadata applicationReferenceMetadata = new ReferenceMetadata();

        applicationReferenceMetadata.setApplication(applicationMetadataEntity.getApplicationId());
        applicationReferenceMetadata.setDefaultValue(applicationMetadataEntity.getDefaultValue());
        if (applicationMetadataEntity.getFormat() != null) {
            applicationReferenceMetadata.setFormat(ReferenceMetadataFormatType.valueOf(applicationMetadataEntity.getFormat().name()));
        } else {
            applicationReferenceMetadata.setFormat(ReferenceMetadataFormatType.STRING);
        }
        applicationReferenceMetadata.setKey(applicationMetadataEntity.getKey());
        applicationReferenceMetadata.setName(applicationMetadataEntity.getName());
        applicationReferenceMetadata.setValue(applicationMetadataEntity.getValue());

        return applicationReferenceMetadata;
    }

    public NewApplicationMetadataEntity convert(ReferenceMetadataInput applicationReferenceMetadataInput, String applicationId) {
        final NewApplicationMetadataEntity newApplicationMetadataEntity = new NewApplicationMetadataEntity();

        newApplicationMetadataEntity.setApplicationId(applicationId);
        newApplicationMetadataEntity.setDefaultValue(applicationReferenceMetadataInput.getDefaultValue());
        if (applicationReferenceMetadataInput.getFormat() != null) {
            newApplicationMetadataEntity.setFormat(MetadataFormat.valueOf(applicationReferenceMetadataInput.getFormat().name()));
        } else {
            newApplicationMetadataEntity.setFormat(MetadataFormat.STRING);
        }
        newApplicationMetadataEntity.setName(applicationReferenceMetadataInput.getName());
        newApplicationMetadataEntity.setValue(applicationReferenceMetadataInput.getValue());

        return newApplicationMetadataEntity;
    }

    public UpdateApplicationMetadataEntity convert(ReferenceMetadataInput applicationReferenceMetadataInput, String applicationId, String metadataId) {
        final UpdateApplicationMetadataEntity updateApplicationMetadataEntity = new UpdateApplicationMetadataEntity();

        updateApplicationMetadataEntity.setApplicationId(applicationId);
        updateApplicationMetadataEntity.setKey(metadataId);
        updateApplicationMetadataEntity.setDefaultValue(applicationReferenceMetadataInput.getDefaultValue());
        if (applicationReferenceMetadataInput.getFormat() != null) {
            updateApplicationMetadataEntity.setFormat(MetadataFormat.valueOf(applicationReferenceMetadataInput.getFormat().name()));
        } else {
            updateApplicationMetadataEntity.setFormat(MetadataFormat.STRING);
        }
        updateApplicationMetadataEntity.setName(applicationReferenceMetadataInput.getName());
        updateApplicationMetadataEntity.setValue(applicationReferenceMetadataInput.getValue());

        return updateApplicationMetadataEntity;
    }
}
