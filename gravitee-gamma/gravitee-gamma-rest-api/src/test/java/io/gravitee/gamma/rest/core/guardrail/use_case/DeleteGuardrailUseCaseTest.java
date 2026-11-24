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
package io.gravitee.gamma.rest.core.guardrail.use_case;

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aDeployGuardrailUseCase;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailDefinition;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailUsage;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailUsageRunning;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ConflictDomainException;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.gamma.rest.core.guardrail.GuardrailAuditFixture;
import io.gravitee.gamma.rest.core.guardrail.GuardrailPublicationFixture;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailDeployment;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeleteGuardrailUseCaseTest {

    private final InMemoryGuardrailRepository guardrailRepository = new InMemoryGuardrailRepository();
    private final GuardrailPublicationFixture publications = new GuardrailPublicationFixture();
    private final GuardrailAuditFixture audits = new GuardrailAuditFixture();
    private final GuardrailDeploymentDomainService deploymentService = publications.service(guardrailRepository);
    private final DeployGuardrailUseCase deploy = aDeployGuardrailUseCase(guardrailRepository, deploymentService, audits.service);
    private final UndeployGuardrailUseCase undeploy = new UndeployGuardrailUseCase(
        guardrailRepository,
        deploymentService,
        audits.service,
        aGuardrailUsage()
    );
    private final DeleteGuardrailUseCase useCase = new DeleteGuardrailUseCase(
        guardrailRepository,
        audits.service,
        deploymentService,
        aGuardrailUsage()
    );
    private final AuditInfo actor = auditInfo(ENVIRONMENT_ID);

    @BeforeEach
    void setUp() {
        guardrailRepository.givenGuardrail(aGuardrail().build());
    }

    private void delete(String environmentId, String guardrailId) {
        useCase.execute(DeleteGuardrailUseCase.Input.builder().auditInfo(auditInfo(environmentId)).guardrailId(guardrailId).build());
    }

    @Test
    void should_delete_the_guardrail() {
        delete(ENVIRONMENT_ID, GUARDRAIL_ID);

        assertThat(guardrailRepository.storedGuardrails()).isEmpty();
    }

    @Test
    void should_record_the_deletion_in_the_audit_trail() {
        delete(ENVIRONMENT_ID, GUARDRAIL_ID);

        assertThat(audits.storage())
            .singleElement()
            .satisfies(audit ->
                assertSoftly(softly -> {
                    softly.assertThat(audit.getEvent()).as("deletion event").isEqualTo(GuardrailAuditEvent.GUARDRAIL_DELETED.name());
                    softly.assertThat(audit.getProperties()).as("guardrail reference").containsEntry("GUARDRAIL", GUARDRAIL_ID);
                })
            );
    }

    @Test
    void should_refuse_to_delete_a_guardrail_of_another_environment() {
        assertThatThrownBy(() -> delete("other-env", GUARDRAIL_ID)).isInstanceOf(GuardrailNotFoundException.class);

        assertThat(guardrailRepository.storedGuardrails()).hasSize(1);
        assertThat(audits.storage()).as("no audit recorded").isEmpty();
    }

    @Test
    void should_refuse_deletion_even_when_the_first_deployment_stamp_was_not_saved() {
        var failingRepository = new InMemoryGuardrailRepository() {
            @Override
            public void updateUpdatedAt(String environmentId, String guardrailId, Instant updatedAt) {
                throw new TechnicalDomainException("Storage unavailable");
            }
        };
        failingRepository.givenGuardrail(aGuardrail().build());
        var service = publications.service(failingRepository);
        var failingDeploy = aDeployGuardrailUseCase(failingRepository, service, audits.service);
        assertThatThrownBy(() -> failingDeploy.execute(new DeployGuardrailUseCase.Input(actor, GUARDRAIL_ID, null))).isInstanceOf(
            TechnicalDomainException.class
        );

        assertThatThrownBy(() -> useCase.execute(new DeleteGuardrailUseCase.Input(actor, GUARDRAIL_ID)))
            .isInstanceOf(ConflictDomainException.class)
            .hasMessageContaining("active publication");

        assertSoftly(softly -> {
            softly.assertThat(publications.withdrawn(ENVIRONMENT_ID, GUARDRAIL_ID)).as("publication preserved").isFalse();
            softly.assertThat(guardrailRepository.storedGuardrails()).as("draft preserved").hasSize(1);
            softly
                .assertThat(audits.storage())
                .as("only the deployment is audited")
                .extracting(AuditEntity::getEvent)
                .containsExactly(GuardrailAuditEvent.GUARDRAIL_DEPLOYED.name());
        });
    }

    @Test
    void should_delete_an_unpublished_draft_without_withdrawing_it() {
        publications.failWithdrawal = true;

        useCase.execute(new DeleteGuardrailUseCase.Input(actor, GUARDRAIL_ID));

        assertSoftly(softly -> {
            softly.assertThat(guardrailRepository.storedGuardrails()).as("draft deleted").isEmpty();
            softly.assertThat(publications.find(ENVIRONMENT_ID, GUARDRAIL_ID)).as("no withdrawal written").isEmpty();
        });
    }

    @Test
    void should_refuse_to_delete_a_guardrail_an_api_runs() {
        var inUse = new DeleteGuardrailUseCase(
            guardrailRepository,
            audits.service,
            deploymentService,
            aGuardrailUsageRunning(GUARDRAIL_ID)
        );

        assertThatThrownBy(() -> inUse.execute(DeleteGuardrailUseCase.Input.builder().auditInfo(actor).guardrailId(GUARDRAIL_ID).build()))
            .as("deleting a guardrail in use")
            .isInstanceOf(GuardrailInUseException.class);
        assertThat(guardrailRepository.storedGuardrails()).as("stored guardrails").hasSize(1);
    }

    @Test
    void should_refuse_deletion_of_a_deployed_guardrail() {
        publications.givenPublication(aGuardrailDefinition().build(), Instant.parse("2026-11-09T10:00:00Z"));
        assertActivePublicationPreventsDeletion();
    }

    @Test
    void should_refuse_deletion_of_a_pending_guardrail() {
        publications.givenPublication(aGuardrailDefinition().name("Previous revision").build(), Instant.parse("2026-11-09T10:00:00Z"));
        assertActivePublicationPreventsDeletion();
    }

    @Test
    void should_delete_a_draft_whose_publication_was_already_removed() {
        var guardrail = guardrailRepository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID).orElseThrow();
        deploymentService.withdraw(
            guardrail,
            new GuardrailDeployment.Published(guardrail.getDefinition(), Instant.parse("2026-11-08T10:00:00Z"), "revision-id"),
            actor
        );
        var publication = publications.find(ENVIRONMENT_ID, GUARDRAIL_ID).orElseThrow();

        useCase.execute(new DeleteGuardrailUseCase.Input(actor, GUARDRAIL_ID));

        assertSoftly(softly -> {
            softly.assertThat(guardrailRepository.storedGuardrails()).as("draft deleted").isEmpty();
            softly.assertThat(publications.find(ENVIRONMENT_ID, GUARDRAIL_ID)).as("withdrawal preserved").contains(publication);
        });
    }

    @Test
    void should_allow_deletion_after_undeployment() {
        deploy.execute(new DeployGuardrailUseCase.Input(actor, GUARDRAIL_ID, null));
        undeploy.execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID));

        useCase.execute(new DeleteGuardrailUseCase.Input(actor, GUARDRAIL_ID));

        assertThat(guardrailRepository.storedGuardrails()).as("withdrawn guardrail can be deleted").isEmpty();
    }

    private void assertActivePublicationPreventsDeletion() {
        var before = guardrailRepository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID).orElseThrow();
        var publication = publications.find(ENVIRONMENT_ID, GUARDRAIL_ID).orElseThrow();

        assertThatThrownBy(() -> useCase.execute(new DeleteGuardrailUseCase.Input(actor, GUARDRAIL_ID)))
            .isInstanceOf(ConflictDomainException.class)
            .hasMessageContaining("active publication");
        assertSoftly(softly -> {
            softly.assertThat(guardrailRepository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID)).as("draft preserved").contains(before);
            softly.assertThat(publications.find(ENVIRONMENT_ID, GUARDRAIL_ID)).as("publication preserved").contains(publication);
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
