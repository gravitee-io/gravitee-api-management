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

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.IndexOptions;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.bson.BsonDocument;
import org.bson.BsonValue;

/**
 * @author GraviteeSource Team
 */
@Builder
public class Index {

    private String name;

    @Getter
    private String collection;

    @Singular
    private Map<String, BsonValue> keys;

    private boolean unique;

    private Collation collation;

    public BsonDocument bson() {
        BsonDocument bson = new BsonDocument();
        for (var entry : keys.entrySet()) {
            bson.append(entry.getKey(), entry.getValue());
        }
        return bson;
    }

    public IndexOptions options() {
        return new IndexOptions().name(name).unique(unique).collation(collation);
    }
}
