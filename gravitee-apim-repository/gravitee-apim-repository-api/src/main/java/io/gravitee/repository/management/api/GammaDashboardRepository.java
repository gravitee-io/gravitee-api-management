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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.GammaDashboard;
import java.util.List;
import java.util.Optional;

/**
 * <h2>Required fields</h2>
 *
 * {@code id}, {@code environmentId}, {@code title}, {@code createdBy}, {@code createdAt} and {@code updatedAt} are
 * required and must be supplied by the caller. The two backends do <strong>not</strong> agree on what happens
 * otherwise: JDBC declares them {@code NOT NULL} and fails the write, while Mongo accepts the document as-is. Callers
 * must therefore validate before writing rather than rely on the store to reject — the same input can succeed on one
 * deployment and fail on another.
 *
 * @author GraviteeSource Team
 */
public interface GammaDashboardRepository extends CrudRepository<GammaDashboard, String> {
    /**
     * Ordered by creation date, then id. The order is part of the contract: without it Mongo would return natural
     * order, which shifts when an update grows a document, so editing one dashboard could reshuffle the whole list.
     */
    List<GammaDashboard> findByEnvironmentId(String environmentId) throws TechnicalException;

    /**
     * Environment-scoped lookup. Prefer this over {@link #findById(Object)} for anything reachable from a request:
     * it makes cross-environment isolation a property of the query rather than of a caller-side check somebody has to
     * remember to write.
     */
    Optional<GammaDashboard> findByIdAndEnvironmentId(String id, String environmentId) throws TechnicalException;

    void deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
