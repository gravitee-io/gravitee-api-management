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
package io.gravitee.repository.mongodb.management.internal.membership;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.repository.mongodb.management.internal.model.MembershipMongo;
import java.util.stream.Stream;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class MembershipMongoRepositoryCustomImpl implements MembershipMongoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public MembershipMongoRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Stream<String> findRefIdsByMemberIdAndMemberTypeAndReferenceType(String memberId, String memberType, String referenceType) {
        Query query = new Query();
        query.fields().include("referenceId");
        query.addCriteria(where("memberId").is(memberId));
        query.addCriteria(where("memberType").is(memberType));
        query.addCriteria(where("referenceType").is(referenceType));
        return mongoTemplate.findDistinct(query, "referenceId", MembershipMongo.class, String.class).parallelStream();
    }
}
