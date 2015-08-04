package io.gravitee.repositories.mongodb.internal.team;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.Team;
import io.gravitee.repositories.mongodb.internal.model.User;

@Repository
public interface TeamRepository extends MongoRepository<Team, String>, TeamRepositoryCustom {

}


