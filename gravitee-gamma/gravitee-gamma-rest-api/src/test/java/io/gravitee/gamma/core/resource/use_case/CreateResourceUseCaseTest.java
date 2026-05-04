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

import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.DEFAULT_ENVIRONMENT_ID;
import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.DEFAULT_NEW_ID;
import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.aCreateCommand;
import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.aPlatformPlugin;
import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.anAuditInfo;
import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.pluginJsonSchema;
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
import io.gravitee.gamma.core.domain.resource.domain_service.ValidateCreateResourceCommandDomainService;
import io.gravitee.gamma.core.domain.resource.exception.ResourceValidationException;
import io.gravitee.gamma.core.domain.resource.model.Resource;
import io.gravitee.gamma.core.domain.resource.model.ResourceAuditEvent;
import io.gravitee.gamma.core.domain.resource.use_case.CreateResourceUseCase;
import io.gravitee.gamma.core.resource.fixture.ResourceFixture;
import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateResourceUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");

    private ResourceCrudServiceInMemory resourceCrudService;
    private AuditCrudServiceInMemory auditCrudService;
    private CreateResourceUseCase useCase;

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

        ResourcePluginProviderInMemory resourcePluginProvider = new ResourcePluginProviderInMemory();
        resourceCrudService = new ResourceCrudServiceInMemory();
        auditCrudService = new AuditCrudServiceInMemory();
        var auditService = new AuditDomainService(auditCrudService, new UserCrudServiceInMemory(), new JacksonJsonDiffProcessor());
        useCase = new CreateResourceUseCase(
            resourceCrudService,
            new ValidateCreateResourceCommandDomainService(resourceCrudService, new JsonSchemaValidatorImpl(), resourcePluginProvider),
            auditService
        );

        resourcePluginProvider.addPlugin(aPlatformPlugin(), pluginJsonSchema());
    }

    @Test
    void should_create_resource_with_supplied_id_and_defaults() {
        var command = aCreateCommand();

        var output = useCase.execute(new CreateResourceUseCase.Input(anAuditInfo(), command));

        ThrowingConsumer<Resource> assertResource = r -> {
            assertThat(r.id()).isEqualTo(DEFAULT_NEW_ID);
            assertThat(r.referenceType()).isEqualTo(Resource.ReferenceType.ENVIRONMENT);
            assertThat(r.referenceId()).isEqualTo(DEFAULT_ENVIRONMENT_ID);
            assertThat(r.definition().getName()).isEqualTo("my-cache");
            assertThat(r.createdAt()).isNotNull();
        };
        assertThat(output.resource()).satisfies(assertResource);
        assertThat(resourceCrudService.storage()).anySatisfy(assertResource);
    }

    @Test
    void should_emit_audit_log_on_create() {
        var command = aCreateCommand();

        useCase.execute(new CreateResourceUseCase.Input(anAuditInfo(), command));

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
                    .properties(Map.of(AuditProperties.RESOURCE.name(), ResourceFixture.DEFAULT_NEW_ID))
                    .event(ResourceAuditEvent.RESOURCE_CREATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_reject_duplicate_name_for_same_environment() {
        resourceCrudService.initWith(java.util.List.of(ResourceFixture.aResource()));
        var command = aCreateCommand();

        assertThatThrownBy(() -> useCase.execute(new CreateResourceUseCase.Input(anAuditInfo(), command)))
            .isInstanceOf(ResourceValidationException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void should_reject_blank_name() {
        var command = aCreateCommand(c -> c.name(""));

        assertThatThrownBy(() -> useCase.execute(new CreateResourceUseCase.Input(anAuditInfo(), command))).isInstanceOf(
            ResourceValidationException.class
        );
    }

    @Test
    void should_reject_invalid_json_configuration() {
        var command = aCreateCommand(c -> c.configuration("not-json"));

        assertThatThrownBy(() -> useCase.execute(new CreateResourceUseCase.Input(anAuditInfo(), command)))
            .isInstanceOf(ResourceValidationException.class)
            .hasMessageContaining("Configuration invalid");
    }
}
