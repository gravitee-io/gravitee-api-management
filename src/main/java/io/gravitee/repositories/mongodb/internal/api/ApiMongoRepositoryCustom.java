package io.gravitee.repositories.mongodb.internal.api;

import java.util.List;

import io.gravitee.repositories.mongodb.internal.model.ApiMongo;

public interface ApiMongoRepositoryCustom {

	/**
	 * Find Apis by team name
	 * @param teamId
	 * @return
	 */
    public List<ApiMongo> findByTeam(String name);
    
	/**
	 * Find Apis by team name
	 * @param teamId
	 * @return
	 */
    public List<ApiMongo> findByUser(String name);
    
    /**
     * Count api by username (owner)
     * @param username
     * @return
     */
	public long countByUser(String username);
    
    /**
     * Count api by username (owner)
     * @param username
     * @return
     */	
	public long countByTeam(String teamname);
}
