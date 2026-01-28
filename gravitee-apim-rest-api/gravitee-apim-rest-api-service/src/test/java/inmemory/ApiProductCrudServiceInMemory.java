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

import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import java.util.Optional;
import java.util.OptionalInt;

public class ApiProductCrudServiceInMemory extends AbstractCrudServiceInMemory<ApiProduct> implements ApiProductCrudService {

    public Optional<ApiProduct> findById(String id) {
        return storage
            .stream()
            .filter(apiProduct -> id.equals(apiProduct.getId()))
            .findFirst();
    }

    @Override
    public ApiProduct update(ApiProduct apiProductToUpdate) {
        OptionalInt index = this.findIndex(this.storage, apiProduct -> apiProduct.getId().equals(apiProductToUpdate.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), apiProductToUpdate);
            return apiProductToUpdate;
        }
        throw new IllegalStateException("ApiProduct not found.");
    }

    @Override
    public void delete(String id) {
        storage.removeIf(apiProduct -> id.equals(apiProduct.getId()));
    }

    @Override
    public ApiProduct get(String id) {
        return storage
            .stream()
            .filter(apiProduct -> id.equals(apiProduct.getId()))
            .findFirst()
            .get();
    }
}
