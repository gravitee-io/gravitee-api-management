package io.gravitee.repositories.mongodb.internal.api;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.gravitee.repositories.mongodb.internal.model.Api;
import io.gravitee.repositories.mongodb.internal.model.LifecycleState;
import io.gravitee.repositories.mongodb.internal.model.Team;
import io.gravitee.repositories.mongodb.internal.model.User;
import io.gravitee.repositories.mongodb.internal.team.TeamRepository;
import io.gravitee.repositories.mongodb.internal.user.UserRepository;

public class ApiRepositoryImpl implements ApiRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private TeamRepository teamRepository;
	
	/**
	 * {@inheritDoc} 
	 */
	public Api findByName(String apiName){
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(apiName));
		Api api = mongoTemplate.findOne(query, Api.class);
		return api;
	}
	

	/**
	 * Update api state
	 * 
	 * @param apiName
	 * 			Api name
	 * @param state
	 * 			State to apply
	 */
	private void updateState(String apiName, LifecycleState state){

		Api api = findByName(apiName);
		api.setLifecycleState(state);
		api.setUpdatedAt(new Date());
		
		mongoTemplate.save(api);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start(String apiName) {
		updateState(apiName, LifecycleState.STARTED);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop(String apiName) {
		updateState(apiName, LifecycleState.STOPPED);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Set<Api> findByCreator(String username){
		
		User user = userRepository.findByUsername(username);
		if(user == null){
			return null;
		}
		
		Query query = new Query();
		query.addCriteria(Criteria.where("creator.$id").is(new ObjectId(user.getId())));
		List<Api> apis = mongoTemplate.find(query, Api.class);
		
		return new HashSet<Api>(apis);
	}

	@Override
	public Set<Api> findByTeam(String teamName) {
		Team team = teamRepository.findByName(teamName);
		if(team == null){
			return null;
		}
		
		Query query = new Query();
		query.addCriteria(Criteria.where("owner.$id").is(new ObjectId(team.getId())));
		List<Api> apis = mongoTemplate.find(query, Api.class);
		
		return new HashSet<Api>(apis);
	}


}
