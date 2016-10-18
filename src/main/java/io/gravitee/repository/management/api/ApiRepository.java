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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Visibility;

import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiRepository extends CrudRepository<Api, String>{

    /**
     * List all APIs.
     *
     * @return All APIs.
     */
    Set<Api> findAll() throws TechnicalException;

    /**
     * List APIs for a given visibility.
     *
     * @param visibility i.e. Public or Private
     * @return List APIs.
     */
    Set<Api> findByVisibility(Visibility visibility) throws TechnicalException;

    /**
     * find a list of Apis via their ids.
     * @param ids a list of apis id
     * @return List APIs.
     */
    Set<Api> findByIds(List<String> ids) throws TechnicalException;

    /**
     * find apis by their groups
     * @param groupIds a list of group ids
     * @return applications
     */
    Set<Api> findByGroups(List<String> groupIds) throws TechnicalException;
}