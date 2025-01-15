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
package io.gravitee.apim.core.group.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.group.domain_service.ValidateGroupCRDDomainService;
import io.gravitee.apim.core.group.model.crd.GroupCRDStatus;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
public class ValidateGroupCRDUseCase {

    private final ValidateGroupCRDDomainService validator;

    public ValidateGroupCRDUseCase(ValidateGroupCRDDomainService validator) {
        this.validator = validator;
    }

    public ImportGroupCRDUseCase.Output execute(ImportGroupCRDUseCase.Input input) {
        var statusBuilder = GroupCRDStatus.builder();

        validator
            .validateAndSanitize(new ValidateGroupCRDDomainService.Input(input.auditInfo(), input.spec()))
            .peek(
                sanitized -> statusBuilder.id(sanitized.spec().getId()).members(sanitized.spec().getMembers().size()),
                errors -> statusBuilder.errors(GroupCRDStatus.Errors.fromErrorList(errors))
            );

        return new ImportGroupCRDUseCase.Output(statusBuilder.build());
    }
}
