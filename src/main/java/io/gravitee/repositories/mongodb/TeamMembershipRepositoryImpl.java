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
package io.gravitee.repositories.mongodb;

import java.util.Set;

import org.springframework.stereotype.Component;

import io.gravitee.repository.api.TeamMembershipRepository;
import io.gravitee.repository.model.Member;
import io.gravitee.repository.model.TeamRole;

@Component
public class TeamMembershipRepositoryImpl implements TeamMembershipRepository {

	@Override
	public void addMember(String teamName, String username, TeamRole role) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateMember(String teamName, String username, TeamRole role) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteMember(String teamName, String username) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<Member> listMembers(String teamName) {
		// TODO Auto-generated method stub
		return null;
	}

}
