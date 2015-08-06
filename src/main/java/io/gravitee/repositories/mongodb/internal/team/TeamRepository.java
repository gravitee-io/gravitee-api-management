package io.gravitee.repositories.mongodb.internal.team;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.Team;

@Repository
public interface TeamRepository extends MongoRepository<Team, String>, TeamRepositoryCustom {

	@Query("{ 'name' : ?0}")
	Team findByName(String teamName);

}


