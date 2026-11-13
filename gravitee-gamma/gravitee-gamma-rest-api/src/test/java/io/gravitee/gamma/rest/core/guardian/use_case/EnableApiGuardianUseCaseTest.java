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

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.API_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDIAN_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.guardian.Guardian.GuardianBuilder;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.domain_service.DescribeApiGuardiansDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.ApiCannotRunGuardiansException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianAlreadyEnabledException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianIncompatibleApiTypeException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotDeployedException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class EnableApiGuardianUseCaseTest {

    private static final Instant DEPLOYED_AT = Instant.parse("2026-11-09T10:00:00Z");

    private final InMemoryGuardianRepository guardians = new InMemoryGuardianRepository();
    private final GuardianPublicationFixture publications = new GuardianPublicationFixture();
    private final GuardianAuditFixture audits = new GuardianAuditFixture();
    private final InMemoryGuardedApiPort apis = new InMemoryGuardedApiPort();
    private final AuditInfo actor = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("publisher").build())
        .build();
    private final EnableApiGuardianUseCase useCase = new EnableApiGuardianUseCase(
        apis,
        guardians,
        publications.service(guardians),
        new DescribeApiGuardiansDomainService(guardians),
        audits.service
    );

    @Test
    void should_enable_a_deployed_guardian_after_those_of_its_phase() {
        givenDeployedGuardian(d -> d.id("guardian-0").name("Prompt injection"));
        givenDeployedGuardian(d -> d.name("Credential leakage"));
        apis.givenApi(
            aGuardedApi().internalFlow(Flow.builder().request(List.of(GuardianSteps.of("guardian-0", "Prompt injection"))).build()).build()
        );

        var output = useCase.execute(new EnableApiGuardianUseCase.Input(actor, API_ID, GUARDIAN_ID));

        assertSoftly(softly -> {
            softly
                .assertThat(output.guardians())
                .as("guardians enabled on the API")
                .containsExactly(
                    new ApiGuardian("guardian-0", "Prompt injection", Phase.REQUEST, 1),
                    new ApiGuardian(GUARDIAN_ID, "Credential leakage", Phase.REQUEST, 2)
                );
            softly
                .assertThat(apis.findById(ENVIRONMENT_ID, API_ID).orElseThrow().internalFlow().getRequest())
                .as("stored request steps")
                .containsExactly(GuardianSteps.of("guardian-0", "Prompt injection"), GuardianSteps.of(GUARDIAN_ID, "Credential leakage"));
        });
    }

    @Test
    void should_enable_the_guardian_on_the_phase_of_its_deployed_revision() {
        givenDeployedGuardian(d -> d.phase(Phase.REQUEST));
        guardians.update(aGuardian(d -> d.phase(Phase.RESPONSE)).build());
        apis.givenApi(aGuardedApi().build());

        var output = useCase.execute(new EnableApiGuardianUseCase.Input(actor, API_ID, GUARDIAN_ID));

        assertThat(output.guardians()).as("guardians enabled on the API").extracting(ApiGuardian::phase).containsExactly(Phase.REQUEST);
    }

    @Test
    void should_record_the_change_in_the_audit_trail_of_the_api() {
        givenDeployedGuardian(d -> d);
        apis.givenApi(aGuardedApi().build());

        useCase.execute(new EnableApiGuardianUseCase.Input(actor, API_ID, GUARDIAN_ID));

        assertThat(audits.storage())
            .as("audits")
            .singleElement()
            .satisfies(audit ->
                assertSoftly(softly -> {
                    softly.assertThat(audit.getEvent()).as("event").isEqualTo(ApiAuditEvent.API_UPDATED.name());
                    softly.assertThat(audit.getReferenceId()).as("API reference").isEqualTo(API_ID);
                    softly.assertThat(audit.getUser()).as("actor").isEqualTo("publisher");
                    softly.assertThat(audit.getProperties()).as("guardian reference").containsEntry("GUARDIAN", GUARDIAN_ID);
                })
            );
    }

    @Test
    void should_refuse_a_guardian_that_is_not_deployed() {
        guardians.givenGuardian(aGuardian().build());
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> useCase.execute(new EnableApiGuardianUseCase.Input(actor, API_ID, GUARDIAN_ID)))
            .as("undeployed guardian")
            .isInstanceOf(GuardianNotDeployedException.class);
    }

    @Test
    void should_refuse_a_guardian_of_another_environment() {
        givenDeployedGuardian(d -> d.environmentId("other-env"));
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> useCase.execute(new EnableApiGuardianUseCase.Input(actor, API_ID, GUARDIAN_ID)))
            .as("guardian of another environment")
            .isInstanceOf(GuardianNotFoundException.class);
    }

    @Test
    void should_refuse_a_guardian_that_does_not_support_the_api_type() {
        givenDeployedGuardian(d -> d.apiTypes(Set.of(ApiType.PROXY)));
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> useCase.execute(new EnableApiGuardianUseCase.Input(actor, API_ID, GUARDIAN_ID)))
            .as("guardian not supporting llm-proxy")
            .isInstanceOf(GuardianIncompatibleApiTypeException.class);
    }

    @Test
    void should_refuse_a_guardian_already_enabled_on_the_api() {
        givenDeployedGuardian(d -> d);
        apis.givenApi(
            aGuardedApi().internalFlow(Flow.builder().request(List.of(GuardianSteps.of(GUARDIAN_ID, "Credential leakage"))).build()).build()
        );

        assertThatThrownBy(() -> useCase.execute(new EnableApiGuardianUseCase.Input(actor, API_ID, GUARDIAN_ID)))
            .as("guardian already enabled")
            .isInstanceOf(GuardianAlreadyEnabledException.class);
    }

    @Test
    void should_refuse_an_api_that_cannot_run_guardians() {
        givenDeployedGuardian(d -> d);
        apis.givenApi(aGuardedApi().supportsGuardians(false).build());

        assertThatThrownBy(() -> useCase.execute(new EnableApiGuardianUseCase.Input(actor, API_ID, GUARDIAN_ID)))
            .as("API without internal flow")
            .isInstanceOf(ApiCannotRunGuardiansException.class);
    }

    @Test
    void should_refuse_an_api_of_another_environment() {
        givenDeployedGuardian(d -> d);
        apis.givenApi(aGuardedApi().environmentId("other-env").build());

        assertThatThrownBy(() -> useCase.execute(new EnableApiGuardianUseCase.Input(actor, API_ID, GUARDIAN_ID)))
            .as("API of another environment")
            .isInstanceOf(ApiNotFoundException.class);
    }

    private void givenDeployedGuardian(UnaryOperator<GuardianBuilder> definition) {
        Guardian guardian = aGuardian(definition).build();
        guardians.givenGuardian(guardian);
        publications.givenPublication(guardian.getDefinition(), DEPLOYED_AT);
    }
}
