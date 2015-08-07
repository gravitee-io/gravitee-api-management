package io.gravitee.repositories.mongodb.internal.application;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.gravitee.repositories.mongodb.internal.model.ApplicationMongo;


public class ApplicationMongoRepositoryImpl implements ApplicationMongoRepositoryCustom{

	@Autowired
	private MongoTemplate mongoTemplate;
	
	
	@Override
	public List<ApplicationMongo> findByTeam(String teamname) {

		Query query = new Query();

		Criteria criteria = 
			Criteria.where("owner.$id").is(teamname)
				.andOperator(
			Criteria.where("owner.$ref").is("teams"));
		
		query.addCriteria(criteria);				
		List<ApplicationMongo> applications = mongoTemplate.find(query, ApplicationMongo.class);
		
		return applications;

	}

	@Override
	public List<ApplicationMongo> findByUser(String username) {

		Query query = new Query();

		Criteria criteria = 
			Criteria.where("owner.$id").is(username)
				.andOperator(
			Criteria.where("owner.$ref").is("users"));
		
		query.addCriteria(criteria);				
		List<ApplicationMongo> applications = mongoTemplate.find(query, ApplicationMongo.class);
		
		return applications;

	}

	@Override
	public long countByUser(String username) {
		Query query = new Query();

		Criteria criteria = 
			Criteria.where("owner.$id").is(username)
				.andOperator(
			Criteria.where("owner.$ref").is("users"));
		
		query.addCriteria(criteria);				
		return mongoTemplate.count(query, ApplicationMongo.class);
		
	}
	
	@Override
	public long countByTeam(String teamname) {
		Query query = new Query();

		Criteria criteria = 
			Criteria.where("owner.$id").is(teamname)
				.andOperator(
			Criteria.where("owner.$ref").is("teams"));
		
		query.addCriteria(criteria);				
		return mongoTemplate.count(query, ApplicationMongo.class);
		
	}
}
