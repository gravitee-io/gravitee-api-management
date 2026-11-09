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
package io.gravitee.apim.core.secret.crud_service;

import io.gravitee.apim.core.secret.exception.SecretNotFoundException;
import io.gravitee.apim.core.secret.model.Secret;
import java.util.Optional;

public interface SecretCrudService {
    Secret create(Secret secret, String encryptedValue);

    Optional<Secret> findById(String organizationId, String environmentId, String id);

    Optional<Secret> findByKey(String organizationId, String environmentId, String key);

    Secret update(Secret secret, String encryptedValue);

    void delete(Secret secret);

    default Secret get(String organizationId, String environmentId, String id) {
        return findById(organizationId, environmentId, id).orElseThrow(() -> new SecretNotFoundException(id));
    }
}
