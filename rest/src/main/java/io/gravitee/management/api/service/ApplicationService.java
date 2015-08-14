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
package io.gravitee.management.api.service;

import io.gravitee.management.api.model.ApplicationEntity;
import io.gravitee.management.api.model.NewApplicationEntity;
import io.gravitee.management.api.model.UpdateApplicationEntity;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface ApplicationService {

    Optional<ApplicationEntity> findByName(String applicationName);

    Set<ApplicationEntity> findByTeam(String teamName);

    Set<ApplicationEntity> findByUser(String username);

    ApplicationEntity createForUser(NewApplicationEntity application, String username);

    ApplicationEntity createForTeam(NewApplicationEntity application, String teamName);

    ApplicationEntity update(String applicationName, UpdateApplicationEntity application);

    void delete(String applicationName);
}
