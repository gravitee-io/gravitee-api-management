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

import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.metadata.model.MetadataId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public class MetadataCrudServiceInMemory implements MetadataCrudService, InMemoryAlternative<Metadata> {

    final ArrayList<Metadata> storage = new ArrayList<>();

    @Override
    public Metadata create(Metadata entity) {
        storage.add(entity);
        return entity;
    }

    @Override
    public Optional<Metadata> findById(MetadataId id) {
        return storage
            .stream()
            .filter(m ->
                Objects.equals(m.getKey(), id.getKey()) &&
                Objects.equals(m.getReferenceId(), id.getReferenceId()) &&
                Objects.equals(m.getReferenceType(), id.getReferenceType())
            )
            .findFirst();
    }

    @Override
    public Metadata update(Metadata metadata) {
        OptionalInt index =
            this.findIndex(
                    this.storage,
                    m ->
                        m.getKey().equals(metadata.getKey()) &&
                        m.getReferenceId().equals(metadata.getReferenceId()) &&
                        m.getReferenceType().equals(metadata.getReferenceType())
                );
        if (index.isPresent()) {
            storage.set(index.getAsInt(), metadata);
            return metadata;
        }

        throw new IllegalStateException("Metadata not found");
    }

    @Override
    public void delete(MetadataId metadataId) {
        storage.removeIf(m ->
            m.getReferenceId().equals(metadataId.getReferenceId()) &&
            m.getReferenceType().equals(metadataId.getReferenceType()) &&
            m.getKey().equals(metadataId.getKey())
        );
    }

    @Override
    public void initWith(List<Metadata> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Metadata> storage() {
        return Collections.unmodifiableList(storage);
    }
}
