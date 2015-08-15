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
package io.gravitee.repository.api;

import java.util.Set;

import javax.management.relation.Role;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Member;
import io.gravitee.repository.model.Team;
import io.gravitee.repository.model.TeamRole;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface TeamMembershipRepository {

	/**
	 * Add {@link Member} to a {@link Team}.
	 * 
	 * @param teamName Team name of the member
	 * @param username Member user name 
	 * @param role {@link Role} given to the member in this team
	 */
    void addMember(String teamName, String username, TeamRole role) throws TechnicalException;

	/**
	 * Update the role of a given {@link Team} {@link Member}.
	 * 
	 * @param teamName Team name of the member
	 * @param username Member user name 
	 * @param role {@link Role} given to the member in this team
	 */
    void updateMember(String teamName, String username, TeamRole role) throws TechnicalException;

    /**
     * Remove a team user {@link Member}
     * 
     * @param teamName Team name where the member will be removed
     * @param username User name removed as member
     */
    void deleteMember(String teamName, String username) throws TechnicalException;

    /**
     * List all team {@link Member}s 
     * 
     * @param teamName Team name off members
     * @return Team members
     */
    Set<Member> listMembers(String teamName) throws TechnicalException;

    /**
     * List {@link Team} where the user is a member.
     *
     * @param username The name used to identify a user.
     * @return List of {@link Team}
     */
    Set<Team> findByUser(String username) throws TechnicalException;

    Member getMember(String teamName, String memberName) throws TechnicalException;

}
