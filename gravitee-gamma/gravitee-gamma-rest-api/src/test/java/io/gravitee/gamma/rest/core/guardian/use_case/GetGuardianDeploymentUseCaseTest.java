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
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aDeployGuardianUseCase;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianUsage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianDeploymentNotFoundException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.rest.api.model.EventType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetGuardianDeploymentUseCaseTest {

    private static final String OTHER_GUARDIAN_ID = "8c0e4d6b-2f71-4a9e-b5d3-1e6f9a2c7b85";

    private final InMemoryGuardianRepository repository = new InMemoryGuardianRepository();
    private final GuardianPublicationFixture publications = new GuardianPublicationFixture();
    private final GuardianAuditFixture audits = new GuardianAuditFixture();
    private final GuardianDeploymentDomainService deploymentService = publications.service(repository);
    private final DeployGuardianUseCase deploy = aDeployGuardianUseCase(repository, deploymentService, audits.service);
    private final UndeployGuardianUseCase undeploy = new UndeployGuardianUseCase(
        repository,
        deploymentService,
        audits.service,
        aGuardianUsage()
    );
    private final GetGuardianDeploymentUseCase get = new GetGuardianDeploymentUseCase(repository, publications.store());
    private final AuditInfo deployer = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("deployer").build())
        .build();

    @BeforeEach
    void given_an_undeployed_guardian() {
        repository.givenGuardian(aGuardian(definition -> definition.name("First name")).build());
        publications.users.initWith(List.of(BaseUserEntity.builder().id("deployer").firstname("Ada").lastname("Lovelace").build()));
    }

    @AfterEach
    void restore_clock() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_return_the_definition_deployed_by_the_requested_revision_rather_than_the_latest() {
        var first = deployAt("2026-11-09T10:00:00Z", GUARDIAN_ID, "First");
        repository.reset();
        repository.givenGuardian(aGuardian(definition -> definition.name("Second name")).build());
        deployAt("2026-11-10T10:00:00Z", GUARDIAN_ID, null);

        var output = get.execute(new GetGuardianDeploymentUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, first));

        var revision = output.deployment().revision();
        var definition = output.deployment().definition();
        assertSoftly(softly -> {
            softly.assertThat(revision.id()).as("revision id").isEqualTo(first);
            softly.assertThat(revision.label()).as("label").isEqualTo("First");
            softly.assertThat(revision.deployedAt()).as("deployment date").isEqualTo(Instant.parse("2026-11-09T10:00:00Z"));
            softly.assertThat(definition.getName()).as("deployed name").isEqualTo("First name");
            softly.assertThat(definition.getId()).as("deployed id").isEqualTo(GUARDIAN_ID);
            softly.assertThat(revision.deployer().displayName()).as("deployer").isEqualTo("Ada Lovelace");
        });
    }

    @Test
    void should_keep_a_deployer_the_user_store_no_longer_knows_by_id() {
        publications.users.reset();
        var revision = deployAt("2026-11-09T10:00:00Z", GUARDIAN_ID, null);

        var output = get.execute(new GetGuardianDeploymentUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, revision));

        assertThat(output.deployment().revision().deployer())
            .as("deployer kept by id")
            .extracting(BaseUserEntity::getId)
            .isEqualTo("deployer");
    }

    @Test
    void should_not_return_the_revision_of_another_guardian() {
        repository.givenGuardian(aGuardian(definition -> definition.id(OTHER_GUARDIAN_ID).name("Other")).build());
        var other = deployAt("2026-11-09T10:00:00Z", OTHER_GUARDIAN_ID, null);

        assertThatThrownBy(() -> get.execute(new GetGuardianDeploymentUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, other))).isInstanceOf(
            GuardianDeploymentNotFoundException.class
        );
    }

    @Test
    void should_not_return_a_withdrawal() {
        deployAt("2026-11-09T10:00:00Z", GUARDIAN_ID, null);
        TimeProvider.overrideClock(Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC));
        undeploy.execute(new UndeployGuardianUseCase.Input(deployer, GUARDIAN_ID));
        var withdrawal = publications.history
            .storage()
            .stream()
            .filter(event -> event.getType() == EventType.UNPUBLISH_GENERIC_EVENT)
            .map(Event::getId)
            .findFirst()
            .orElseThrow();

        assertThatThrownBy(() -> get.execute(new GetGuardianDeploymentUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, withdrawal))).isInstanceOf(
            GuardianDeploymentNotFoundException.class
        );
    }

    @Test
    void should_refuse_an_unknown_revision() {
        deployAt("2026-11-09T10:00:00Z", GUARDIAN_ID, null);

        assertThatThrownBy(() -> get.execute(new GetGuardianDeploymentUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, "unknown"))).isInstanceOf(
            GuardianDeploymentNotFoundException.class
        );
    }

    @Test
    void should_refuse_a_guardian_unknown_in_the_environment() {
        var revision = deployAt("2026-11-09T10:00:00Z", GUARDIAN_ID, null);

        assertThatThrownBy(() ->
            get.execute(new GetGuardianDeploymentUseCase.Input("another-environment", GUARDIAN_ID, revision))
        ).isInstanceOf(GuardianNotFoundException.class);
    }

    /** Deploys, then answers the id of the revision the deployment recorded. */
    private String deployAt(String instant, String guardianId, String label) {
        TimeProvider.overrideClock(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
        deploy.execute(new DeployGuardianUseCase.Input(deployer, guardianId, label));
        return publications.history.storage().getLast().getId();
    }
}
