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
package inmemory;

import io.gravitee.apim.core.application_metadata.crud_service.ApplicationMetadataCrudService;
import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import io.gravitee.rest.api.model.UpdateApplicationMetadataEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationMetadataCrudServiceInMemory
    implements ApplicationMetadataCrudService, InMemoryAlternative<ApplicationMetadataEntity> {

    private final List<ApplicationMetadataEntity> storage = new ArrayList<>();

    @Override
    public ApplicationMetadataEntity create(NewApplicationMetadataEntity metadata) {
        ApplicationMetadataEntity entity = new ApplicationMetadataEntity();
        entity.setApplicationId(metadata.getApplicationId());
        entity.setName(metadata.getName());
        entity.setFormat(metadata.getFormat());
        entity.setValue(metadata.getValue());
        entity.setDefaultValue(metadata.getDefaultValue());

        storage.add(entity);
        return entity;
    }

    @Override
    public ApplicationMetadataEntity update(UpdateApplicationMetadataEntity metadata) {
        OptionalInt index = this.findIndex(this.storage, entity -> entity.getApplicationId().equals(metadata.getApplicationId()));
        if (index.isPresent()) {
            ApplicationMetadataEntity entity = new ApplicationMetadataEntity();
            entity.setApplicationId(metadata.getApplicationId());
            entity.setName(metadata.getName());
            entity.setFormat(metadata.getFormat());
            entity.setValue(metadata.getValue());
            entity.setDefaultValue(metadata.getDefaultValue());
            entity.setKey(metadata.getKey());

            storage.set(index.getAsInt(), entity);
            return entity;
        }

        throw new IllegalStateException("Application metadata not found");
    }

    @Override
    public void delete(ApplicationMetadataEntity metadata) {
        OptionalInt index = this.findIndex(this.storage, entity -> entity.getApplicationId().equals(metadata.getApplicationId()));
        if (index.isPresent()) {
            this.storage.remove(metadata);
            return;
        }

        throw new IllegalStateException("Application metadata not found");
    }

    @Override
    public void initWith(List<ApplicationMetadataEntity> items) {
        this.storage.addAll(items);
    }

    @Override
    public void reset() {
        this.storage.clear();
    }

    @Override
    public List<ApplicationMetadataEntity> storage() {
        return this.storage;
    }
}
