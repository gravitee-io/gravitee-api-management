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

import io.gravitee.apim.core.api.domain_service.ApiAuthorizationDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiQueryCriteria;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiAuthorizationDomainServiceInMemory implements ApiAuthorizationDomainService, InMemoryAlternative<Api> {

    final List<Api> storage = new ArrayList<>();

    @Override
    public void initWith(List<Api> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Api> storage() {
        return storage;
    }

    @Override
    public Set<String> findIdsByUser(
        ExecutionContext executionContext,
        String userId,
        ApiQueryCriteria apiQueryCriteria,
        Sortable sortable,
        boolean manageOnly
    ) {
        return storage.stream().map(Api::getId).collect(Collectors.toSet());
    }
}
