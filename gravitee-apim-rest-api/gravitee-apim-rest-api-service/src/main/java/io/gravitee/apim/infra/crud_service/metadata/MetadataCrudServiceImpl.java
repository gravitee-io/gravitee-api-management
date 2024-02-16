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
package io.gravitee.apim.infra.crud_service.metadata;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.infra.adapter.MetadataAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class MetadataCrudServiceImpl implements MetadataCrudService {

    private final MetadataRepository metadataRepository;

    public MetadataCrudServiceImpl(@Lazy MetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    public Metadata create(Metadata metadata) {
        try {
            var result = metadataRepository.create(MetadataAdapter.INSTANCE.toRepository(metadata));
            return MetadataAdapter.INSTANCE.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurs while trying to create the %s metadata of [%sId=%s]",
                    metadata.getKey(),
                    metadata.getReferenceType().name().toLowerCase(),
                    metadata.getReferenceId()
                ),
                e
            );
        }
    }
}
