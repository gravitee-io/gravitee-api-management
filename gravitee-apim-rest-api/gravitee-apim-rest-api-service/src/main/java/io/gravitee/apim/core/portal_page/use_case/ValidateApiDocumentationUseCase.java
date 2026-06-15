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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_page.domain_service.ValidateApiDocumentationDomainService;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ValidateApiDocumentationUseCase {

    private final ValidateApiDocumentationDomainService validator;

    public CreateOrUpdateApiDocumentationUseCase.Output execute(CreateOrUpdateApiDocumentationUseCase.Input input) {
        var result = validator.validateAndSanitize(
            new ValidateApiDocumentationDomainService.Input(
                input.auditInfo(),
                input.portalPageContentId(),
                input.apiId(),
                input.name(),
                input.type(),
                input.content(),
                input.location(),
                input.order()
            )
        );
        List<Validator.Error> errors = result.errors().orElseGet(List::of);
        return new CreateOrUpdateApiDocumentationUseCase.Output(input.portalPageContentId(), errors);
    }
}
