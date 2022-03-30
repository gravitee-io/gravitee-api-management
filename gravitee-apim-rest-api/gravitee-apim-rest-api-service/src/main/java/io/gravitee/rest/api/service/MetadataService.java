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
package io.gravitee.rest.api.service;

import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.MetadataEntity;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.NewMetadataEntity;
import io.gravitee.rest.api.model.UpdateMetadataEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface MetadataService {
    List<MetadataEntity> findAllDefault();

    MetadataEntity create(ExecutionContext executionContext, NewMetadataEntity metadata);

    MetadataEntity update(ExecutionContext executionContext, UpdateMetadataEntity metadata);

    void delete(ExecutionContext executionContext, String metadataId);

    void checkMetadataFormat(ExecutionContext executionContext, MetadataFormat format, String value);

    void checkMetadataFormat(
        ExecutionContext executionContext,
        MetadataFormat format,
        String value,
        MetadataReferenceType referenceType,
        Object entity
    );

    MetadataEntity findDefaultByKey(String key);
}
