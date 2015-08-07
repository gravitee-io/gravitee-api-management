package io.gravitee.repositories.mongodb.internal.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import io.gravitee.repositories.mongodb.internal.model.TeamMemberMongo;
import io.gravitee.repositories.mongodb.internal.model.TeamMongo;
import io.gravitee.repositories.mongodb.internal.team.TeamMongoRepository;

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
