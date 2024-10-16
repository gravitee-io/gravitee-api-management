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

import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.apim.core.specgen.query_service.ApiSpecGenQueryService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Optional;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSpecGenQueryServiceInMemory implements ApiSpecGenQueryService, InMemoryAlternative<ApiSpecGen> {

    private List<ApiSpecGen> storage;

    @Override
    public Optional<ApiSpecGen> findByIdAndType(ExecutionContext context, String id, ApiType type) {
        return storage
            .stream()
            .filter(api -> context.getEnvironmentId().equals(api.environmentId()))
            .filter(api -> type.equals(api.type()))
            .findFirst();
    }

    @Override
    public void initWith(List<ApiSpecGen> items) {
        reset();
        storage = items;
    }

    @Override
    public void reset() {
        storage = null;
    }

    @Override
    public List<ApiSpecGen> storage() {
        return storage;
    }
}
