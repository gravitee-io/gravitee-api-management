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
import io.gravitee.repository.management.model.AmConnection;
import java.util.Optional;

/**
 * @author GraviteeSource Team
 */
public interface AmConnectionRepository {
    Optional<AmConnection> findByOrganizationId(String organizationId) throws TechnicalException;

    AmConnection create(AmConnection amConnection) throws TechnicalException;

    /**
     * Updates an existing connection. Throws if no connection exists for the organization (it does not
     * upsert) so the JDBC and Mongo adapters behave the same way.
     */
    AmConnection update(AmConnection amConnection) throws TechnicalException;

    void delete(String organizationId) throws TechnicalException;
}
