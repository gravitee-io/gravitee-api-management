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

import io.gravitee.repository.management.model.MembershipType;
import io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Collection;


public class ApplicationMongoRepositoryImpl implements ApplicationMongoRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public Collection<ApplicationMongo> findByUser(String username, MembershipType membershipType) {
		Query query = new Query();

		if (membershipType == null) {
			query.addCriteria(Criteria.where("members").elemMatch(Criteria.where("user.$id").is(username)));
		} else {
			query.addCriteria(Criteria.where("members").elemMatch(
					Criteria.where("user.$id").is(username)
							.and("type").is(membershipType)));
		}

		return mongoTemplate.find(query, ApplicationMongo.class);
	}

	@Override
	public int countByUser(String username, MembershipType membershipType) {
		Query query = new Query();

		if (membershipType == null) {
			query.addCriteria(Criteria.where("members").elemMatch(Criteria.where("user.$id").is(username)));
		} else {
			query.addCriteria(Criteria.where("members").elemMatch(
					Criteria.where("user.$id").is(username)
							.and("type").is(membershipType)));
		}

		return (int) mongoTemplate.count(query, ApplicationMongo.class);
	}
}
