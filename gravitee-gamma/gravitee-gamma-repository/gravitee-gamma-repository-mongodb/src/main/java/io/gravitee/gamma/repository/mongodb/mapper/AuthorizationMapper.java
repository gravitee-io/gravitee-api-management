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
package io.gravitee.gamma.repository.mongodb.mapper;

import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import io.gravitee.gamma.repository.mongodb.internal.model.AuthorizationEntityMongo;
import io.gravitee.gamma.repository.mongodb.internal.model.AuthorizationPolicyMongo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthorizationMapper {
    default AuthorizationEntity map(AuthorizationEntityMongo mongo) {
        if (mongo == null) {
            return null;
        }
        return AuthorizationEntity.builder()
            .id(mongo.id())
            .entityId(mongo.entityId())
            .kind(mongo.kind())
            .attributes(mongo.attributes())
            .parents(mongo.parents())
            .source(mongo.source())
            .environmentId(mongo.environmentId())
            .createdAt(mongo.createdAt())
            .updatedAt(mongo.updatedAt())
            .build();
    }

    default AuthorizationEntityMongo map(AuthorizationEntity domain) {
        if (domain == null) {
            return null;
        }
        return new AuthorizationEntityMongo()
            .id(domain.id())
            .entityId(domain.entityId())
            .kind(domain.kind())
            .attributes(domain.attributes())
            .parents(domain.parents())
            .source(domain.source())
            .environmentId(domain.environmentId())
            .createdAt(domain.createdAt())
            .updatedAt(domain.updatedAt());
    }

    default AuthorizationPolicy map(AuthorizationPolicyMongo mongo) {
        if (mongo == null) {
            return null;
        }
        return AuthorizationPolicy.builder()
            .id(mongo.id())
            .name(mongo.name())
            .kind(mongo.kind())
            .entityId(mongo.entityId())
            .policyText(mongo.policyText())
            .status(mongo.status())
            .environmentId(mongo.environmentId())
            .createdAt(mongo.createdAt())
            .updatedAt(mongo.updatedAt())
            .build();
    }

    default AuthorizationPolicyMongo map(AuthorizationPolicy domain) {
        if (domain == null) {
            return null;
        }
        return new AuthorizationPolicyMongo()
            .id(domain.id())
            .name(domain.name())
            .kind(domain.kind())
            .entityId(domain.entityId())
            .policyText(domain.policyText())
            .status(domain.status())
            .environmentId(domain.environmentId())
            .createdAt(domain.createdAt())
            .updatedAt(domain.updatedAt());
    }
}
