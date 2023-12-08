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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApiMetadataQueryServiceInMemory implements ApiMetadataQueryService, InMemoryAlternative<Map.Entry<String, List<ApiMetadata>>> {

    MapStorage<String, List<ApiMetadata>> storage = new MapStorage<>();

    @Override
    public Map<String, ApiMetadata> findApiMetadata(String apiId) {
        return storage.data().get(apiId).stream().collect(toMap(ApiMetadata::getKey, Function.identity()));
    }

    @Override
    public ApiMetadataQueryServiceInMemory initWith(Storage<Map.Entry<String, List<ApiMetadata>>> items) {
        storage.clear();
        items.data().forEach(entry -> storage.data().put(entry.getKey(), entry.getValue()));
        return this;
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public Storage<Map.Entry<String, List<ApiMetadata>>> storage() {
        // FIXME ugly
        return Storage.from(storage.data().entrySet().stream().toList());
    }

    @Override
    public void syncStorageWith(InMemoryAlternative<Map.Entry<String, List<ApiMetadata>>> other) {
        // FIXME: to implement
    }
}
