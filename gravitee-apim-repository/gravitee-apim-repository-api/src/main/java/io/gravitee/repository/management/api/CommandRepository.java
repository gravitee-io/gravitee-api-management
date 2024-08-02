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
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import java.util.List;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface CommandRepository extends CrudRepository<Command, String> {
    List<Command> search(CommandCriteria criteria);

    /**
     * Delete commands by environment
     * @param environmentId
     * @return List of IDs for deleted commands
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;

    /**
     * Delete commands by organization
     * @param organizationId
     * @return List of IDs for deleted commands
     */
    List<String> deleteByOrganizationId(String organizationId) throws TechnicalException;
}
