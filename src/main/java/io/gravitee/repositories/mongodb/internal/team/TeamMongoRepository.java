package io.gravitee.repositories.mongodb.internal.team;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.TeamMongo;

@Repository
public interface TeamMongoRepository extends MongoRepository<TeamMongo, String>, TeamMongoRepositoryCustom {

	@Query("{ 'name' : ?0}")
	TeamMongo findByName(String teamName);

}


