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
package io.gravitee.repository.mongodb.management.internal.page;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.gravitee.repository.mongodb.management.internal.model.PageMongo;
import org.springframework.data.mongodb.core.query.Update;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageMongoRepositoryImpl implements PageMongoRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	public int findMaxPageOrderByApi(String apiId) {
		Query query = new Query();
		query.limit(1);
		query.with(new Sort(Sort.Direction.DESC, "order"));
		query.addCriteria(Criteria.where("api").is(apiId));

		PageMongo page = mongoTemplate.findOne(query, PageMongo.class);
		return (page != null) ? page.getOrder() : 0;
	}

	public int findMaxPortalPageOrder() {
		Query query = new Query();
		query.limit(1);
		query.with(new Sort(Sort.Direction.DESC, "order"));
		query.addCriteria(Criteria.where("api").exists(false));

		PageMongo page = mongoTemplate.findOne(query, PageMongo.class);
		return (page != null) ? page.getOrder() : 0;
	}

	public void updateAllPageWithParent(String pageFolderId, String apiId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("parentId").is(pageFolderId));
        query.addCriteria(Criteria.where("api").is(apiId));
        Update update = new Update();
        update.set("parentId", "");

        mongoTemplate.updateMulti(query,update, PageMongo.class);
    }
}