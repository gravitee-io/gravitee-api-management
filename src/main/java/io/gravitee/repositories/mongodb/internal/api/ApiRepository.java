package io.gravitee.repositories.mongodb.internal.api;

import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.Api;

@Repository
public interface ApiRepository extends MongoRepository<Api, String>, ApiRepositoryCustom {

	@Query("{'name' : ?0}")  
	public Api findByName(String name);

	@Query("{'team.$id' : ?0}")
    public Set<Api> findByTeamId(ObjectId teamId);
		
	@Query("{'creator.$id' : ?0}")  
	public List<Api> findByCreatorId(ObjectId creatorId);
	
	@Query(delete=true, value="{'name' : ?0}")
	public void deleteByName(String apiName);

	
}


