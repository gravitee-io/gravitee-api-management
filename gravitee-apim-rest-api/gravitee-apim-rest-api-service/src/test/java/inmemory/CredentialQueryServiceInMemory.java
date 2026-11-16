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

import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialQuery;
import io.gravitee.apim.core.credential.query_service.CredentialQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.Comparator;
import java.util.Locale;

/** Searches the credentials of a {@link CredentialCrudServiceInMemory}, so a test writes and reads one store. */
public class CredentialQueryServiceInMemory implements CredentialQueryService {

    private final CredentialCrudServiceInMemory credentials;

    public CredentialQueryServiceInMemory(CredentialCrudServiceInMemory credentials) {
        this.credentials = credentials;
    }

    @Override
    public Page<Credential> search(String organizationId, String environmentId, CredentialQuery query, Pageable pageable) {
        var matching = credentials
            .storage()
            .stream()
            .filter(c -> c.organizationId().equals(organizationId) && c.environmentId().equals(environmentId))
            .filter(c -> query.name() == null || c.name().toLowerCase(Locale.ROOT).contains(query.name().toLowerCase(Locale.ROOT)))
            .filter(c -> query.strategies() == null || query.strategies().isEmpty() || query.strategies().contains(c.strategy()))
            .filter(c -> query.serviceId() == null || query.serviceId().equals(c.serviceId()))
            .sorted(Comparator.comparing(Credential::name).thenComparing(Credential::id))
            .toList();
        var content = matching
            .stream()
            .skip((long) (pageable.getPageNumber() - 1) * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .toList();
        return new Page<>(content, pageable.getPageNumber(), content.size(), matching.size());
    }
}
