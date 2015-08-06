package io.gravitee.repositories.mongodb.internal.user;

import java.util.Set;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.Team;
import io.gravitee.repositories.mongodb.internal.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String>, UserRepositoryCustom {

	@Query("{ 'username' : ?0}")
	User findByUsername(String username);
	


}


