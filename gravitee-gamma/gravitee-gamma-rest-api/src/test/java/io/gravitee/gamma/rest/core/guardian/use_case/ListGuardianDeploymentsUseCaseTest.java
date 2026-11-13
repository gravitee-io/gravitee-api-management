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
import static org.assertj.core.groups.Tuple.tuple;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
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
class ListGuardianDeploymentsUseCaseTest {

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
    private final ListGuardianDeploymentsUseCase list = new ListGuardianDeploymentsUseCase(repository, publications.store());
    private final AuditInfo deployer = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("deployer").build())
        .build();

    @BeforeEach
    void given_an_undeployed_guardian() {
        repository.givenGuardian(aGuardian().build());
        publications.users.initWith(List.of(BaseUserEntity.builder().id("deployer").firstname("Ada").lastname("Lovelace").build()));
    }

    @AfterEach
    void restore_clock() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_list_the_deployed_revisions_newest_first_with_their_date_deployer_and_label() {
        deployAt("2026-11-09T10:00:00Z", GUARDIAN_ID, "First");
        deployAt("2026-11-10T10:00:00Z", GUARDIAN_ID, null);

        var output = list.execute(new ListGuardianDeploymentsUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, 1, 10));

        assertSoftly(softly -> {
            softly
                .assertThat(output.deployments().getContent())
                .as("revisions, newest first")
                .extracting(d -> d.label(), d -> d.deployedAt())
                .containsExactly(tuple(null, Instant.parse("2026-11-10T10:00:00Z")), tuple("First", Instant.parse("2026-11-09T10:00:00Z")));
            softly
                .assertThat(output.deployments().getContent())
                .as("deployer of each revision")
                .extracting(d -> d.deployer().displayName())
                .containsOnly("Ada Lovelace");
            softly
                .assertThat(output.deployments().getContent())
                .as("revision ids")
                .extracting(d -> d.id())
                .doesNotContainNull();
            softly.assertThat(output.deployments().getTotalElements()).as("total").isEqualTo(2);
        });
    }

    @Test
    void should_leave_withdrawals_out() {
        deployAt("2026-11-09T10:00:00Z", GUARDIAN_ID, null);
        TimeProvider.overrideClock(Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC));
        undeploy.execute(new UndeployGuardianUseCase.Input(deployer, GUARDIAN_ID));
        deployAt("2026-11-11T10:00:00Z", GUARDIAN_ID, null);

        var output = list.execute(new ListGuardianDeploymentsUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, 1, 10));

        assertThat(output.deployments().getContent())
            .as("deployed revisions only")
            .extracting(d -> d.deployedAt())
            .containsExactly(Instant.parse("2026-11-11T10:00:00Z"), Instant.parse("2026-11-09T10:00:00Z"));
    }

    @Test
    void should_keep_a_deployer_the_user_store_no_longer_knows_by_id() {
        publications.users.reset();
        deployAt("2026-11-09T10:00:00Z", GUARDIAN_ID, null);

        var output = list.execute(new ListGuardianDeploymentsUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, 1, 10));

        assertThat(output.deployments().getContent())
            .singleElement()
            .extracting(d -> d.deployer().getId())
            .isEqualTo("deployer");
    }

    @Test
    void should_not_list_the_deployments_of_another_guardian() {
        repository.givenGuardian(aGuardian(definition -> definition.id(OTHER_GUARDIAN_ID).name("Other")).build());
        deployAt("2026-11-09T10:00:00Z", OTHER_GUARDIAN_ID, "Other");
        deployAt("2026-11-10T10:00:00Z", GUARDIAN_ID, "Mine");

        var output = list.execute(new ListGuardianDeploymentsUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, 1, 10));

        assertThat(output.deployments().getContent())
            .as("own revisions only")
            .extracting(d -> d.label())
            .containsExactly("Mine");
    }

    @Test
    void should_refuse_a_guardian_unknown_in_the_environment() {
        deployAt("2026-11-09T10:00:00Z", GUARDIAN_ID, null);

        assertThatThrownBy(() ->
            list.execute(new ListGuardianDeploymentsUseCase.Input("another-environment", GUARDIAN_ID, 1, 10))
        ).isInstanceOf(GuardianNotFoundException.class);
    }

    @Test
    void should_paginate_the_revisions() {
        deployAt("2026-11-09T10:00:00Z", GUARDIAN_ID, null);
        deployAt("2026-11-10T10:00:00Z", GUARDIAN_ID, null);
        deployAt("2026-11-11T10:00:00Z", GUARDIAN_ID, null);

        var output = list.execute(new ListGuardianDeploymentsUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, 2, 2));

        assertSoftly(softly -> {
            softly
                .assertThat(output.deployments().getContent())
                .as("second page")
                .extracting(d -> d.deployedAt())
                .containsExactly(Instant.parse("2026-11-09T10:00:00Z"));
            softly.assertThat(output.deployments().getTotalElements()).as("total").isEqualTo(3);
            softly.assertThat(output.deployments().getPageNumber()).as("page").isEqualTo(2);
            softly.assertThat(output.perPage()).as("page size served").isEqualTo(2);
        });
    }

    @Test
    void should_serve_a_default_page_when_none_is_asked_and_cap_an_oversized_one() {
        deployAt("2026-11-09T10:00:00Z", GUARDIAN_ID, null);

        var byDefault = list.execute(new ListGuardianDeploymentsUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, 0, 0));
        var oversized = list.execute(new ListGuardianDeploymentsUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID, 1, 1000));

        assertSoftly(softly -> {
            softly.assertThat(byDefault.deployments().getPageNumber()).as("default page").isEqualTo(1);
            softly.assertThat(byDefault.perPage()).as("default page size").isEqualTo(10);
            softly.assertThat(oversized.perPage()).as("capped page size").isEqualTo(100);
        });
    }

    private void deployAt(String instant, String guardianId, String label) {
        TimeProvider.overrideClock(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
        deploy.execute(new DeployGuardianUseCase.Input(deployer, guardianId, label));
    }
}
