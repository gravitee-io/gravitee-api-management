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
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ConflictDomainException;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianUsageDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeleteGuardianUseCaseTest {

    private final InMemoryGuardianRepository guardianRepository = new InMemoryGuardianRepository();
    private final GuardianPublicationFixture publications = new GuardianPublicationFixture();
    private final GuardianAuditFixture audits = new GuardianAuditFixture();
    private final GuardianDeploymentDomainService deploymentService = publications.service(guardianRepository);
    private final DeployGuardianUseCase deploy = new DeployGuardianUseCase(guardianRepository, deploymentService, audits.service);
    private final UndeployGuardianUseCase undeploy = new UndeployGuardianUseCase(
        guardianRepository,
        deploymentService,
        audits.service,
        new GuardianUsageDomainService(new InMemoryGuardedApiPort())
    );
    private final DeleteGuardianUseCase useCase = new DeleteGuardianUseCase(
        guardianRepository,
        audits.service,
        deploymentService,
        new GuardianUsageDomainService(new InMemoryGuardedApiPort())
    );
    private final AuditInfo actor = auditInfo(ENVIRONMENT_ID);

    @BeforeEach
    void setUp() {
        guardianRepository.givenGuardian(aGuardian().build());
    }

    private void delete(String environmentId, String guardianId) {
        useCase.execute(DeleteGuardianUseCase.Input.builder().auditInfo(auditInfo(environmentId)).guardianId(guardianId).build());
    }

    @Test
    void should_delete_the_guardian() {
        delete(ENVIRONMENT_ID, GUARDIAN_ID);

        assertThat(guardianRepository.storedGuardians()).isEmpty();
    }

    @Test
    void should_record_the_deletion_in_the_audit_trail() {
        delete(ENVIRONMENT_ID, GUARDIAN_ID);

        assertThat(audits.storage())
            .singleElement()
            .satisfies(audit ->
                assertSoftly(softly -> {
                    softly.assertThat(audit.getEvent()).as("deletion event").isEqualTo(GuardianAuditEvent.GUARDIAN_DELETED.name());
                    softly.assertThat(audit.getProperties()).as("guardian reference").containsEntry("GUARDIAN", GUARDIAN_ID);
                })
            );
    }

    @Test
    void should_refuse_to_delete_a_guardian_of_another_environment() {
        assertThatThrownBy(() -> delete("other-env", GUARDIAN_ID)).isInstanceOf(GuardianNotFoundException.class);

        assertThat(guardianRepository.storedGuardians()).hasSize(1);
        assertThat(audits.storage()).as("no audit recorded").isEmpty();
    }

    @Test
    void should_refuse_deletion_even_when_the_first_deployment_stamp_was_not_saved() {
        var failingRepository = new InMemoryGuardianRepository() {
            @Override
            public void updateUpdatedAt(String environmentId, String guardianId, Instant updatedAt) {
                throw new TechnicalDomainException("Storage unavailable");
            }
        };
        failingRepository.givenGuardian(aGuardian().build());
        var service = publications.service(failingRepository);
        var failingDeploy = new DeployGuardianUseCase(failingRepository, service, audits.service);
        assertThatThrownBy(() -> failingDeploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null))).isInstanceOf(
            TechnicalDomainException.class
        );

        assertThatThrownBy(() -> useCase.execute(new DeleteGuardianUseCase.Input(actor, GUARDIAN_ID)))
            .isInstanceOf(ConflictDomainException.class)
            .hasMessageContaining("active publication");

        assertSoftly(softly -> {
            softly.assertThat(publications.withdrawn(ENVIRONMENT_ID, GUARDIAN_ID)).as("publication preserved").isFalse();
            softly.assertThat(guardianRepository.storedGuardians()).as("draft preserved").hasSize(1);
            softly
                .assertThat(audits.storage())
                .as("only the deployment is audited")
                .extracting(AuditEntity::getEvent)
                .containsExactly(GuardianAuditEvent.GUARDIAN_DEPLOYED.name());
        });
    }

    @Test
    void should_delete_an_unpublished_draft_without_withdrawing_it() {
        publications.failWithdrawal = true;

        useCase.execute(new DeleteGuardianUseCase.Input(actor, GUARDIAN_ID));

        assertSoftly(softly -> {
            softly.assertThat(guardianRepository.storedGuardians()).as("draft deleted").isEmpty();
            softly.assertThat(publications.find(ENVIRONMENT_ID, GUARDIAN_ID)).as("no withdrawal written").isEmpty();
        });
    }

    @Test
    void should_refuse_deletion_of_a_deployed_guardian() {
        publications.givenPublication(aGuardianDefinition().build(), Instant.parse("2026-11-09T10:00:00Z"));
        assertActivePublicationPreventsDeletion();
    }

    @Test
    void should_refuse_deletion_of_a_pending_guardian() {
        publications.givenPublication(aGuardianDefinition().name("Previous revision").build(), Instant.parse("2026-11-09T10:00:00Z"));
        assertActivePublicationPreventsDeletion();
    }

    @Test
    void should_delete_a_draft_whose_publication_was_already_removed() {
        var guardian = guardianRepository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow();
        deploymentService.withdraw(
            guardian,
            new GuardianDeployment.Published(guardian.getDefinition(), Instant.parse("2026-11-08T10:00:00Z")),
            actor
        );
        var publication = publications.find(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow();

        useCase.execute(new DeleteGuardianUseCase.Input(actor, GUARDIAN_ID));

        assertSoftly(softly -> {
            softly.assertThat(guardianRepository.storedGuardians()).as("draft deleted").isEmpty();
            softly.assertThat(publications.find(ENVIRONMENT_ID, GUARDIAN_ID)).as("withdrawal preserved").contains(publication);
        });
    }

    @Test
    void should_allow_deletion_after_undeployment() {
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        undeploy.execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID));

        useCase.execute(new DeleteGuardianUseCase.Input(actor, GUARDIAN_ID));

        assertThat(guardianRepository.storedGuardians()).as("withdrawn guardian can be deleted").isEmpty();
    }

    private void assertActivePublicationPreventsDeletion() {
        var before = guardianRepository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow();
        var publication = publications.find(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow();

        assertThatThrownBy(() -> useCase.execute(new DeleteGuardianUseCase.Input(actor, GUARDIAN_ID)))
            .isInstanceOf(ConflictDomainException.class)
            .hasMessageContaining("active publication");
        assertSoftly(softly -> {
            softly.assertThat(guardianRepository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID)).as("draft preserved").contains(before);
            softly.assertThat(publications.find(ENVIRONMENT_ID, GUARDIAN_ID)).as("publication preserved").contains(publication);
        });
        assertThat(audits.storage()).as("no audit recorded").isEmpty();
    }

    private static AuditInfo auditInfo(String environmentId) {
        return AuditInfo.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(environmentId)
            .actor(AuditActor.builder().userId("someone").build())
            .build();
    }
}
