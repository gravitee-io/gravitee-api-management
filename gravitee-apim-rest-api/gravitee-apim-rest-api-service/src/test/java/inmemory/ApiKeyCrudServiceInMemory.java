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

import io.gravitee.apim.core.api_key.crud_service.ApiKeyCrudService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

public class ApiKeyCrudServiceInMemory implements ApiKeyCrudService, InMemoryAlternative<ApiKeyEntity> {

    final ArrayList<ApiKeyEntity> storage = new ArrayList<>();

    @Override
    public ApiKeyEntity update(ApiKeyEntity entity) {
        OptionalInt index = this.findIndex(this.storage, apiKey -> apiKey.getId().equals(entity.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), entity);
            return entity;
        }

        throw new IllegalStateException("ApiKey not found");
    }

    @Override
    public void initWith(List<ApiKeyEntity> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<ApiKeyEntity> storage() {
        return Collections.unmodifiableList(storage);
    }
}
