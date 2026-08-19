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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.mongodb.MongoCommandException;
import com.mongodb.ServerAddress;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class IndexUpgraderTest {

    private static final String COLLECTION = "groups";
    private static final String INDEX_NAME = "name_1";

    @Mock
    private ReactiveMongoOperations template;

    @Mock
    private ReactiveIndexOperations indexOperations;

    @Mock
    private Environment environment;

    private IndexUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new IndexUpgrader() {
            @Override
            protected Index buildIndex() {
                return Index.builder()
                    .collection(COLLECTION)
                    .name(INDEX_NAME)
                    .key("name", ascending())
                    .build();
            }
        };
        lenient()
            .when(environment.getProperty("management.mongodb.prefix", ""))
            .thenReturn("");
        upgrader.setEnvironment(environment);
        upgrader.setMongoTemplate(template);
        lenient().when(template.indexOps(COLLECTION)).thenReturn(indexOperations);
    }

    @Test
    void should_succeed_when_index_is_created() {
        when(indexOperations.ensureIndex(any())).thenReturn(Mono.just(INDEX_NAME));

        assertThat(upgrader.upgrade()).isTrue();
    }

    @Test
    void should_skip_when_an_index_with_the_same_key_pattern_already_exists() {
        // Amazon DocumentDB rejects a second index on the same key pattern, even with a different name or collation.
        when(indexOperations.ensureIndex(any())).thenReturn(
            Mono.error(indexOptionsConflict())
        );

        assertThat(upgrader.upgrade()).isTrue();
    }

    @Test
    void should_skip_when_the_conflict_is_wrapped_by_spring_data() {
        // Spring Data translates MongoCommandException into a DataAccessException keeping the original as cause.
        var wrapped = new DataIntegrityViolationException(
            "Command failed with error 85",
            indexOptionsConflict()
        );
        when(indexOperations.ensureIndex(any())).thenReturn(Mono.error(wrapped));

        assertThat(upgrader.upgrade()).isTrue();
    }

    @Test
    void should_fail_on_any_other_mongo_error() {
        when(indexOperations.ensureIndex(any())).thenReturn(
            Mono.error(mongoError(86, "IndexKeySpecsConflict"))
        );

        assertThat(upgrader.upgrade()).isFalse();
    }

    @Test
    void should_fail_on_unexpected_error() {
        when(indexOperations.ensureIndex(any())).thenReturn(
            Mono.error(new IllegalStateException("boom"))
        );

        assertThat(upgrader.upgrade()).isFalse();
    }

    @Nested
    class IsIndexOptionsConflict {

        @Test
        void should_detect_error_code_85() {
            assertThat(
                IndexUpgrader.isIndexOptionsConflict(indexOptionsConflict())
            ).isTrue();
        }

        @Test
        void should_detect_error_code_85_in_cause_chain() {
            var wrapped = new RuntimeException(
                "outer",
                new DataIntegrityViolationException("inner", indexOptionsConflict())
            );
            assertThat(IndexUpgrader.isIndexOptionsConflict(wrapped)).isTrue();
        }

        @Test
        void should_ignore_other_error_codes() {
            assertThat(
                IndexUpgrader.isIndexOptionsConflict(
                    mongoError(86, "IndexKeySpecsConflict")
                )
            ).isFalse();
        }

        @Test
        void should_ignore_non_mongo_errors() {
            assertThat(
                IndexUpgrader.isIndexOptionsConflict(new IllegalStateException("boom"))
            ).isFalse();
        }
    }

    private static MongoCommandException indexOptionsConflict() {
        return mongoError(
            IndexUpgrader.INDEX_OPTIONS_CONFLICT_ERROR_CODE,
            "IndexOptionsConflict"
        );
    }

    private static MongoCommandException mongoError(int code, String codeName) {
        var response = new BsonDocument("ok", new BsonInt32(0))
            .append("code", new BsonInt32(code))
            .append("codeName", new BsonString(codeName))
            .append(
                "errmsg",
                new BsonString("Index already exists with different options")
            );
        return new MongoCommandException(response, new ServerAddress("localhost", 27017));
    }
}
