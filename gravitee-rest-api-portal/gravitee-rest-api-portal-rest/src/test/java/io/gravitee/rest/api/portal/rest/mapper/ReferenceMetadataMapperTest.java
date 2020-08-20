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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReferenceMetadataMapperTest {

    private static final String METADATA_APPLICATION_ID = "my-metadata-application-id";
    private static final String METADATA_DEFAULT_VALUE = "my-metadata-default-value";
    private static final String METADATA_VALUE = "my-metadata-value";
    private static final String METADATA_FORMAT = "BOOLEAN";
    private static final String METADATA_KEY = "my-metadata-key";
    private static final String METADATA_NAME = "my-metadata-name";

    private ReferenceMetadataMapper referenceMetadataMapper = new ReferenceMetadataMapper();

    @Test
    public void testConvertEntityToPojo() {
        //init
        ApplicationMetadataEntity applicationMetadataEntity = new ApplicationMetadataEntity();
        applicationMetadataEntity.setApplicationId(METADATA_APPLICATION_ID);
        applicationMetadataEntity.setDefaultValue(METADATA_DEFAULT_VALUE);
        applicationMetadataEntity.setValue(METADATA_VALUE);
        applicationMetadataEntity.setFormat(MetadataFormat.valueOf(METADATA_FORMAT));
        applicationMetadataEntity.setKey(METADATA_KEY);
        applicationMetadataEntity.setName(METADATA_NAME);

        //Test
        final ReferenceMetadata referenceMetadata = referenceMetadataMapper.convert(applicationMetadataEntity);
        assertNotNull(referenceMetadata);
        assertEquals(METADATA_APPLICATION_ID, referenceMetadata.getApplication());
        assertEquals(METADATA_DEFAULT_VALUE, referenceMetadata.getDefaultValue());
        assertEquals(METADATA_KEY, referenceMetadata.getKey());
        assertEquals(METADATA_NAME, referenceMetadata.getName());
        assertEquals(METADATA_VALUE, referenceMetadata.getValue());
        assertEquals(ReferenceMetadataFormatType.valueOf(METADATA_FORMAT), referenceMetadata.getFormat());
    }

    @Test
    public void testConvertInputToNewMetadataEntity() {
        //init
        ReferenceMetadataInput metadataInput = new ReferenceMetadataInput();
        metadataInput.setDefaultValue(METADATA_DEFAULT_VALUE);
        metadataInput.setValue(METADATA_VALUE);
        metadataInput.setFormat(ReferenceMetadataFormatType.valueOf(METADATA_FORMAT));
        metadataInput.setName(METADATA_NAME);

        //Test
        final NewApplicationMetadataEntity newApplicationMetadataEntity = referenceMetadataMapper.convert(metadataInput, METADATA_APPLICATION_ID);
        assertNotNull(newApplicationMetadataEntity);
        assertEquals(METADATA_APPLICATION_ID, newApplicationMetadataEntity.getApplicationId());
        assertEquals(METADATA_DEFAULT_VALUE, newApplicationMetadataEntity.getDefaultValue());
        assertEquals(METADATA_NAME, newApplicationMetadataEntity.getName());
        assertEquals(METADATA_VALUE, newApplicationMetadataEntity.getValue());
        assertEquals(MetadataFormat.valueOf(METADATA_FORMAT), newApplicationMetadataEntity.getFormat());
    }

    @Test
    public void testConvertInputToUpdateMetadataEntity() {
        //init
        ReferenceMetadataInput metadataInput = new ReferenceMetadataInput();
        metadataInput.setDefaultValue(METADATA_DEFAULT_VALUE);
        metadataInput.setValue(METADATA_VALUE);
        metadataInput.setFormat(ReferenceMetadataFormatType.valueOf(METADATA_FORMAT));
        metadataInput.setName(METADATA_NAME);

        //Test
        final UpdateApplicationMetadataEntity updateApplicationMetadataEntity = referenceMetadataMapper.convert(metadataInput, METADATA_APPLICATION_ID, METADATA_KEY);
        assertNotNull(updateApplicationMetadataEntity);
        assertEquals(METADATA_APPLICATION_ID, updateApplicationMetadataEntity.getApplicationId());
        assertEquals(METADATA_DEFAULT_VALUE, updateApplicationMetadataEntity.getDefaultValue());
        assertEquals(METADATA_NAME, updateApplicationMetadataEntity.getName());
        assertEquals(METADATA_KEY, updateApplicationMetadataEntity.getKey());
        assertEquals(METADATA_VALUE, updateApplicationMetadataEntity.getValue());
        assertEquals(MetadataFormat.valueOf(METADATA_FORMAT), updateApplicationMetadataEntity.getFormat());
    }
}
