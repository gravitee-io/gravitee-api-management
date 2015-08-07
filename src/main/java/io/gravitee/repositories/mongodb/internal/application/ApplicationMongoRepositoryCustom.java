package io.gravitee.repositories.mongodb.internal.application;

import java.util.List;

import io.gravitee.repositories.mongodb.internal.model.ApplicationMongo;

public interface ApplicationMongoRepositoryCustom {

	/**
	 * Find Applications by team name
	 * @param teamId
	 * @return
	 */
    public List<ApplicationMongo> findByTeam(String name);
    
	/**
	 * Find Applications by team name
	 * @param teamId
	 * @return
	 */
    public List<ApplicationMongo> findByUser(String name);
    
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
