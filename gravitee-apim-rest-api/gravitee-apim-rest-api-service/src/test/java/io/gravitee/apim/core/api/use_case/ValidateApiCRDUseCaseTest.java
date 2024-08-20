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
package io.gravitee.apim.core.api.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import fixtures.core.model.ApiCRDFixtures;
import io.gravitee.apim.core.api.domain_service.ValidateApiCRDDomainService;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateApiCRDUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().environmentId("TEST").organizationId("TEST").build();

    ValidateApiCRDDomainService validator = mock(ValidateApiCRDDomainService.class);

    ValidateApiCRDUseCase cut = new ValidateApiCRDUseCase(validator);

    @Test
    void should_return_status_and_no_errors() {
        var spec = ApiCRDFixtures.anApiCRD();

        var input = new ImportApiCRDUseCase.Input(AUDIT_INFO, spec);

        when(validator.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec)))
            .thenReturn(Validator.Result.ofValue(new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec)));

        var output = cut.execute(input);

        assertThat(output.status())
            .isEqualTo(
                ApiCRDStatus
                    .builder()
                    .id("api-id")
                    .crossId("api-cross-id")
                    .environmentId("TEST")
                    .organizationId("TEST")
                    .plan("plan-name", "plan-id")
                    .state("STARTED")
                    .errors(ApiCRDStatus.Errors.EMPTY)
                    .build()
            );
    }

    @Test
    void should_return_status_and_warnings() {
        var spec = ApiCRDFixtures.anApiCRD();

        var input = new ImportApiCRDUseCase.Input(AUDIT_INFO, spec);

        when(validator.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec)))
            .thenReturn(
                Validator.Result.ofBoth(
                    new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec),
                    List.of(Validator.Error.warning("something went wrong but it's OK"))
                )
            );

        var output = cut.execute(input);

        assertThat(output.status())
            .isEqualTo(
                ApiCRDStatus
                    .builder()
                    .id("api-id")
                    .crossId("api-cross-id")
                    .environmentId("TEST")
                    .organizationId("TEST")
                    .plan("plan-name", "plan-id")
                    .state("STARTED")
                    .errors(new ApiCRDStatus.Errors(List.of(), List.of("something went wrong but it's OK")))
                    .build()
            );
    }

    @Test
    void should_return_empty_status_with_errors() {
        var spec = ApiCRDFixtures.anApiCRD();

        var input = new ImportApiCRDUseCase.Input(AUDIT_INFO, spec);

        when(validator.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec)))
            .thenReturn(Validator.Result.ofErrors(List.of(Validator.Error.severe("something went wrong and it's not OK"))));

        var output = cut.execute(input);

        assertThat(output.status())
            .isEqualTo(
                ApiCRDStatus.builder().errors(new ApiCRDStatus.Errors(List.of("something went wrong and it's not OK"), List.of())).build()
            );
    }
}
