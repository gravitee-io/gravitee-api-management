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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.ValidateCRDDomainService;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class ValidateCRDUseCase {

    private final ValidateCRDDomainService validator;

    public ImportCRDUseCase.Output execute(ImportCRDUseCase.Input input) {
        var statusBuilder = ApiCRDStatus.builder();

        validator
            .validateAndSanitize(new ValidateCRDDomainService.Input(input.auditInfo(), input.spec()))
            .peek(
                sanitized ->
                    statusBuilder
                        .id(sanitized.spec().getId())
                        .crossId(sanitized.spec().getCrossId())
                        .organizationId(input.auditInfo().organizationId())
                        .environmentId(input.auditInfo().environmentId())
                        .crossId(sanitized.spec().getCrossId())
                        .state(sanitized.spec().getState())
                        .plans(
                            sanitized
                                .spec()
                                .getPlans()
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getId()))
                        ),
                errors -> statusBuilder.errors(ApiCRDStatus.Errors.fromErrorList(errors))
            );

        return new ImportCRDUseCase.Output(statusBuilder.build());
    }
}
