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
package io.gravitee.repository.mongodb.management;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;

public class MongoOrganizationKeyKekIdsTest extends AbstractManagementRepositoryTest {

    @Inject
    private MongoTemplate mongoTemplate;

    @Value("${management.mongodb.prefix:}")
    private String tablePrefix;

    @Override
    protected String getTestCasesPath() {
        return "/data/organization-key-tests/";
    }

    @Test
    public void should_list_a_null_key_encryption_key_id_without_failing() throws Exception {
        var keys = mongoTemplate.getCollection(tablePrefix + "organization_keys");
        keys.insertOne(
            new Document("_id", "key-null").append("organizationId", "org-null").append("wrappedKey", "AAAA").append("kekId", null)
        );
        keys.insertOne(new Document("_id", "key-missing").append("organizationId", "org-missing").append("wrappedKey", "AAAA"));

        assertThat(organizationKeyRepository.findKekIds()).containsExactlyInAnyOrder("kek-a", "kek-b", null);
    }
}
