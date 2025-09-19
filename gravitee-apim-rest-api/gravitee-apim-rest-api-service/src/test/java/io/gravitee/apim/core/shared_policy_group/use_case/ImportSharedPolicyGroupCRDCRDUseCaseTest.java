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
package io.gravitee.apim.core.shared_policy_group.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import fakes.FakePolicyValidationDomainService;
import fixtures.core.model.SharedPolicyGroupFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.SharedPolicyGroupCrudServiceInMemory;
import inmemory.SharedPolicyGroupHistoryCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plugin.model.FlowPhase;
import io.gravitee.apim.core.shared_policy_group.domain_service.ValidateCreateSharedPolicyGroupDomainService;
import io.gravitee.apim.core.shared_policy_group.domain_service.ValidateSharedPolicyGroupCRDDomainService;
import io.gravitee.apim.core.shared_policy_group.domain_service.ValidateUpdateSharedPolicyGroupDomainService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
class ImportSharedPolicyGroupCRDCRDUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private final String ORG_ID = "org-id";
    private final String ENV_ID = "env-id";
    private final String USER_ID = "user-id";
    private final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORG_ID)
        .environmentId(ENV_ID)
        .actor(AuditActor.builder().userId(USER_ID).build())
        .build();

    private final SharedPolicyGroupCrudServiceInMemory sharedPolicyGroupCrudService = new SharedPolicyGroupCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final FakePolicyValidationDomainService policyValidationDomainService = new FakePolicyValidationDomainService();
    private final ValidateCreateSharedPolicyGroupDomainService validateCreateSharedPolicyGroupDomainService =
        new ValidateCreateSharedPolicyGroupDomainService(sharedPolicyGroupCrudService, policyValidationDomainService);
    private final ValidateUpdateSharedPolicyGroupDomainService validateUpdateSharedPolicyGroupDomainService =
        new ValidateUpdateSharedPolicyGroupDomainService(sharedPolicyGroupCrudService, policyValidationDomainService);
    private final AuditDomainService auditService = new AuditDomainService(
        auditCrudService,
        userCrudService,
        new JacksonJsonDiffProcessor()
    );
    private final CreateSharedPolicyGroupUseCase createSharedPolicyGroupUseCase = new CreateSharedPolicyGroupUseCase(
        sharedPolicyGroupCrudService,
        validateCreateSharedPolicyGroupDomainService,
        auditService
    );
    private final UpdateSharedPolicyGroupUseCase updateSharedPolicyGroupUseCase = new UpdateSharedPolicyGroupUseCase(
        sharedPolicyGroupCrudService,
        validateUpdateSharedPolicyGroupDomainService,
        auditService
    );

    private final EventCrudInMemory eventCrudInMemory = new EventCrudInMemory();
    private final EventLatestCrudInMemory eventLatestCrudInMemory = new EventLatestCrudInMemory();
    private final SharedPolicyGroupHistoryCrudServiceInMemory sharedPolicyGroupHistoryCrudService =
        new SharedPolicyGroupHistoryCrudServiceInMemory();

    private final DeploySharedPolicyGroupUseCase deploySharedPolicyGroupUseCase = new DeploySharedPolicyGroupUseCase(
        eventCrudInMemory,
        eventLatestCrudInMemory,
        sharedPolicyGroupCrudService,
        sharedPolicyGroupHistoryCrudService,
        auditService
    );

    private final ValidateSharedPolicyGroupCRDDomainService validateSharedPolicyGroupCRDDomainService =
        new ValidateSharedPolicyGroupCRDDomainService(
            sharedPolicyGroupCrudService,
            validateCreateSharedPolicyGroupDomainService,
            validateUpdateSharedPolicyGroupDomainService
        );

    private ImportSharedPolicyGroupCRDCRDUseCase cut;

    @BeforeEach
    void setUp() {
        sharedPolicyGroupCrudService.reset();
        cut = new ImportSharedPolicyGroupCRDCRDUseCase(
            createSharedPolicyGroupUseCase,
            updateSharedPolicyGroupUseCase,
            deploySharedPolicyGroupUseCase,
            validateSharedPolicyGroupCRDDomainService,
            sharedPolicyGroupCrudService
        );
    }

    @Nested
    class Create {

        @Test
        void should_create_and_deploy() {
            // Given
            var crd = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();

            // When
            var output = cut.execute(new ImportSharedPolicyGroupCRDCRDUseCase.Input(AUDIT_INFO, crd));

            // Then
            assertThat(output.status()).isNotNull();
            assertThat(output.status().getCrossId()).isNotNull();
            assertThat(output.status().getEnvironmentId()).isEqualTo(ENV_ID);
            assertThat(output.status().getOrganizationId()).isEqualTo(ORG_ID);

            SharedPolicyGroup spg = sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(ENV_ID, crd.getCrossId()).get();
            assertThat(spg).isNotNull();
            assertThat(spg.getCreatedAt()).isNotNull();
            assertThat(spg.getDeployedAt()).isNotNull();
            assertThat(spg.getLifecycleState()).isEqualTo(SharedPolicyGroup.SharedPolicyGroupLifecycleState.DEPLOYED);
        }

        @Test
        void should_not_create_when_policy_validation_fails() {
            // Given
            var crd = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();
            crd.setSteps(
                List.of(Step.builder().policy("policy_throw_invalid_data_exception").configuration("{ \"key\": \"value\" }").build())
            );

            // When
            var throwable = Assertions.catchThrowable(() -> cut.execute(new ImportSharedPolicyGroupCRDCRDUseCase.Input(AUDIT_INFO, crd)));

            // Then
            Assertions.assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Unable to import because of errors [Invalid configuration for policy policy_throw_invalid_data_exception]");
        }

        @Test
        void should_not_create_when_policy_execution_phase_validation_fails() {
            // Given
            var crd = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();
            crd.setSteps(
                List.of(Step.builder().policy("policy_throw_unexpected_policy_exception").configuration("{ \"key\": \"value\" }").build())
            );

            // When
            var throwable = Assertions.catchThrowable(() -> cut.execute(new ImportSharedPolicyGroupCRDCRDUseCase.Input(AUDIT_INFO, crd)));

            // Then
            Assertions.assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage(
                    "Unable to import because of errors [Unexpected policies [policy_throw_unexpected_policy_exception] for API type PROXY and phase REQUEST]"
                );
        }

        @Test
        void should_not_create_when_phase_is_null() {
            // Given
            var crd = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();
            crd.setPhase(null);

            // When
            var throwable = Assertions.catchThrowable(() -> cut.execute(new ImportSharedPolicyGroupCRDCRDUseCase.Input(AUDIT_INFO, crd)));

            // Then
            Assertions.assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Unable to import because of errors [Phase is required.]");
        }

        @Test
        void should_not_create_when_phase_is_invalid() {
            // Given
            var crd = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();
            crd.setApiType(ApiType.PROXY);
            crd.setPhase(FlowPhase.SUBSCRIBE);

            // When
            var throwable = Assertions.catchThrowable(() -> cut.execute(new ImportSharedPolicyGroupCRDCRDUseCase.Input(AUDIT_INFO, crd)));

            // Then
            Assertions.assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Unable to import because of errors [Invalid phase SUBSCRIBE for API type PROXY]");
        }
    }

    @Nested
    class Update {

        @Test
        void should_update_and_deploy() {
            // Given
            var crd = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();
            crd.setSharedPolicyGroupId(UUID.randomUUID().toString());
            SharedPolicyGroup sharedPolicyGroup = crd.toSharedPolicyGroup();
            sharedPolicyGroup.setEnvironmentId(ENV_ID);
            sharedPolicyGroup.setHrid(sharedPolicyGroup.getCrossId());

            sharedPolicyGroupCrudService.create(sharedPolicyGroup);

            // When
            var output = cut.execute(new ImportSharedPolicyGroupCRDCRDUseCase.Input(AUDIT_INFO, crd));

            // Then
            assertThat(output.status()).isNotNull();
            assertThat(output.status().getCrossId()).isNotNull();
            assertThat(output.status().getEnvironmentId()).isEqualTo(ENV_ID);
            assertThat(output.status().getOrganizationId()).isEqualTo(ORG_ID);

            SharedPolicyGroup spg = sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(ENV_ID, crd.getCrossId()).get();
            assertThat(spg).isNotNull();
            assertThat(spg.getDeployedAt()).isNotNull();
            assertThat(spg.getLifecycleState()).isEqualTo(SharedPolicyGroup.SharedPolicyGroupLifecycleState.DEPLOYED);
        }

        @Test
        void should_throw_exception_when_policy_validation_fails() {
            // Given
            var crd = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();
            sharedPolicyGroupCrudService.initWith(List.of(crd.toSharedPolicyGroup()));

            // When
            crd.setSteps(
                List.of(Step.builder().policy("policy_throw_invalid_data_exception").configuration("{ \"key\": \"value\" }").build())
            );
            var throwable = Assertions.catchThrowable(() -> cut.execute(new ImportSharedPolicyGroupCRDCRDUseCase.Input(AUDIT_INFO, crd)));

            // Then
            Assertions.assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Unable to import because of errors [Invalid configuration for policy policy_throw_invalid_data_exception]");
        }

        @Test
        void should_throw_exception_when_policy_execution_phase_validation_fails() {
            // Given
            var crd = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();
            sharedPolicyGroupCrudService.initWith(List.of(crd.toSharedPolicyGroup()));

            // When
            crd.setSteps(
                List.of(Step.builder().policy("policy_throw_unexpected_policy_exception").configuration("{ \"key\": \"value\" }").build())
            );
            var throwable = Assertions.catchThrowable(() -> cut.execute(new ImportSharedPolicyGroupCRDCRDUseCase.Input(AUDIT_INFO, crd)));

            // Then
            Assertions.assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage(
                    "Unable to import because of errors [Unexpected policies [policy_throw_unexpected_policy_exception] for API type PROXY and phase REQUEST]"
                );
        }
    }
}
