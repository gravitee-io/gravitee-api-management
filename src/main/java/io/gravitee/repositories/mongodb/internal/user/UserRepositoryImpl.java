package io.gravitee.repositories.mongodb.internal.user;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import io.gravitee.repositories.mongodb.internal.model.Team;
import io.gravitee.repositories.mongodb.internal.model.User;
import io.gravitee.repositories.mongodb.internal.team.TeamRepository;

/**
 * User Mongo repository implementation.
 * 
 * @author ldassonville
 *
 */
public class UserRepositoryImpl implements UserRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private TeamRepository teamRepository;
	
	@Override
	public Set<User> findByTeam(String teamName) {

		Team team = teamRepository.findByName(teamName);
		
		if(team == null || team.getMembers().isEmpty()){
			return new HashSet<>();
		}
		
		return new HashSet<>(team.getMembers());
		
	}

}
