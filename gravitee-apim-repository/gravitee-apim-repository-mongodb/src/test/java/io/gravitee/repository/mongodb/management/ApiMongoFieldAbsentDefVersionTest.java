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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Closes the "field-absent vs BSON-null" coverage gap left by ApiRepositoryTest:
 * fixture docs go through Spring Data's POJO writer, which materialises every
 * absent JSON key as an explicit BSON null. The legacy-V2 production scenario —
 * documents where the {@code definitionVersion} key is omitted entirely — is
 * never represented in that corpus.
 *
 * This test inserts a document directly via {@link MongoTemplate}, bypassing the
 * POJO writer, and asserts the {@code $in [..., null]} query matches it. A
 * {@code $exists: false} sanity check confirms the field is truly absent.
 */
public class ApiMongoFieldAbsentDefVersionTest extends AbstractManagementRepositoryTest {

    private static final String FIELD_ABSENT_API_ID = "field-absent-v2-api";
    private static final String FIELD_ABSENT_ENV_ID = "ENV-FIELD-ABSENT";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${management.mongodb.prefix:}")
    private String collectionPrefix;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Override
    protected String getModelPackage() {
        return "io.gravitee.repository.management.model.";
    }

    @Override
    protected void createModel(Object object) {
        // not used: fixture loading is disabled
    }

    private String apisCollectionName() {
        return collectionPrefix + "apis";
    }

    @BeforeEach
    public void insertFieldAbsentDoc() {
        Document doc = new Document()
            .append("_id", FIELD_ABSENT_API_ID)
            .append("environmentId", FIELD_ABSENT_ENV_ID)
            .append("name", "field-absent v2 api")
            .append("definition", "{}")
            .append("visibility", "PRIVATE");
        // No definitionVersion key at all — this is the legacy-V2 storage shape.
        mongoTemplate.getCollection(apisCollectionName()).insertOne(doc);
    }

    @AfterEach
    public void deleteFieldAbsentDoc() {
        mongoTemplate.getCollection(apisCollectionName()).deleteOne(new Document("_id", FIELD_ABSENT_API_ID));
    }

    @Test
    public void v2QueryMatchesDocsWithDefinitionVersionFieldAbsent() {
        long fieldAbsentCount = mongoTemplate
            .getCollection(apisCollectionName())
            .countDocuments(new Document("_id", FIELD_ABSENT_API_ID).append("definitionVersion", new Document("$exists", false)));
        assertThat(fieldAbsentCount).as("seeded doc must have definitionVersion truly absent, not BSON-null").isEqualTo(1L);

        List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.V2)).environmentId(FIELD_ABSENT_ENV_ID).build(),
            ApiFieldFilter.allFields()
        );
        assertThat(apis).extracting(Api::getId).containsExactly(FIELD_ABSENT_API_ID);
    }
}
