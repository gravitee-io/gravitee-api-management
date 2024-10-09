/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management.internal.page;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.mongodb.management.internal.model.PageMongo;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageMongoRepositoryImpl implements PageMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public int findMaxPageReferenceIdAndReferenceTypeOrder(String referenceId, String referenceType) {
        Query query = new Query();
        query.limit(1);
        query.with(Sort.by(Sort.Direction.DESC, "order"));
        query.addCriteria(where("referenceType").is(referenceType).and("referenceId").is(referenceId));

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
            if (criteria.getReferenceId() != null) {
                q.addCriteria(where("referenceId").is(criteria.getReferenceId()));
            }
            if (criteria.getReferenceType() != null) {
                q.addCriteria(where("referenceType").is(criteria.getReferenceType()));
            }
            if (criteria.getPublished() != null) {
                q.addCriteria(where("published").is(criteria.getPublished()));
            }
            if (criteria.getVisibility() != null) {
                if (Visibility.PUBLIC.name().equals(criteria.getVisibility())) {
                    q.addCriteria(
                        new Criteria().orOperator(where("visibility").exists(false), where("visibility").is(Visibility.PUBLIC.name()))
                    );
                } else {
                    q.addCriteria(where("visibility").is(Visibility.PRIVATE.name()));
                }
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
            if (criteria.getUseAutoFetch() != null) {
                q.addCriteria(where("useAutoFetch").is(criteria.getUseAutoFetch().booleanValue()));
            }
        }

        q.with(Sort.by(ASC, "order"));

        return mongoTemplate.find(q, PageMongo.class);
    }

    @Override
    public Page<PageMongo> findAll(Pageable pageable) {
        Query query = new Query();
        long total = mongoTemplate.count(query, PageMongo.class);

        query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        List<PageMongo> pages = mongoTemplate.find(query, PageMongo.class);

        return new Page<>(pages, pageable.pageNumber(), pages.size(), total);
    }

    @Override
    public long countByParentIdAndIsPublished(String parentId) {
        Query query = new Query();
        query.addCriteria(where("parentId").is(parentId));
        query.addCriteria(where("published").is(true));
        return mongoTemplate.count(query, PageMongo.class);
    }

    @Override
    public void unsetHomepage(Collection<String> ids) {
        Query query = new Query();
        query.addCriteria(where("_id").in(ids));
        UpdateDefinition upd = new Update().set("homepage", false);
        mongoTemplate.upsert(query, upd, PageMongo.class);
    }
}
