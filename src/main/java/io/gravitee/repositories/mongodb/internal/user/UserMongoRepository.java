package io.gravitee.repositories.mongodb.internal.user;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.UserMongo;

@Repository
public interface UserMongoRepository extends MongoRepository<UserMongo, String>, UserMongoRepositoryCustom {

	@Query("{ 'username' : ?0}")
	UserMongo findByUsername(String username);
	


}


