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
import io.gravitee.repository.bridge.client.utils.ExcludeMethodFromGeneratedCoverage;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpEnvironmentRepository extends AbstractRepository implements EnvironmentRepository {

    @Override
    public Optional<Environment> findById(String environmentId) throws TechnicalException {
        return blockingGet(get("/environments/" + environmentId, BodyCodecs.optional(Environment.class)).send()).payload();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Environment create(Environment item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Environment update(Environment item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public void delete(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Environment> findAll() throws TechnicalException {
        try {
            return blockingGet(get("/environments", BodyCodecs.set(Environment.class)).send()).payload();
        } catch (TechnicalException te) {
            // Ensure that an exception is thrown and managed by the caller
            throw new IllegalStateException(te);
        }
    }

    @Override
    public Set<Environment> findByOrganization(String organizationId) throws TechnicalException {
        try {
            return blockingGet(
                get("/environments/_byOrganizationId", BodyCodecs.set(Environment.class))
                    .addQueryParam("organizationId", organizationId)
                    .send()
            )
                .payload();
        } catch (TechnicalException te) {
            // Ensure that an exception is thrown and managed by the caller
            throw new IllegalStateException(te);
        }
    }

    @Override
    public Set<Environment> findByOrganizationsAndHrids(Set<String> organizationsHrids, Set<String> hrids) throws TechnicalException {
        try {
            return blockingGet(
                get("/environments/_byOrganizationsAndHrids", BodyCodecs.set(Environment.class))
                    .addQueryParam("organizationsIds", String.join(",", organizationsHrids))
                    .addQueryParam("hrids", String.join(",", hrids))
                    .send()
            )
                .payload();
        } catch (TechnicalException te) {
            // Ensure that an exception is thrown and managed by the caller
            throw new IllegalStateException(te);
        }
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Optional<Environment> findByCockpit(String cockpitId) throws TechnicalException {
        throw new IllegalStateException();
    }
}
