package io.gravitee.repositories.mongodb.internal.api;

import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.ApiMongo;

@Repository
public interface ApiMongoRepository extends MongoRepository<ApiMongo, String>, ApiMongoRepositoryCustom {


		
	/**
	 * Find Apis by creator username
	 * @param username
	 * @return
	 */
	@Query("{'creator.$id' : ?0}")  
	public List<ApiMongo> findByCreator(String username);



}


