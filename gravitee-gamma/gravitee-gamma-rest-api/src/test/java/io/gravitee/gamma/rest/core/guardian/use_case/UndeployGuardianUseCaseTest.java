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
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UndeployGuardianUseCaseTest {

    private static final Instant DEPLOYED_AT = Instant.parse("2026-11-09T10:00:00Z");
    private static final Instant WITHDRAWN_AT = DEPLOYED_AT.plusSeconds(60);
    private final InMemoryGuardianRepository repository = new InMemoryGuardianRepository();
    private final GuardianPublicationFixture publications = new GuardianPublicationFixture();
    private final GuardianAuditFixture audits = new GuardianAuditFixture();
    private final AuditInfo actor = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("original-user").build())
        .build();

    @BeforeEach
    void given_a_deployed_guardian() {
        TimeProvider.overrideClock(Clock.fixed(WITHDRAWN_AT, ZoneOffset.UTC));
        repository.givenGuardian(aGuardian().lifecycleState(GuardianLifecycleState.DEPLOYED).deployedAt(DEPLOYED_AT).build());
        publications.givenPublication(aGuardianDefinition().build(), DEPLOYED_AT);
    }

    @AfterEach
    void restore_clock() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    private UndeployGuardianUseCase useCase(InMemoryGuardianRepository store, GuardianPublicationFixture events) {
        return new UndeployGuardianUseCase(store, events.service(store), audits.service);
    }

    @Test
    void should_preserve_a_draft_saved_while_the_withdrawal_is_published() {
        var newer = aGuardian(d -> d.name("Concurrent draft"))
            .description("Concurrent description")
            .build();
        publications.beforeWithdrawal = () -> repository.update(newer);

        var result = useCase(repository, publications).execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID)).guardian();

        assertSoftly(softly -> {
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID).map(Guardian::getDefinition))
                .as("concurrent draft remains stored")
                .contains(newer.getDefinition());
            softly.assertThat(result.getDefinition()).as("response reads current draft").isEqualTo(newer.getDefinition());
            softly.assertThat(result.getDescription()).as("response preserves current description").isEqualTo(newer.getDescription());
            softly.assertThat(result.getLifecycleState()).as("withdrawal state").isEqualTo(GuardianLifecycleState.UNDEPLOYED);
        });
    }

    @Test
    void should_not_replace_the_guardian_when_undeploying() {
        var readOnly = new InMemoryGuardianRepository() {
            @Override
            public Guardian update(Guardian guardian) {
                throw new TechnicalDomainException("Guardian writes unavailable");
            }
        };
        readOnly.givenGuardian(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow());

        var result = useCase(readOnly, publications).execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID)).guardian();

        assertSoftly(softly -> {
            softly
                .assertThat(result.getLifecycleState())
                .as("withdrawal does not depend on a draft write")
                .isEqualTo(GuardianLifecycleState.UNDEPLOYED);
            softly.assertThat(result.getDeployedAt()).as("last deployment retained").isEqualTo(DEPLOYED_AT);
            softly.assertThat(result.getUpdatedAt()).as("withdrawal date derived from event").isEqualTo(WITHDRAWN_AT);
            softly.assertThat(audits.storage()).as("withdrawal audited").hasSize(1);
        });
    }

    @Test
    void should_record_the_withdrawal_in_the_audit_trail() {
        var deployed = repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow();

        var result = useCase(repository, publications).execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID)).guardian();

        assertThat(audits.storage())
            .as("one withdrawal audit")
            .singleElement()
            .satisfies(audit ->
                assertSoftly(softly -> {
                    softly.assertThat(audit.getEvent()).as("withdrawal event").isEqualTo(GuardianAuditEvent.GUARDIAN_UNDEPLOYED.name());
                    softly.assertThat(audit.getUser()).as("actor").isEqualTo(actor.actor().userId());
                    softly.assertThat(audit.getOrganizationId()).as("organization").isEqualTo(ORGANIZATION_ID);
                    softly.assertThat(audit.getEnvironmentId()).as("environment").isEqualTo(ENVIRONMENT_ID);
                    softly.assertThat(audit.getReferenceId()).as("environment reference").isEqualTo(ENVIRONMENT_ID);
                    softly.assertThat(audit.getProperties()).as("guardian reference").containsEntry("GUARDIAN", GUARDIAN_ID);
                    softly.assertThat(audit.getCreatedAt().toInstant()).as("withdrawal date").isEqualTo(WITHDRAWN_AT);
                    softly
                        .assertThat(audit.getPatch())
                        .as("deployed to withdrawn state patch")
                        .isEqualTo(new JacksonJsonDiffProcessor().diff(deployed, result));
                })
            );
    }

    @Test
    void should_audit_only_the_call_that_withdraws_the_guardian() {
        useCase(repository, publications).execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID));
        publications.failWithdrawal = true;

        useCase(repository, publications).execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID));

        assertThat(audits.storage()).as("repeated undeployment is not audited again").hasSize(1);
    }

    @Test
    void should_withdraw_the_guardian_even_when_its_audit_cannot_be_recorded() {
        audits.failRecording = true;

        var result = useCase(repository, publications).execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID)).guardian();

        assertSoftly(softly -> {
            softly.assertThat(result.getLifecycleState()).as("withdrawn state").isEqualTo(GuardianLifecycleState.UNDEPLOYED);
            softly.assertThat(publications.withdrawn(ENVIRONMENT_ID, GUARDIAN_ID)).as("withdrawal published").isTrue();
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow().getUpdatedAt())
                .as("withdrawal completed")
                .isEqualTo(WITHDRAWN_AT);
            softly.assertThat(audits.storage()).as("best-effort audit").isEmpty();
        });
    }

    @Test
    void should_recover_the_sorting_timestamp_after_a_metadata_write_failure_without_duplicating_audit() {
        var failing = new InMemoryGuardianRepository() {
            boolean fail = true;

            @Override
            public void updateUpdatedAt(String environmentId, String guardianId, Instant date) {
                if (fail) {
                    throw new TechnicalDomainException("Metadata unavailable");
                }
                super.updateUpdatedAt(environmentId, guardianId, date);
            }
        };
        failing.givenGuardian(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow());
        assertThatThrownBy(() ->
            useCase(failing, publications).execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID))
        ).isInstanceOf(TechnicalDomainException.class);
        failing.fail = false;
        publications.failWithdrawal = true;

        useCase(failing, publications).execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID));

        assertSoftly(softly -> {
            softly.assertThat(audits.storage()).as("one audit despite retrying metadata").hasSize(1);
            softly
                .assertThat(failing.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow().getUpdatedAt())
                .as("withdrawal timestamp persisted for sorting")
                .isEqualTo(WITHDRAWN_AT);
        });
    }
}
