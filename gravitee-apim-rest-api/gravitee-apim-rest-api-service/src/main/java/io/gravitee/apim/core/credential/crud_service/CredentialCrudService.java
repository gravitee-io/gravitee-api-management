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
package io.gravitee.gamma.module.platform.core.credential.port.repository;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.module.platform.core.credential.exception.CredentialNotFoundException;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import java.util.Optional;

public interface CredentialRepository {
    Credential create(Credential credential);

    Optional<Credential> findById(String organizationId, String environmentId, String id);

    default Credential get(String organizationId, String environmentId, String id) {
        return findById(organizationId, environmentId, id).orElseThrow(() -> new CredentialNotFoundException(id));
    }

    Credential update(Credential credential);

    void delete(Credential credential);

    /**
     * @param query text the name contains, ignoring case, or {@code null} for every credential
     * @param pageNumber the page, from 0
     * @return the credentials sorted by name
     */
    Page<Credential> search(String organizationId, String environmentId, String query, int pageNumber, int pageSize);
}
