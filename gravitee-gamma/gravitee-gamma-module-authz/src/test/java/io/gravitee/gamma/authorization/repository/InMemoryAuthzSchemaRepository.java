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
package io.gravitee.gamma.authorization.repository;

import io.gravitee.gamma.authorization.api.AuthzSchemaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAuthzSchemaRepository implements AuthzSchemaRepository {

    private final ConcurrentMap<String, String> byEnv = new ConcurrentHashMap<>();

    @Override
    public Optional<String> find(String environmentId) {
        return Optional.ofNullable(byEnv.get(environmentId));
    }

    @Override
    public void save(String environmentId, String schemaText, Instant updatedAt) {
        byEnv.put(environmentId, schemaText);
    }

    @Override
    public boolean delete(String environmentId) {
        return byEnv.remove(environmentId) != null;
    }
}
