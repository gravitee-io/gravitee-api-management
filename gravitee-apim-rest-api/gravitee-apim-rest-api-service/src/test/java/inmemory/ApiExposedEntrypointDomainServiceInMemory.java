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

import io.gravitee.apim.core.api.domain_service.ApiExposedEntrypointDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ExposedEntrypoint;
import java.util.ArrayList;
import java.util.List;

public class ApiExposedEntrypointDomainServiceInMemory
    implements ApiExposedEntrypointDomainService, InMemoryAlternative<ExposedEntrypoint> {

    final List<ExposedEntrypoint> storage = new ArrayList<>();

    @Override
    public void initWith(List<ExposedEntrypoint> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<ExposedEntrypoint> storage() {
        return storage;
    }

    @Override
    public List<ExposedEntrypoint> get(String organizationId, String environmentId, Api api) {
        return storage;
    }
}
