package io.gravitee.repositories.mongodb.internal.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.gravitee.repositories.mongodb.internal.model.ApiMongo;

public class ApiMongoRepositoryImpl implements ApiMongoRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	
	@Override
	public List<ApiMongo> findByTeam(String teamname) {

		Query query = new Query();

		Criteria criteria = 
			Criteria.where("owner.$id").is(teamname)
				.andOperator(
			Criteria.where("owner.$ref").is("teams"));
		
		query.addCriteria(criteria);				
		List<ApiMongo> apis = mongoTemplate.find(query, ApiMongo.class);
		
		return apis;

	}

	@Override
	public List<ApiMongo> findByUser(String username) {

		Query query = new Query();

		Criteria criteria = 
			Criteria.where("owner.$id").is(username)
				.andOperator(
			Criteria.where("owner.$ref").is("users"));
		
		query.addCriteria(criteria);				
		List<ApiMongo> apis = mongoTemplate.find(query, ApiMongo.class);
		
		return apis;

	}

	@Override
	public long countByUser(String username) {
		Query query = new Query();

		Criteria criteria = 
			Criteria.where("owner.$id").is(username)
				.andOperator(
			Criteria.where("owner.$ref").is("users"));
		
		query.addCriteria(criteria);				
		return mongoTemplate.count(query, ApiMongo.class);
		
	}
	
	@Override
	public long countByTeam(String teamname) {
		Query query = new Query();

		Criteria criteria = 
			Criteria.where("owner.$id").is(teamname)
				.andOperator(
			Criteria.where("owner.$ref").is("teams"));
		
		query.addCriteria(criteria);				
		return mongoTemplate.count(query, ApiMongo.class);
		
	}
	
}
