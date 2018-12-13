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

import io.gravitee.repository.management.api.search.PageCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.gravitee.repository.mongodb.management.internal.model.PageMongo;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageMongoRepositoryImpl implements PageMongoRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public int findMaxPageOrderByApi(String apiId) {
		Query query = new Query();
		query.limit(1);
		query.with(new Sort(Sort.Direction.DESC, "order"));
		query.addCriteria(where("api").is(apiId));

		PageMongo page = mongoTemplate.findOne(query, PageMongo.class);
		return (page != null) ? page.getOrder() : 0;
	}

	@Override
	public int findMaxPortalPageOrder() {
		Query query = new Query();
		query.limit(1);
		query.with(new Sort(Sort.Direction.DESC, "order"));
		query.addCriteria(where("api").exists(false));

		PageMongo page = mongoTemplate.findOne(query, PageMongo.class);
		return (page != null) ? page.getOrder() : 0;
	}

	@Override
	public List<PageMongo> search(PageCriteria criteria) {
		final Query q = new Query();

		if (criteria != null) {
			if (criteria.getHomepage() != null) {
				q.addCriteria(where("homepage").is(criteria.getHomepage()));
			}
			if (criteria.getApi() == null) {
				q.addCriteria(where("api").exists(false));
			} else {
				q.addCriteria(where("api").is(criteria.getApi()));
			}
			if (criteria.getPublished() != null) {
				q.addCriteria(where("published").is(criteria.getPublished()));
			}
			if (criteria.getName() != null) {
				q.addCriteria(where("name").is(criteria.getName()));
			}
			if (criteria.getParent() != null) {
				q.addCriteria(where("parentId").is(criteria.getParent()));
			}
			if (criteria.getRootParent() != null && criteria.getRootParent().equals(Boolean.TRUE)) {
				q.addCriteria(where("parentId").exists(false));
			}
			if (criteria.getType() != null) {
				q.addCriteria(where("type").is(criteria.getType()));
			}
		}

		q.with(new Sort(ASC, "order"));

		return mongoTemplate.find(q, PageMongo.class);
	}
}