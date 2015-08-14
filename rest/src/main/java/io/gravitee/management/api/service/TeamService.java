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

import io.gravitee.management.api.model.NewTeamEntity;
import io.gravitee.management.api.model.TeamEntity;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface TeamService {

    Optional<TeamEntity> findByName(String teamName);

    TeamEntity create(NewTeamEntity team);

    TeamEntity update(NewTeamEntity team);

    Set<TeamEntity> findByUser(String username);

    Set<TeamEntity> findAll(boolean publicOnly);
}
