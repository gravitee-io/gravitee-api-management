package io.gravitee.repositories.mongodb.internal.api;

import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.Api;

@Repository
public interface ApiRepository extends MongoRepository<Api, String>, ApiRepositoryCustom {

	@Query("{'name' : ?0}")  
	public Api findByName(String name);

	@Query("{ }")  
	public List<Api> findAll(String name);
	
	@Query("{'team' : ?0}")
    public Set<Api> findByTeam(String teamName);

}


