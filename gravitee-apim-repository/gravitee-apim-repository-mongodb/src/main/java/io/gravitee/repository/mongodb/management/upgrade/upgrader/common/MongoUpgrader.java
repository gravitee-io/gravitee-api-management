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

import io.gravitee.node.api.upgrader.Upgrader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author GraviteeSource Team
 */
public abstract class MongoUpgrader implements Upgrader {

    protected MongoTemplate template;

    private String prefix;

    @Autowired
    public void setMongoTemplate(MongoTemplate template) {
        this.template = template;
    }

    @Autowired
    public void setEnvironment(Environment environment) {
        this.prefix = environment.getProperty("management.mongodb.prefix", "");
    }

    protected String buildCollectionName(String collectionName) {
        return prefix + collectionName;
    }
}
