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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntityKind;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyKind;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyStatus;
import io.gravitee.gamma.repository.mongodb.internal.model.AuthorizationEntityMongo;
import io.gravitee.gamma.repository.mongodb.internal.model.AuthorizationPolicyMongo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AuthorizationMapperTest {

    private final AuthorizationMapper mapper = Mappers.getMapper(AuthorizationMapper.class);

    @Test
    void should_map_authorization_entity_round_trip() {
        AuthorizationEntity entity = AuthorizationEntity.builder()
            .id("entity-id-1")
            .entityId("logical-entity-id")
            .kind(AuthorizationEntityKind.RESOURCE)
            .attributes(Map.of("env", "prod", "tier", "gold"))
            .parents(List.of("parent-1", "parent-2"))
            .source("manual")
            .environmentId("env-1")
            .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
            .updatedAt(Instant.parse("2026-01-02T11:00:00Z"))
            .build();

        AuthorizationEntityMongo mongo = mapper.map(entity);
        AuthorizationEntity result = mapper.map(mongo);

        assertThat(result).usingRecursiveComparison().isEqualTo(entity);
    }

    @Test
    void should_map_authorization_policy_round_trip() {
        AuthorizationPolicy policy = AuthorizationPolicy.builder()
            .id("policy-id-1")
            .name("Allow read on resource")
            .kind(AuthorizationPolicyKind.RESOURCE)
            .entityId("resource-1")
            .policyText("allow if subject.role == 'admin'")
            .status(AuthorizationPolicyStatus.DEPLOYED)
            .environmentId("env-1")
            .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
            .updatedAt(Instant.parse("2026-01-02T11:00:00Z"))
            .build();

        AuthorizationPolicyMongo mongo = mapper.map(policy);
        AuthorizationPolicy result = mapper.map(mongo);

        assertThat(result).usingRecursiveComparison().isEqualTo(policy);
    }

    @Test
    void should_return_null_when_mapping_null_entity() {
        assertThat(mapper.map((AuthorizationEntity) null)).isNull();
        assertThat(mapper.map((AuthorizationEntityMongo) null)).isNull();
    }

    @Test
    void should_return_null_when_mapping_null_policy() {
        assertThat(mapper.map((AuthorizationPolicy) null)).isNull();
        assertThat(mapper.map((AuthorizationPolicyMongo) null)).isNull();
    }

    @Test
    void should_preserve_kind_enum_value_for_entity() {
        AuthorizationEntity entity = AuthorizationEntity.builder().kind(AuthorizationEntityKind.PRINCIPAL).build();
        assertThat(mapper.map(entity).kind()).isEqualTo(AuthorizationEntityKind.PRINCIPAL);
    }

    @Test
    void should_preserve_status_enum_value_for_policy() {
        AuthorizationPolicy policy = AuthorizationPolicy.builder().status(AuthorizationPolicyStatus.DRAFT).build();
        assertThat(mapper.map(policy).status()).isEqualTo(AuthorizationPolicyStatus.DRAFT);
    }
}
