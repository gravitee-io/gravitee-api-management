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

import io.gravitee.definition.jackson.model.MembershipEntity;
import io.gravitee.definition.jackson.model.TeamRole;

import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface TeamMembershipService {

    void addOrUpdateMember(String teamName, String username, TeamRole teamRole);

    void deleteMember(String teamName, String username);

    Set<MembershipEntity> findMembers(String teamName, TeamRole teamRole);
}
