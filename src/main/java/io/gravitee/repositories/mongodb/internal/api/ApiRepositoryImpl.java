package io.gravitee.repositories.mongodb.internal.api;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.gravitee.repositories.mongodb.internal.model.Api;
import io.gravitee.repositories.mongodb.internal.model.LifecycleState;

public class ApiRepositoryImpl implements ApiRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	
	public Api findByName( String apiName){
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(apiName));
		Api api = mongoTemplate.findOne(query, Api.class);
		
		return api;
	}
	
	private void updateState(String apiName, LifecycleState state){

		Api api = findByName(apiName);
		api.setLifecycleState(state);
		api.setUpdatedAt(new Date());
		
		mongoTemplate.save(api);
	}
	
	@Override
	public void start(String apiName) {
		updateState(apiName, LifecycleState.STARTED);
	}

	@Override
	public void stop(String apiName) {
		updateState(apiName, LifecycleState.STOPPED);
	}

	@Override
	public void delete(String apiName) {
		
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(apiName));
		mongoTemplate.findAndRemove(query, Api.class);
		
	}

}
