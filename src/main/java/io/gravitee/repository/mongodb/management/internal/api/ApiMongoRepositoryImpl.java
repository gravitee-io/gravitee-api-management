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
package io.gravitee.repository.mongodb.management.internal.api;

import io.gravitee.repository.management.model.MembershipType;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Collection;

public class ApiMongoRepositoryImpl implements ApiMongoRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public Collection<ApiMongo> findByMember(String username, MembershipType membershipType, Visibility visibility) {
		Query query = new Query();

		if (visibility != null) {
			query.addCriteria(Criteria.where("visibility").is(visibility.toString()));
		}

		if (username != null) {
			if (membershipType == null) {
				query.addCriteria(Criteria.where("members").elemMatch(Criteria.where("user.$id").is(username)));
			} else {
				query.addCriteria(Criteria.where("members").elemMatch(
						Criteria.where("user.$id").is(username)
								.and("type").is(membershipType)));
			}
		}

		return mongoTemplate.find(query, ApiMongo.class);
	}

	@Override
	public int countByUser(String username, String membershipType) {
		Criteria criteriaMember;

		if (membershipType == null) {
			criteriaMember = Criteria.where("members").elemMatch(Criteria.where("user.$id").is(username));
		} else {
			criteriaMember = Criteria.where("members").elemMatch(Criteria.where("user.$id").is(username).and("type").is(membershipType));
		}

		Query query = new Query();
		query.addCriteria(criteriaMember);

		return (int) mongoTemplate.count(query, ApiMongo.class);
	}
}
