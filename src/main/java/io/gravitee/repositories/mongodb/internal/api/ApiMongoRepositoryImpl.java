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
