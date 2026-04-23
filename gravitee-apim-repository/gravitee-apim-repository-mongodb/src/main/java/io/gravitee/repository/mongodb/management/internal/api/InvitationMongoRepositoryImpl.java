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
package io.gravitee.repository.mongodb.management.internal.api;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.InvitationRepository.InvitationCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.mongodb.management.internal.model.InvitationMongo;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author GraviteeSource Team
 */
public class InvitationMongoRepositoryImpl implements InvitationMongoRepositoryCustom {

    private static final String DEFAULT_SORT_FIELD = "email";
    private static final Set<String> SORTABLE_FIELDS = Set.of(
        "id",
        "referenceType",
        "referenceId",
        "email",
        "apiRole",
        "applicationRole",
        "createdAt",
        "updatedAt"
    );

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<InvitationMongo> search(InvitationCriteria criteria, Sortable sortable, Pageable pageable) {
        var query = new Query();

        if (criteria != null) {
            if (StringUtils.isNotBlank(criteria.referenceId())) {
                query.addCriteria(where("referenceId").is(criteria.referenceId()));
            }

            if (criteria.referenceType() != null) {
                query.addCriteria(where("referenceType").is(criteria.referenceType().name()));
            }

            if (StringUtils.isNotBlank(criteria.email())) {
                query.addCriteria(where("email").regex(Pattern.quote(criteria.email()), "i"));
            }
        }

        query.with(buildSort(sortable));

        long total = mongoTemplate.count(query, InvitationMongo.class);

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        query.collation(Collation.of(Locale.ENGLISH).strength(Collation.ComparisonLevel.secondary()));

        List<InvitationMongo> invitations = mongoTemplate.find(query, InvitationMongo.class);

        return new Page<>(invitations, pageable != null ? pageable.pageNumber() : 0, invitations.size(), total);
    }

    private Sort buildSort(Sortable sortable) {
        var field = sortable != null && StringUtils.isNotBlank(sortable.field())
            ? FieldUtils.toCamelCase(sortable.field())
            : DEFAULT_SORT_FIELD;

        if (!SORTABLE_FIELDS.contains(field)) {
            field = DEFAULT_SORT_FIELD;
        }

        var direction = sortable != null && Order.DESC.equals(sortable.order()) ? Sort.Direction.DESC : ASC;
        return Sort.by(direction, field);
    }
}
