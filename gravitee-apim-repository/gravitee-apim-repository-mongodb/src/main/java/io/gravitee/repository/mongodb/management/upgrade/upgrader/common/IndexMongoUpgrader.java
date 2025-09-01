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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.common;

import com.mongodb.reactivestreams.client.MongoCollection;
import io.gravitee.node.api.upgrader.Upgrader;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import reactor.core.publisher.Mono;

/**
 * @author GraviteeSource Team
 */
public abstract class IndexMongoUpgrader implements Upgrader {

    protected ReactiveMongoOperations template;

    private String prefix;

    @Autowired
    @Qualifier("indexManagementReactiveMongoTemplate")
    public void setMongoTemplate(ReactiveMongoOperations template) {
        this.template = template;
    }

    @Autowired
    public void setEnvironment(Environment environment) {
        this.prefix = environment.getProperty("management.mongodb.prefix", "");
    }

    protected Mono<MongoCollection<Document>> getCollection(String collectionName) {
        return template.getCollection(buildCollectionName(collectionName));
    }

    protected String buildCollectionName(String collectionName) {
        return prefix + collectionName;
    }
}
