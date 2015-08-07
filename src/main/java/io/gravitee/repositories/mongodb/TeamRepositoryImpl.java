package io.gravitee.repositories.mongodb;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repositories.mongodb.internal.model.TeamMongo;
import io.gravitee.repositories.mongodb.internal.team.TeamMongoRepository;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.TeamRepository;
import io.gravitee.repository.model.Team;


@Component
public class TeamRepositoryImpl implements TeamRepository{

	@Autowired
	private TeamMongoRepository internalTeamRepo;

	@Autowired
	private GraviteeMapper mapper;

	@Override
	public Set<Team> findAll() {
		
		List<TeamMongo> teams = internalTeamRepo.findAll();
		Set<Team> res = mapper.collection2set(teams, TeamMongo.class, Team.class);
		return res;
	}

	@Override
	public Optional<Team> findByName(String name) {
		TeamMongo team = internalTeamRepo.findOne(name);
		return Optional.ofNullable(mapper.map(team, Team.class));
	}

	@Override
	public Team create(Team team) {
		
		TeamMongo teamMongo = mapper.map(team, TeamMongo.class);
		TeamMongo teamMongoCreated = internalTeamRepo.insert(teamMongo);
		return mapper.map(teamMongoCreated, Team.class);
	}

	@Override
	public Team update(Team team) {
		
		TeamMongo teamMongo = mapper.map(team, TeamMongo.class);
		TeamMongo teamMongoUpdated = internalTeamRepo.save(teamMongo);
		return mapper.map(teamMongoUpdated, Team.class);
	}

	@Override
	public void delete(String name) {
		
		internalTeamRepo.delete(name);
		
	}
	
}
