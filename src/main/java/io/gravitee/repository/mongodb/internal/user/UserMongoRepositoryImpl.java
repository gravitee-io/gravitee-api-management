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
package io.gravitee.repository.mongodb.internal.user;

import java.util.List;

import io.gravitee.repository.mongodb.internal.model.TeamMemberMongo;
import io.gravitee.repository.mongodb.internal.model.TeamMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import io.gravitee.repository.mongodb.internal.team.TeamMongoRepository;

/**
 * User Mongo repository implementation.
 * 
 * @author ldassonville
 *
 */
public class UserMongoRepositoryImpl implements UserMongoRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private TeamMongoRepository teamRepository;
	
	@Override
	public List<TeamMemberMongo> findByTeam(String teamName) {
		TeamMongo team = teamRepository.findByName(teamName);
		return team.getMembers();
	}

}
