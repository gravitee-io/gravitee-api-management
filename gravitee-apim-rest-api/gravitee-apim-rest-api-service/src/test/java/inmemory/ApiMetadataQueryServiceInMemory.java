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

public class ApiMetadataQueryServiceInMemory implements ApiMetadataQueryService, InMemoryAlternative<Map.Entry<String, List<ApiMetadata>>> {

    Map<String, List<ApiMetadata>> storage = new HashMap<>();

    @Override
    public Map<String, ApiMetadata> findApiMetadata(String apiId) {
        return storage.get(apiId).stream().collect(toMap(ApiMetadata::getKey, Function.identity()));
    }

    @Override
    public void initWith(List<Map.Entry<String, List<ApiMetadata>>> items) {
        storage.clear();
        items.forEach(entry -> storage.put(entry.getKey(), entry.getValue()));
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Map.Entry<String, List<ApiMetadata>>> storage() {
        return storage.entrySet().stream().toList();
    }
}
