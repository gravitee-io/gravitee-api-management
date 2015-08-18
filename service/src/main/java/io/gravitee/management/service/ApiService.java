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
package io.gravitee.management.service;

import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.model.UpdateApiEntity;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface ApiService {

    Optional<ApiEntity> findByName(String apiName);

    Set<ApiEntity> findAll();

    Set<ApiEntity> findByTeam(String teamName, boolean publicOnly);

    Set<ApiEntity> findByUser(String username, boolean publicOnly);

    ApiEntity createForUser(NewApiEntity api, String username) throws ApiAlreadyExistsException;

    ApiEntity createForTeam(NewApiEntity api, String teamName) throws ApiAlreadyExistsException;

    ApiEntity update(String apiName, UpdateApiEntity api);

    void delete(String apiName);

    void start(String apiName);

    void stop(String apiName);
}
