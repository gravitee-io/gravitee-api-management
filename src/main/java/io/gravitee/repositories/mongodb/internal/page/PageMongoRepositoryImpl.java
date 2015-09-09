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
package io.gravitee.repositories.mongodb.internal.page;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.gravitee.repositories.mongodb.internal.model.PageMongo;

/**
 * @author Titouan COMPIEGNE
 */
public class PageMongoRepositoryImpl implements PageMongoRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	public int findMaxPageOrderByApiName(String apiName) {
		Query query = new Query();
		query.limit(1);
		query.with(new Sort(Sort.Direction.DESC, "order"));
		query.addCriteria(Criteria.where("apiName").is(apiName));	

		PageMongo page = mongoTemplate.findOne(query, PageMongo.class);
		return (page != null) ? page.getOrder() : 0;
	}
	
	
	
}