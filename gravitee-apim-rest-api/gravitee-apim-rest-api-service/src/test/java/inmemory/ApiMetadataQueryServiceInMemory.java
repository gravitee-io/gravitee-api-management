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

import static java.util.stream.Collectors.toMap;

import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.infra.adapter.MetadataAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ApiMetadataQueryServiceInMemory implements ApiMetadataQueryService, InMemoryAlternative<ApiMetadata> {

    final List<Metadata> storage;

    public ApiMetadataQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public ApiMetadataQueryServiceInMemory(MetadataCrudServiceInMemory metadataCrudServiceInMemory) {
        this.storage = metadataCrudServiceInMemory.storage;
    }

    @Override
    public Map<String, ApiMetadata> findApiMetadata(String apiId) {
        return storage
            .stream()
            .filter(metadata -> metadata.getReferenceId().equals(apiId))
            .collect(toMap(Metadata::getKey, MetadataAdapter.INSTANCE::toApiMetadata));
    }

    @Override
    public void initWith(List<ApiMetadata> items) {
        storage.clear();
        storage.addAll(items.stream().map(MetadataAdapter.INSTANCE::toMetadata).toList());
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<ApiMetadata> storage() {
        return storage.stream().map(MetadataAdapter.INSTANCE::toApiMetadata).toList();
    }
}
