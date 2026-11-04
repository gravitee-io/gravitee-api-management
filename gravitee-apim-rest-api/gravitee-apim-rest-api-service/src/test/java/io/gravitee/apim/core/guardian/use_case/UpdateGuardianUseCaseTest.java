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
import io.gravitee.apim.core.guardian.domain_service.ValidateUpdateGuardianDomainService;
import io.gravitee.apim.core.guardian.model.GuardianAuditEvent;
import io.gravitee.apim.core.guardian.model.GuardianPhase;
import io.gravitee.apim.core.guardian.model.UpdateGuardian;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateGuardianUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("org-id")
        .environmentId("env-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private final GuardianCrudServiceInMemory guardianCrudService = new GuardianCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();

    private UpdateGuardianUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateGuardianUseCase(
            guardianCrudService,
            new ValidateUpdateGuardianDomainService(),
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor())
        );
        guardianCrudService.initWith(List.of(GuardianFixtures.aGuardian().build()));
    }

    @AfterEach
    void tearDown() {
        guardianCrudService.reset();
        auditCrudService.reset();
    }

    @Test
    void should_update_the_general_settings() {
        var output = useCase.execute(
            new UpdateGuardianUseCase.Input("guardian-id", GuardianFixtures.anUpdateGuardian().build(), AUDIT_INFO)
        );

        assertThat(output.guardian())
            .extracting(g -> g.getName(), g -> g.getDescription(), g -> g.getVersion())
            .containsExactly("Renamed", "New description", "2.0.0");
    }

    @Test
    void should_keep_the_fields_the_payload_omits() {
        var output = useCase.execute(
            new UpdateGuardianUseCase.Input("guardian-id", UpdateGuardian.builder().name("Renamed").build(), AUDIT_INFO)
        );

        assertThat(output.guardian())
            .extracting(g -> g.getName(), g -> g.getDescription(), g -> g.getVersion())
            .containsExactly("Renamed", "Blocks a prompt that carries a credential", "1.0.0");
    }

    @Test
    void should_leave_the_definition_the_api_type_and_the_phase_untouched() {
        var output = useCase.execute(
            new UpdateGuardianUseCase.Input("guardian-id", GuardianFixtures.anUpdateGuardian().build(), AUDIT_INFO)
        );

        assertThat(output.guardian().getDefinition()).isEqualTo("{\"policies\":[]}");
        assertThat(output.guardian().getApiType()).isEqualTo(ApiType.LLM_PROXY);
        assertThat(output.guardian().getPhase()).isEqualTo(GuardianPhase.REQUEST);
    }

    @Test
    void should_touch_the_update_date() {
        var before = guardianCrudService.storage().get(0).getUpdatedAt();

        var output = useCase.execute(
            new UpdateGuardianUseCase.Input("guardian-id", GuardianFixtures.anUpdateGuardian().build(), AUDIT_INFO)
        );

        assertThat(output.guardian().getUpdatedAt()).isAfter(before);
    }

    @Test
    void should_write_an_audit_log() {
        useCase.execute(new UpdateGuardianUseCase.Input("guardian-id", GuardianFixtures.anUpdateGuardian().build(), AUDIT_INFO));

        assertThat(auditCrudService.storage())
            .hasSize(1)
            .first()
            .satisfies(audit -> assertThat(audit.getEvent()).isEqualTo(GuardianAuditEvent.GUARDIAN_UPDATED.name()));
    }

    @Test
    void should_refuse_a_blank_name() {
        var input = new UpdateGuardianUseCase.Input("guardian-id", GuardianFixtures.anUpdateGuardian().name("").build(), AUDIT_INFO);

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("Name");
    }
}
