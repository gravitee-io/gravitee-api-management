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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repositories.mongodb.internal.model.TeamMemberMongo;
import io.gravitee.repositories.mongodb.internal.model.TeamMongo;
import io.gravitee.repositories.mongodb.internal.model.UserMongo;
import io.gravitee.repositories.mongodb.internal.team.TeamMongoRepository;
import io.gravitee.repositories.mongodb.internal.user.UserMongoRepository;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.TeamMembershipRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Member;
import io.gravitee.repository.model.Team;
import io.gravitee.repository.model.TeamRole;

@Component
public class TeamMembershipRepositoryImpl implements TeamMembershipRepository {

	@Autowired
	private GraviteeMapper mapper;
	
	@Autowired
	private TeamMongoRepository internalTeamRepo;

	@Autowired
	private UserMongoRepository internalUserRepo;
	
	private Logger logger = LoggerFactory.getLogger(TeamMembershipRepositoryImpl.class);
	
	@Override
	public void addMember(String teamName, String username, TeamRole role) throws TechnicalException {
	
		TeamMongo team = internalTeamRepo.findByName(teamName);
		UserMongo member = internalUserRepo.findOne(username);
		
		TeamMemberMongo teamMemberMongo = new TeamMemberMongo();
		teamMemberMongo.setMember(member);
		teamMemberMongo.setRole(String.valueOf(role));
		
		team.getMembers().add(teamMemberMongo);
		internalTeamRepo.save(team);
		
	}

	@Override
	public void updateMember(String teamName, String username, TeamRole role) throws TechnicalException {
		TeamMongo teamMongo = internalTeamRepo.findByName(teamName);
		
		//TODO deal with null / validation / mongo upset implementation
		List<TeamMemberMongo> membersMongo = teamMongo.getMembers();
		
		if(membersMongo != null){
			for (TeamMemberMongo teamMemberMongo : membersMongo) {
				if(username.equals(teamMemberMongo.getMember().getName())){
					teamMemberMongo.setRole(String.valueOf(role));
				}
			}
			internalTeamRepo.save(teamMongo);
		}
	}

	@Override
	public void deleteMember(String teamName, String username) throws TechnicalException {
		
		TeamMongo teamMongo = internalTeamRepo.findByName(teamName);
		
		//TODO deal with null  / validation / mongo upset implementation
		List<TeamMemberMongo> membersMongo = teamMongo.getMembers();
		
		if(membersMongo != null){
			List<TeamMemberMongo> toRemove = new ArrayList<>();
			for (TeamMemberMongo teamMemberMongo : membersMongo) {
				if(username.equals(teamMemberMongo.getMember().getName())){
					toRemove.add(teamMemberMongo);
				}
			}
			teamMongo.getMembers().removeAll(toRemove);
			internalTeamRepo.save(teamMongo);
		}
	}

	@Override
	public Set<Member> listMembers(String teamName) throws TechnicalException {
		TeamMongo teamMongo = internalTeamRepo.findByName(teamName);
		
		//TODO deal with null 
		List<TeamMemberMongo> membersMongo = teamMongo.getMembers();
		
		Set<Member> res = new HashSet<>();
		if(membersMongo != null){
			
			for (TeamMemberMongo teamMemberMongo : membersMongo) {
			
				Member member = new Member();
				member.setUsername(teamMemberMongo.getMember().getName());
				member.setRole(TeamRole.valueOf(teamMemberMongo.getRole()));
				res.add(member);
			}
		}
		return res;
	}

	@Override
	public Set<Team> findByUser(String username) throws TechnicalException {
		
		List<TeamMongo> teams = internalTeamRepo.findByUser(username);
		return mapper.collection2set(teams, TeamMongo.class, Team.class);
	
	}

	@Override
	public Member getMember(String teamName, String memberName) throws TechnicalException {
		try{
			TeamMemberMongo memberMongo = internalTeamRepo.getMember(memberName, teamName);
			return map(memberMongo);
		}catch(Exception e){
			logger.error("Fail to get team member", e);
			throw new TechnicalException(String.format("Failed to get member [%s] from team [%s]", memberName, teamName),e);
		}
	}
	
	
	protected Member map(TeamMemberMongo memberMongo){
		if(memberMongo == null){
			return null;
		}
		
		Member member = new Member();
		member.setUsername(memberMongo.getMember().getName());
		member.setRole(TeamRole.valueOf(memberMongo.getRole()));
		member.setCreatedAt(memberMongo.getCreatedAt());
		member.setUpdatedAt(memberMongo.getUpdatedAt());
		return member;
	}

}
