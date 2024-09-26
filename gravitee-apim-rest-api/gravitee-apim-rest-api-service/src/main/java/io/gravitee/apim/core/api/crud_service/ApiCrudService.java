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
package io.gravitee.apim.core.api.crud_service;

import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApiCrudService {
    default Api get(String id) {
        return findById(id).orElseThrow(() -> new ApiNotFoundException(id));
    }

    default Optional<Api> findById(String id) {
        return stream(find(List.of(id))).findFirst();
    }

    Collection<Api> find(Collection<String> ids);
    boolean existsById(String id);
    Api create(Api api);
    Api update(Api api);
    void delete(String id);
}
