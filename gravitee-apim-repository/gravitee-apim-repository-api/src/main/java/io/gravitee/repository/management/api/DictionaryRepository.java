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
import io.gravitee.repository.management.model.Dictionary;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface DictionaryRepository extends CrudRepository<Dictionary, String> {
    /**
     * List all dictionaries for requested environments
     * @param environments
     * @return all dictionaries for requested environments
     * @throws TechnicalException
     */
    Set<Dictionary> findAllByEnvironments(Set<String> environments) throws TechnicalException;

    /**
     * Delete dictionaries by environment ID
     * @param environmentId
     * @return List of IDs for deleted dictionaries
     * @throws TechnicalException
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
