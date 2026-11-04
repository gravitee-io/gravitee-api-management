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
package io.gravitee.apim.core.guardian.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.GuardianFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.GuardianCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.guardian.domain_service.ValidateCreateGuardianDomainService;
import io.gravitee.apim.core.guardian.model.GuardianAuditEvent;
import io.gravitee.apim.core.guardian.model.GuardianLifecycleState;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateGuardianUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORG_ID)
        .environmentId(ENV_ID)
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private final GuardianCrudServiceInMemory guardianCrudService = new GuardianCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();

    private CreateGuardianUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        useCase = new CreateGuardianUseCase(
            guardianCrudService,
            new ValidateCreateGuardianDomainService(),
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor())
        );
    }

    @AfterEach
    void tearDown() {
        guardianCrudService.reset();
        auditCrudService.reset();
    }

    @Test
    void should_create_a_guardian_undeployed_with_a_generated_id() {
        var output = useCase.execute(new CreateGuardianUseCase.Input(GuardianFixtures.aCreateGuardian().build(), AUDIT_INFO));

        assertThat(output.guardian())
            .extracting(g -> g.getId(), g -> g.getEnvironmentId(), g -> g.getLifecycleState(), g -> g.getApiType())
            .containsExactly("generated-id", ENV_ID, GuardianLifecycleState.UNDEPLOYED, ApiType.LLM_PROXY);
        assertThat(output.guardian().getCreatedAt()).isNotNull();
        assertThat(output.guardian().getUpdatedAt()).isEqualTo(output.guardian().getCreatedAt());
        assertThat(guardianCrudService.storage()).hasSize(1);
    }

    @Test
    void should_keep_the_id_when_one_is_provided() {
        var output = useCase.execute(new CreateGuardianUseCase.Input(GuardianFixtures.aCreateGuardian().id("my-id").build(), AUDIT_INFO));

        assertThat(output.guardian().getId()).isEqualTo("my-id");
    }

    @Test
    void should_write_an_audit_log() {
        useCase.execute(new CreateGuardianUseCase.Input(GuardianFixtures.aCreateGuardian().build(), AUDIT_INFO));

        assertThat(auditCrudService.storage())
            .hasSize(1)
            .first()
            .satisfies(audit -> assertThat(audit.getEvent()).isEqualTo(GuardianAuditEvent.GUARDIAN_CREATED.name()));
    }

    @Test
    void should_refuse_an_api_type_other_than_llm_proxy() {
        var input = new CreateGuardianUseCase.Input(GuardianFixtures.aCreateGuardian().apiType(ApiType.PROXY).build(), AUDIT_INFO);

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("llm-proxy");
        assertThat(guardianCrudService.storage()).isEmpty();
    }

    @Test
    void should_refuse_a_blank_name() {
        var input = new CreateGuardianUseCase.Input(GuardianFixtures.aCreateGuardian().name("  ").build(), AUDIT_INFO);

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("Name");
    }

    @Test
    void should_refuse_a_missing_phase() {
        var input = new CreateGuardianUseCase.Input(GuardianFixtures.aCreateGuardian().phase(null).build(), AUDIT_INFO);

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("Phase");
    }
}
