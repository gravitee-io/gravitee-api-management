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

import io.gravitee.apim.core.specgen.crud_service.ApiSpecGenCrudService;
import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.apim.core.specgen.query_service.ApiSpecGenQueryService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSpecGenCrudServiceInMemory implements ApiSpecGenCrudService, InMemoryAlternative<Entry<String, Integer>> {

    private Map<String, Integer> storage;

    @Override
    public void enableAnalyticsLogging(ApiSpecGen apiSpecGen, String userId) {
        final String key = apiSpecGen.id() + "-" + userId;
        storage.compute(key, (k, v) -> v == null ? 1 : v + 1);
    }

    @Override
    public void initWith(List<Entry<String, Integer>> items) {
        reset();
        storage = items.stream().collect(toMap(Entry::getKey, Entry::getValue));
    }

    @Override
    public void reset() {
        storage = null;
    }

    @Override
    public List<Entry<String, Integer>> storage() {
        return storage.entrySet().stream().toList();
    }
}
