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
package io.gravitee.apim.core.subscription.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.subscription.domain_service.SubscriptionCRDDomainService;
import io.gravitee.apim.core.subscription.domain_service.ValidateSubscriptionCRDDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.apim.core.validation.Validator;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
class ImportSubscriptionCRDUseCaseTest {

    private static final String ORGANIZATION_ID = "org-id";
    private static final String ENVIRONMENT_ID = "env-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, "user-id");
    private static final String SUBSCRIPTION_ID = "subscription-id";

    private static final SubscriptionCRDSpec SPEC = SubscriptionCRDSpec.builder()
        .id(SUBSCRIPTION_ID)
        .referenceId("api-id")
        .referenceType(SubscriptionReferenceType.API)
        .applicationId("application-id")
        .planId("plan-id")
        .customApiKey("custom-key")
        .build();

    private final SubscriptionCRDDomainService subscriptionCRDDomainService = mock(SubscriptionCRDDomainService.class);
    private final ValidateSubscriptionCRDDomainService validateSubscriptionCRDDomainService = mock(
        ValidateSubscriptionCRDDomainService.class
    );

    private final ImportSubscriptionCRDUseCase cut = new ImportSubscriptionCRDUseCase(
        subscriptionCRDDomainService,
        validateSubscriptionCRDDomainService
    );

    @Test
    void should_import_with_sanitized_input() {
        var sanitizedSpec = SPEC.toBuilder().customApiKey("another-custom-key").build();
        when(validateSubscriptionCRDDomainService.validateAndSanitize(any(ValidateSubscriptionCRDDomainService.Input.class))).thenReturn(
            Validator.Result.ofValue(new ValidateSubscriptionCRDDomainService.Input(AUDIT_INFO, sanitizedSpec))
        );
        when(subscriptionCRDDomainService.createOrUpdate(AUDIT_INFO, sanitizedSpec)).thenReturn(
            SubscriptionEntity.builder()
                .id(SUBSCRIPTION_ID)
                .status(SubscriptionEntity.Status.ACCEPTED)
                .startingAt(ZonedDateTime.now())
                .endingAt(ZonedDateTime.now().plusDays(30))
                .build()
        );

        var output = cut.execute(new ImportSubscriptionCRDUseCase.Input(AUDIT_INFO, SPEC));

        verify(subscriptionCRDDomainService).createOrUpdate(AUDIT_INFO, sanitizedSpec);
        assertThat(output.status().getStatus()).isEqualTo(SubscriptionEntity.Status.ACCEPTED.name());
    }

    @Test
    void should_reject_import_when_validation_contains_severe_errors() {
        when(validateSubscriptionCRDDomainService.validateAndSanitize(any(ValidateSubscriptionCRDDomainService.Input.class))).thenReturn(
            Validator.Result.ofErrors(List.of(Validator.Error.severe("customApiKey requires an API_KEY plan")))
        );

        assertThatThrownBy(() -> cut.execute(new ImportSubscriptionCRDUseCase.Input(AUDIT_INFO, SPEC)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("Unable to import because of errors")
            .hasMessageContaining("customApiKey requires an API_KEY plan");
    }
}
