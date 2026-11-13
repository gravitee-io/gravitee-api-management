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
package io.gravitee.gamma.rest.infra.adapter;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.API_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDIAN_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.ApiFixtures;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.guardian.Guardian.GuardianBuilder;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.domain_service.ValidateApiGuardiansDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class GuardiansDeploymentValidatorAdapterTest {

    private static final Instant DEPLOYED_AT = Instant.parse("2026-11-09T10:00:00Z");

    private final InMemoryGuardianRepository guardians = new InMemoryGuardianRepository();
    private final GuardianPublicationFixture publications = new GuardianPublicationFixture();
    private final GuardiansDeploymentValidatorAdapter validator = new GuardiansDeploymentValidatorAdapter(
        new ValidateApiGuardiansDomainService(guardians, publications.service(guardians))
    );

    private Flow deployedInternalFlow;

    @Test
    void should_let_an_api_deploy_the_guardians_its_definition_runs() {
        givenDeployedGuardian(d -> d.phase(Phase.REQUEST));
        givenApiRunning(Phase.REQUEST, GUARDIAN_ID);

        assertThatCode(this::validateDeployable).as("API running a deployed guardian").doesNotThrowAnyException();
    }

    @Test
    void should_refuse_an_api_running_a_guardian_the_environment_no_longer_holds() {
        givenApiRunning(Phase.REQUEST, GUARDIAN_ID);

        assertThatThrownBy(this::validateDeployable)
            .as("API running a deleted guardian")
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessageContaining(GUARDIAN_ID)
            .cause()
            .as("refusal of the guardian, kept with its parameters")
            .isInstanceOf(GuardianNotFoundException.class);
    }

    @Test
    void should_refuse_an_api_running_a_guardian_that_is_no_longer_deployed() {
        guardians.givenGuardian(aGuardian().build());
        givenApiRunning(Phase.REQUEST, GUARDIAN_ID);

        assertThatThrownBy(this::validateDeployable).as("API running an undeployed guardian").isInstanceOf(ApiNotDeployableException.class);
    }

    @Test
    void should_refuse_an_api_running_a_guardian_that_does_not_support_its_type() {
        givenDeployedGuardian(d -> d.apiTypes(Set.of(ApiType.PROXY)));
        givenApiRunning(Phase.REQUEST, GUARDIAN_ID);

        assertThatThrownBy(this::validateDeployable)
            .as("API running a guardian of another API type")
            .isInstanceOf(ApiNotDeployableException.class);
    }

    @Test
    void should_refuse_an_api_running_a_guardian_in_a_phase_the_deployed_revision_left() {
        givenDeployedGuardian(d -> d.phase(Phase.RESPONSE));
        givenApiRunning(Phase.REQUEST, GUARDIAN_ID);

        assertThatThrownBy(this::validateDeployable)
            .as("API running a response guardian on its request phase")
            .isInstanceOf(ApiNotDeployableException.class);
    }

    @Test
    void should_say_nothing_about_an_api_that_runs_no_guardian() {
        assertThatCode(this::validateDeployable).as("API without guardian").doesNotThrowAnyException();
    }

    @Test
    void should_say_nothing_about_an_api_whose_definition_has_no_internal_flow() {
        assertThatCode(() -> validator.validateDeployable(ApiFixtures.aProxyApiV2()))
            .as("API that cannot run Guardians")
            .doesNotThrowAnyException();
    }

    /** The definition being deployed, which is not always the one the store holds, is what the check reads. */
    private void validateDeployable() {
        var api = ApiFixtures.aLLMProxyApiV4().toBuilder().id(API_ID).environmentId(ENVIRONMENT_ID).build();
        api.getApiDefinitionHttpV4().setInternalFlow(deployedInternalFlow);
        validator.validateDeployable(api);
    }

    private void givenApiRunning(Phase phase, String guardianId) {
        var step = List.of(GuardianSteps.of(guardianId, "Credential leakage"));
        deployedInternalFlow = phase == Phase.REQUEST ? Flow.builder().request(step).build() : Flow.builder().response(step).build();
    }

    private void givenDeployedGuardian(UnaryOperator<GuardianBuilder> definition) {
        Guardian guardian = aGuardian(definition).build();
        guardians.givenGuardian(guardian);
        publications.givenPublication(guardian.getDefinition(), DEPLOYED_AT);
    }
}
