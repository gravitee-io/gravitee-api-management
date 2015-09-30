/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management.internal.application;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo;


public class ApplicationMongoRepositoryImpl implements ApplicationMongoRepositoryCustom{

	@Autowired
	private MongoTemplate mongoTemplate;
	
	
	@Override
	public List<ApplicationMongo> findByTeam(String teamname) {

		Query query = new Query();

		query.addCriteria(Criteria.where("owner.$id").is(teamname));	
		query.addCriteria(Criteria.where("owner.$ref").is("teams"));
		
		List<ApplicationMongo> applications = mongoTemplate.find(query, ApplicationMongo.class);
		
		return applications;

	}

	@Override
	public List<ApplicationMongo> findByUser(String username) {

		Query query = new Query();

		query.addCriteria(Criteria.where("owner.$id").is(username));	
		query.addCriteria(Criteria.where("owner.$ref").is("users"));				
		List<ApplicationMongo> applications = mongoTemplate.find(query, ApplicationMongo.class);
		
		return applications;

	}

	@Override
	public long countByUser(String username) {
		Query query = new Query();

		query.addCriteria(Criteria.where("owner.$id").is(username));	
		query.addCriteria(Criteria.where("owner.$ref").is("users"));	
		return mongoTemplate.count(query, ApplicationMongo.class);
		
	}
	
	@Override
	public long countByTeam(String teamname) {
		Query query = new Query();

		query.addCriteria(Criteria.where("owner.$id").is(teamname));	
		query.addCriteria(Criteria.where("owner.$ref").is("teams"));
				
		return mongoTemplate.count(query, ApplicationMongo.class);
		
	}
}
