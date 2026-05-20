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
package io.gravitee.apim.core.user.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateUserDomainServiceImplTest {

    private static final Instant INSTANT_NOW = Instant.parse("2024-06-01T12:00:00Z");
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");

    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    CreateUserDomainServiceImpl service;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
        UuidString.overrideGenerator(() -> "generated-uuid");
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
        UuidString.reset();
    }

    @BeforeEach
    void setUp() {
        service = new CreateUserDomainServiceImpl(userCrudService);
    }

    @AfterEach
    void tearDown() {
        userCrudService.reset();
    }

    @Test
    void should_create_external_user_with_all_fields() {
        var result = service.createExternalUser(EXECUTION_CONTEXT, "jane@example.com", Optional.of("Jane"), Optional.of("Doe"));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getId()).isEqualTo("generated-uuid");
            soft.assertThat(result.getOrganizationId()).isEqualTo("org-id");
            soft.assertThat(result.getSource()).isEqualTo("gravitee");
            soft.assertThat(result.getSourceId()).isEqualTo("jane@example.com");
            soft.assertThat(result.getEmail()).isEqualTo("jane@example.com");
            soft.assertThat(result.getFirstname()).isEqualTo("Jane");
            soft.assertThat(result.getLastname()).isEqualTo("Doe");
            soft.assertThat(result.getStatus()).isEqualTo("ACTIVE");
            soft.assertThat(result.getCreatedAt()).isEqualTo(Date.from(INSTANT_NOW));
            soft.assertThat(result.getUpdatedAt()).isEqualTo(Date.from(INSTANT_NOW));
        });
    }

    @Test
    void should_use_null_for_absent_firstname_and_lastname() {
        var result = service.createExternalUser(EXECUTION_CONTEXT, "nofirstlast@example.com", Optional.empty(), Optional.empty());

        assertThat(result.getFirstname()).isNull();
        assertThat(result.getLastname()).isNull();
    }

    @Test
    void should_store_user_in_crud_service() {
        service.createExternalUser(EXECUTION_CONTEXT, "stored@example.com", Optional.empty(), Optional.empty());

        assertThat(userCrudService.storage()).hasSize(1).extracting(BaseUserEntity::getEmail).containsExactly("stored@example.com");
    }
}
