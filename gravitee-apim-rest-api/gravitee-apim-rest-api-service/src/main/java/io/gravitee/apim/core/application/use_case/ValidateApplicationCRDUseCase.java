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
package io.gravitee.apim.core.application.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.application.domain_service.ValidateApplicationCRDDomainService;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDStatus;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class ValidateApplicationCRDUseCase {

    private final ValidateApplicationCRDDomainService validator;

    public ImportApplicationCRDUseCase.Output execute(ImportApplicationCRDUseCase.Input input) {
        var statusBuilder = ApplicationCRDStatus.builder();

        validator
            .validateAndSanitize(new ValidateApplicationCRDDomainService.Input(input.auditInfo(), input.crd()))
            .peek(
                sanitized ->
                    statusBuilder
                        .id(sanitized.spec().getId())
                        .organizationId(sanitized.auditInfo().organizationId())
                        .environmentId(sanitized.auditInfo().environmentId())
                        .build(),
                errors -> statusBuilder.errors(ApplicationCRDStatus.Errors.fromErrorList(errors))
            );

        return new ImportApplicationCRDUseCase.Output(statusBuilder.build());
    }
}
