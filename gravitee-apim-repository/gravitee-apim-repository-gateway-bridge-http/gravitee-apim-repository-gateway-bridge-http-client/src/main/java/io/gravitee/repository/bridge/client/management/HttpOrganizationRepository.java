/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.bridge.client.management;

import io.gravitee.repository.bridge.client.utils.BodyCodecs;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpOrganizationRepository extends AbstractRepository implements OrganizationRepository {

    @Override
    public Optional<Organization> findById(String organizationId) throws TechnicalException {
        return blockingGet(get("/organizations/" + organizationId, BodyCodecs.optional(Organization.class)).send()).payload();
    }

    @Override
    public Organization create(Organization item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Organization update(Organization item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Long count() {
        throw new IllegalStateException();
    }

    @Override
    public Set<Organization> findByHrids(Set<String> hrids) {
        try {
            return blockingGet(
                get("/organizations/_byHrids", BodyCodecs.set(Organization.class)).addQueryParam("hrids", String.join(",", hrids)).send()
            )
                .payload();
        } catch (TechnicalException te) {
            // Ensure that an exception is thrown and managed by the caller
            throw new IllegalStateException(te);
        }
    }

    @Override
    public Set<Organization> findAll() throws TechnicalException {
        try {
            return blockingGet(get("/organizations", BodyCodecs.set(Organization.class)).send()).payload();
        } catch (TechnicalException te) {
            // Ensure that an exception is thrown and managed by the caller
            throw new IllegalStateException(te);
        }
    }
}
