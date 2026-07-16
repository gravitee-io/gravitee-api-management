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
package io.gravitee.gamma.core.resource.use_case;

import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.anAuditInfo;
import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.anUpdateCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gamma.inmemory.ResourceCrudServiceInMemory;
import gamma.inmemory.ResourcePluginProviderInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.core.domain.resource.domain_service.ValidateUpdateResourceCommandDomainService;
import io.gravitee.gamma.core.domain.resource.exception.ResourceNotFoundException;
import io.gravitee.gamma.core.domain.resource.exception.ResourceValidationException;
import io.gravitee.gamma.core.domain.resource.model.ResourceAuditEvent;
import io.gravitee.gamma.core.domain.resource.use_case.UpdateResourceUseCase;
import io.gravitee.gamma.core.resource.fixture.ResourceFixture;
import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateResourceUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");

    private ResourceCrudServiceInMemory repository;
    private AuditCrudServiceInMemory auditCrudService;
    private UpdateResourceUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(seed -> seed != null ? seed : "generated-id");
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));

        repository = new ResourceCrudServiceInMemory();
        auditCrudService = new AuditCrudServiceInMemory();
        useCase = new UpdateResourceUseCase(
            repository,
            new ValidateUpdateResourceCommandDomainService(repository, new JsonSchemaValidatorImpl(), new ResourcePluginProviderInMemory()),
            new AuditDomainService(auditCrudService, new UserCrudServiceInMemory(), new JacksonJsonDiffProcessor())
        );
    }

    @Test
    void should_update_definition_and_bump_updated_at() {
        var existing = ResourceFixture.aResource();
        repository.initWith(List.of(existing));
        var command = anUpdateCommand(c -> c.configuration("{\"ttl\":60}"));

        var output = useCase.execute(new UpdateResourceUseCase.Input(anAuditInfo(), existing.id(), command));

        assertThat(output.resource().definition().getConfiguration()).isEqualTo("{\"ttl\":60}");
        assertThat(output.resource().updatedAt()).isAfterOrEqualTo(existing.updatedAt());
    }

    @Test
    void should_emit_audit_log_on_update() {
        var existing = ResourceFixture.aResource();
        repository.initWith(List.of(existing));
        var command = anUpdateCommand(c -> c.configuration("{\"ttl\":60}"));

        useCase.execute(new UpdateResourceUseCase.Input(anAuditInfo(), existing.id(), command));

        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .contains(
                AuditEntity.builder()
                    .id("generated-id")
                    .organizationId(ResourceFixture.DEFAULT_ORGANIZATION_ID)
                    .environmentId(ResourceFixture.DEFAULT_ENVIRONMENT_ID)
                    .referenceType(AuditEntity.AuditReferenceType.ENVIRONMENT)
                    .referenceId(ResourceFixture.DEFAULT_ENVIRONMENT_ID)
                    .user(ResourceFixture.DEFAULT_USER_ID)
                    .properties(Map.of(AuditProperties.RESOURCE.name(), ResourceFixture.DEFAULT_ID))
                    .event(ResourceAuditEvent.RESOURCE_UPDATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_skip_uniqueness_check_when_name_unchanged() {
        // Two resources with different names
        var existing = ResourceFixture.aResource();
        var other = ResourceFixture.aResource(r -> r.id("other").definition(ResourceFixture.aDefinition(d -> d.name("other"))));
        repository.initWith(List.of(existing, other));

        // Update `existing` keeping the same name — must not conflict
        var command = anUpdateCommand(c -> c.configuration("{\"ttl\":90}"));
        var output = useCase.execute(new UpdateResourceUseCase.Input(anAuditInfo(), existing.id(), command));

        assertThat(output.resource().definition().getName()).isEqualTo("my-cache");
    }

    @Test
    void should_reject_rename_when_colliding_with_existing_name() {
        var existing = ResourceFixture.aResource();
        var other = ResourceFixture.aResource(r -> r.id("other").definition(ResourceFixture.aDefinition(d -> d.name("other"))));
        repository.initWith(List.of(existing, other));

        var command = anUpdateCommand(c -> c.name("other"));

        assertThatThrownBy(() -> useCase.execute(new UpdateResourceUseCase.Input(anAuditInfo(), existing.id(), command))).isInstanceOf(
            ResourceValidationException.class
        );
    }

    @Test
    void should_throw_when_resource_not_found() {
        var command = anUpdateCommand();

        assertThatThrownBy(() -> useCase.execute(new UpdateResourceUseCase.Input(anAuditInfo(), "unknown", command))).isInstanceOf(
            ResourceNotFoundException.class
        );
    }

    @Test
    void should_throw_when_resource_in_different_environment() {
        var existing = ResourceFixture.aResource(r -> r.referenceId("OTHER"));
        repository.initWith(List.of(existing));
        var command = anUpdateCommand();

        assertThatThrownBy(() -> useCase.execute(new UpdateResourceUseCase.Input(anAuditInfo(), existing.id(), command))).isInstanceOf(
            ResourceNotFoundException.class
        );
    }
}
