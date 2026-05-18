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
package io.gravitee.repository.mongodb.management.authorization;

import io.gravitee.apim.authorization.domain.Entity;
import io.gravitee.apim.authorization.domain.EntityKind;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "authz_entities")
public class EntityDocument {

    @Id
    private String id;

    @Field("entityId")
    private String entityId;

    @Field("kind")
    private EntityKind kind;

    @Field("attributes")
    private Map<String, Object> attributes;

    @Field("parents")
    private List<String> parents;

    @Field("source")
    private String source;

    @Field("environmentId")
    private String environmentId;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;

    public static EntityDocument fromDomain(Entity entity) {
        return new EntityDocument(
            entity.id(),
            entity.entityId(),
            entity.kind(),
            entity.attributes(),
            entity.parents(),
            entity.source(),
            entity.environmentId(),
            entity.createdAt(),
            entity.updatedAt()
        );
    }

    public Entity toDomain() {
        return new Entity(id, entityId, kind, attributes, parents, source, environmentId, createdAt, updatedAt);
    }
}
