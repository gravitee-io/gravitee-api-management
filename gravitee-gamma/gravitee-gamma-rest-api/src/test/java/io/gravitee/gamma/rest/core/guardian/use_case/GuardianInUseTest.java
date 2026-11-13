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
package io.gravitee.gamma.rest.core.guardian.use_case;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDIAN_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aDefinitionToSubmit;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianValidator;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.anUpdateGuardianCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianUsageDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianInUseException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A Guardian enabled on an API keeps running there: it can be neither withdrawn nor deleted, and what decides where it
 * runs — its phase and its API types — cannot change under the APIs.
 */
class GuardianInUseTest {

    private static final Instant DEPLOYED_AT = Instant.parse("2026-11-09T10:00:00Z");

    private final InMemoryGuardianRepository guardians = new InMemoryGuardianRepository();
    private final GuardianPublicationFixture publications = new GuardianPublicationFixture();
    private final GuardianAuditFixture audits = new GuardianAuditFixture();
    private final InMemoryGuardedApiPort apis = new InMemoryGuardedApiPort();
    private final GuardianUsageDomainService usage = new GuardianUsageDomainService(apis);
    private final AuditInfo actor = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("owner").build())
        .build();

    @BeforeEach
    void given_a_guardian_enabled_on_an_api() {
        apis.givenApi(
            aGuardedApi()
                .name("Support assistant")
                .internalFlow(Flow.builder().request(List.of(GuardianSteps.of(GUARDIAN_ID, "Credential leakage"))).build())
                .build()
        );
    }

    @Test
    void should_refuse_to_undeploy_it() {
        givenDeployedGuardian();
        var useCase = new UndeployGuardianUseCase(guardians, publications.service(guardians), audits.service, usage);

        assertThatThrownBy(() -> useCase.execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID)))
            .as("undeploying a guardian in use")
            .isInstanceOf(GuardianInUseException.class)
            .hasMessageContaining("Support assistant");
        assertThat(publications.find(ENVIRONMENT_ID, GUARDIAN_ID)).as("publication").get().isInstanceOf(GuardianDeployment.Published.class);
    }

    @Test
    void should_refuse_to_delete_it() {
        guardians.givenGuardian(aGuardian().build());
        var useCase = new DeleteGuardianUseCase(guardians, audits.service, publications.service(guardians), usage);

        assertThatThrownBy(() -> useCase.execute(DeleteGuardianUseCase.Input.builder().auditInfo(actor).guardianId(GUARDIAN_ID).build()))
            .as("deleting a guardian in use")
            .isInstanceOf(GuardianInUseException.class);
        assertThat(guardians.storedGuardians()).as("stored guardians").hasSize(1);
    }

    @Test
    void should_refuse_to_change_its_phase() {
        givenDeployedGuardian();

        assertThatThrownBy(() -> update(anUpdateGuardianCommand().definition(aDefinitionToSubmit().phase(Phase.RESPONSE).build()).build()))
            .as("moving a guardian in use to another phase")
            .isInstanceOf(GuardianInUseException.class);
    }

    @Test
    void should_update_the_rest_of_its_definition() {
        givenDeployedGuardian();

        var output = update(anUpdateGuardianCommand().definition(aDefinitionToSubmit().name("Renamed").build()).build());

        assertThat(output.guardian().getName()).as("name").isEqualTo("Renamed");
    }

    private UpdateGuardianUseCase.Output update(UpdateGuardianCommand command) {
        var useCase = new UpdateGuardianUseCase(
            guardians,
            aGuardianValidator(guardians),
            audits.service,
            publications.service(guardians),
            usage
        );
        return useCase.execute(
            UpdateGuardianUseCase.Input.builder().auditInfo(actor).guardianId(GUARDIAN_ID).guardianToUpdate(command).build()
        );
    }

    private void givenDeployedGuardian() {
        Guardian guardian = aGuardian().deployedAt(DEPLOYED_AT).build();
        guardians.givenGuardian(guardian);
        publications.givenPublication(guardian.getDefinition(), DEPLOYED_AT);
    }
}
