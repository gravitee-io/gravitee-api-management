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
package io.gravitee.repository.mongodb.management.internal.theme;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.*;
import io.gravitee.repository.mongodb.management.internal.model.ThemeMongo;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ThemeMongoRepositoryImpl implements ThemeMongoRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ThemeMongo> search(ThemeCriteria criteria, Pageable pageable) {
        var query = buildQuery(criteria, pageable);

        long total = mongoTemplate.count(query, ThemeMongo.class);

        List<ThemeMongo> apis = mongoTemplate.find(query, ThemeMongo.class);

        return new Page<>(apis, pageable != null ? pageable.pageNumber() : 0, pageable != null ? pageable.pageSize() : 0, total);
    }

    private Query buildQuery(ThemeCriteria themeCriteria, Pageable pageable) {
        Query query = new Query();

        if (Objects.nonNull(themeCriteria.getEnabled())) {
            query.addCriteria(where("enabled").is(themeCriteria.getEnabled()));
        }

        if (Objects.nonNull(themeCriteria.getType())) {
            query.addCriteria(where("type").is(themeCriteria.getType().name()));
        }

        Sort sort = Sort.by(ASC, "name");

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize(), sort));
        } else {
            query.with(sort);
        }
        return query;
    }
}
