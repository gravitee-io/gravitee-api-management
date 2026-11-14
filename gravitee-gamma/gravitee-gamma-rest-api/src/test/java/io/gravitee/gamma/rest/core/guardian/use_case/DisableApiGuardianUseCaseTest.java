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
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.domain_service.DescribeApiGuardiansDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotEnabledException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DisableApiGuardianUseCaseTest {

    private final InMemoryGuardianRepository guardians = new InMemoryGuardianRepository();
    private final GuardianAuditFixture audits = new GuardianAuditFixture();
    private final InMemoryGuardedApiPort apis = new InMemoryGuardedApiPort();
    private final AuditInfo actor = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("publisher").build())
        .build();
    private final DisableApiGuardianUseCase useCase = new DisableApiGuardianUseCase(
        apis,
        new DescribeApiGuardiansDomainService(guardians),
        audits.service
    );

    @BeforeEach
    void given_an_api_running_two_request_guardians() {
        guardians.givenGuardian(aGuardian(d -> d.id("guardian-1").name("One")).build());
        guardians.givenGuardian(aGuardian(d -> d.id("guardian-2").name("Two")).build());
        apis.givenApi(
            aGuardedApi()
                .internalFlow(
                    Flow.builder().request(List.of(GuardianSteps.of("guardian-1", "One"), GuardianSteps.of("guardian-2", "Two"))).build()
                )
                .build()
        );
    }

    @Test
    void should_remove_the_guardian_from_the_api() {
        var output = useCase.execute(new DisableApiGuardianUseCase.Input(actor, API_ID, "guardian-1"));

        assertSoftly(softly -> {
            softly
                .assertThat(output.guardians())
                .as("guardians enabled on the API")
                .containsExactly(new ApiGuardian("guardian-2", "Two", Phase.REQUEST, 1));
            softly
                .assertThat(apis.findById(ENVIRONMENT_ID, API_ID).orElseThrow().internalFlow().getRequest())
                .as("stored request steps")
                .containsExactly(GuardianSteps.of("guardian-2", "Two"));
            softly
                .assertThat(audits.storage())
                .as("audits")
                .singleElement()
                .satisfies(audit -> {
                    assertThat(audit.getEvent()).as("event").isEqualTo(ApiAuditEvent.API_UPDATED.name());
                    assertThat(audit.getProperties()).as("guardian reference").containsEntry("GUARDIAN", "guardian-1");
                });
        });
    }

    @Test
    void should_refuse_a_guardian_that_is_not_enabled_on_the_api() {
        assertThatThrownBy(() -> useCase.execute(new DisableApiGuardianUseCase.Input(actor, API_ID, "guardian-3")))
            .as("guardian not enabled")
            .isInstanceOf(GuardianNotEnabledException.class);
    }

    @Test
    void should_refuse_an_api_of_another_environment() {
        var otherEnvironment = AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId("other-env").build();

        assertThatThrownBy(() -> useCase.execute(new DisableApiGuardianUseCase.Input(otherEnvironment, API_ID, "guardian-1")))
            .as("API of another environment")
            .isInstanceOf(ApiNotFoundException.class);
    }
}
