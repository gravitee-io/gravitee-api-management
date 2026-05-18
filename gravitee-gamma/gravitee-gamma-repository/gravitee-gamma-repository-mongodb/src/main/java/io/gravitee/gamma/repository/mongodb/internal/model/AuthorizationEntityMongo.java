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
package io.gravitee.gamma.repository.mongodb.internal.model;

import io.gravitee.gamma.repository.authorization.model.AuthorizationEntityKind;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Accessors(fluent = true)
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}authz_entities")
public class AuthorizationEntityMongo {

    @Id
    private String id;

    private String entityId;
    private AuthorizationEntityKind kind;
    private Map<String, Object> attributes;
    private List<String> parents;
    private String source;
    private String environmentId;
    private Instant createdAt;
    private Instant updatedAt;
}
