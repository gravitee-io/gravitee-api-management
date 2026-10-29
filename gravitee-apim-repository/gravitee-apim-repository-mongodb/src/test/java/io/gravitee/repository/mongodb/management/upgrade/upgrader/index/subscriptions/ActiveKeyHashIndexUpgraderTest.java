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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.subscriptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ActiveKeyHashIndexUpgraderTest {

    private static final String COLLECTION = "subscriptions";
    private static final String INDEX_NAME = "akh1_unique";

    @Mock
    private ReactiveMongoOperations template;

    @Mock
    private ReactiveIndexOperations indexOperations;

    @Mock
    private Environment environment;

    private ActiveKeyHashIndexUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new ActiveKeyHashIndexUpgrader();
        when(environment.getProperty("management.mongodb.prefix", "")).thenReturn("");
        upgrader.setEnvironment(environment);
        upgrader.setMongoTemplate(template);
        when(template.indexOps(COLLECTION)).thenReturn(indexOperations);
    }

    @Test
    void should_create_a_unique_index_on_the_hash_of_the_active_subscriptions_only() {
        when(indexOperations.ensureIndex(any())).thenReturn(Mono.just(INDEX_NAME));

        assertThat(upgrader.upgrade()).isTrue();

        var definition = ArgumentCaptor.forClass(IndexDefinition.class);
        verify(indexOperations).ensureIndex(definition.capture());
        assertThat(definition.getValue().getIndexKeys()).isEqualTo(
            new Document("activeKeyHash", 1)
        );
        assertThat(definition.getValue().getIndexOptions())
            .containsEntry("name", INDEX_NAME)
            .containsEntry("unique", true)
            .containsEntry(
                "partialFilterExpression",
                new Document("activeKeyHash", new Document("$exists", true))
            );
    }
}
