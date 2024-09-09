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
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import java.util.*;
import java.util.function.Function;

public class ApiMetadataQueryServiceInMemory implements ApiMetadataQueryService, InMemoryAlternative<Metadata> {

    final List<Metadata> storage;

    public ApiMetadataQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public ApiMetadataQueryServiceInMemory(MetadataCrudServiceInMemory metadataCrudServiceInMemory) {
        this.storage = metadataCrudServiceInMemory.storage;
    }

    @Override
    public Map<String, ApiMetadata> findApiMetadata(String environmentId, String apiId) {
        Map<String, ApiMetadata> apiMetadata = storage
            .stream()
            .filter(metadata ->
                Objects.equals(metadata.getReferenceId(), environmentId) &&
                Metadata.ReferenceType.ENVIRONMENT.equals(metadata.getReferenceType())
            )
            .map(m ->
                ApiMetadata
                    .builder()
                    .key(m.getKey())
                    .name(m.getName())
                    .defaultValue(m.getValue())
                    .format(Metadata.MetadataFormat.valueOf(m.getFormat().name()))
                    .build()
            )
            .collect(toMap(ApiMetadata::getKey, Function.identity()));

        storage
            .stream()
            .filter(metadata ->
                Objects.equals(metadata.getReferenceId(), apiId) && Metadata.ReferenceType.API.equals(metadata.getReferenceType())
            )
            .forEach(m ->
                apiMetadata.compute(
                    m.getKey(),
                    (key, existing) ->
                        Optional
                            .ofNullable(existing)
                            .map(value -> value.toBuilder().apiId(apiId).name(m.getName()).value(m.getValue()).build())
                            .orElse(this.toApiMetadata(m))
                )
            );

        return apiMetadata;
    }

    @Override
    public void initWith(List<Metadata> items) {
        storage.clear();
        storage.addAll(items);
    }

    public void initWithApiMetadata(List<ApiMetadata> items) {
        storage.clear();
        storage.addAll(items.stream().map(MetadataAdapter.INSTANCE::toMetadata).toList());
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Metadata> storage() {
        return storage;
    }

    private ApiMetadata toApiMetadata(Metadata metadata) {
        return ApiMetadata
            .builder()
            .apiId(metadata.getReferenceId())
            .key(metadata.getKey())
            .name(metadata.getName())
            .value(metadata.getValue())
            .format(metadata.getFormat())
            .build();
    }
}
