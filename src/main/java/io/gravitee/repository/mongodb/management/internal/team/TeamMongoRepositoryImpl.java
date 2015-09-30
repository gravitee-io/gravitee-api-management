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
package io.gravitee.repository.mongodb.management.internal.team;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.gravitee.repository.mongodb.management.internal.model.TeamMemberMongo;
import io.gravitee.repository.mongodb.management.internal.model.TeamMongo;

public class TeamMongoRepositoryImpl implements TeamMongoRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public List<TeamMongo> findByUser(String username) {
		
		Query query = new Query();
		
		Criteria criteria = Criteria.where("members.member.$id").is(username);
		query.addCriteria(criteria);

		List<TeamMongo> teams = mongoTemplate.find(query, TeamMongo.class);

		return teams;
	}

	@Override
	public TeamMemberMongo getMember(String username, String teamname) {
		Query query = new Query();
		
		Criteria criteriaMember = Criteria.where("members.member.$id").is(username);
		Criteria criteriaTeam = Criteria.where("name").is(teamname);
		
		query.addCriteria(criteriaMember);	
		query.addCriteria(criteriaTeam);	
		
		TeamMongo team = mongoTemplate.findOne(query, TeamMongo.class);
		
		if(team == null){
			return null;
		}
		
		Optional<TeamMemberMongo> optional = team.getMembers().stream().filter(new Predicate<TeamMemberMongo>() {

			@Override
			public boolean test(TeamMemberMongo t) {
				return username.equalsIgnoreCase(t.getMember().getName());
			}
		}).findFirst();
		
		if(optional.isPresent()){
			return optional.get();
		}
		return null;
	}

}
