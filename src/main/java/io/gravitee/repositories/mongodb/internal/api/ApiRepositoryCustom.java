package io.gravitee.repositories.mongodb.internal.api;

public interface ApiRepositoryCustom {

	void delete(String apiName);
	
    void start(String apiName);
	
    void stop(String apiName);
}
