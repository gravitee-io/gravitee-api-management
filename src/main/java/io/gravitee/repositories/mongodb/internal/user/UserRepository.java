package io.gravitee.repositories.mongodb.internal.user;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String>, UserRepositoryCustom {

	@Query("{ 'username' : ?0}")
	User findByUsername(String username);

	
    //Set<User> findByTeam(String teamName);
}


