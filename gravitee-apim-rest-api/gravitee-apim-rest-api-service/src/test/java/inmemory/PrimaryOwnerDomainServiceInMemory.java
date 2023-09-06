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

import io.gravitee.apim.core.api.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.exception.ApiPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrimaryOwnerDomainServiceInMemory
    implements ApiPrimaryOwnerDomainService, InMemoryAlternative<Map.Entry<String, PrimaryOwnerEntity>> {

    Map<String, PrimaryOwnerEntity> storage = new HashMap<>();

    @Override
    public PrimaryOwnerEntity getApiPrimaryOwner(ExecutionContext executionContext, String apiId) throws ApiPrimaryOwnerNotFoundException {
        PrimaryOwnerEntity primaryOwnerEntity = storage.get(apiId);
        if (primaryOwnerEntity == null) {
            throw new ApiPrimaryOwnerNotFoundException(apiId);
        }
        return primaryOwnerEntity;
    }

    @Override
    public void initWith(List<Map.Entry<String, PrimaryOwnerEntity>> items) {
        items.forEach(entry -> storage.put(entry.getKey(), entry.getValue()));
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Map.Entry<String, PrimaryOwnerEntity>> storage() {
        return storage.entrySet().stream().toList();
    }
}
