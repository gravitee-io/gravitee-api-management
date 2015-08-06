package io.gravitee.repositories.mongodb.internal.api;

import java.util.Set;

import io.gravitee.repositories.mongodb.internal.model.Api;

public interface ApiRepositoryCustom {

	Set<Api> findByTeam(String teamName);
	
	Set<Api> findByCreator(String username);

    void start(String apiName);
	
    void stop(String apiName);
    

}
