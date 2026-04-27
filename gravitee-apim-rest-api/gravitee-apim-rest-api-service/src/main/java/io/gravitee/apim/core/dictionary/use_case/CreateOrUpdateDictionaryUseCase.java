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
package io.gravitee.apim.core.dictionary.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.dictionary.domain_service.DictionaryAutomationDomainService;
import io.gravitee.apim.core.dictionary.model.Dictionary;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOrUpdateDictionaryUseCase {

    private final DictionaryAutomationDomainService dictionaryAutomationDomainService;

    public record Input(ExecutionContext executionContext, Dictionary dictionary, boolean deploy) {}

    public record Output(DictionaryEntity dictionary) {}

    public Output execute(Input input) {
        var dictionary = input.dictionary();
        var existing = dictionaryAutomationDomainService.findById(input.executionContext(), dictionary.getId());

        DictionaryEntity result;
        if (existing.isPresent()) {
            result = dictionaryAutomationDomainService.update(input.executionContext(), dictionary.getId(), dictionary);
        } else {
            result = dictionaryAutomationDomainService.create(input.executionContext(), dictionary);
        }

        result = dictionaryAutomationDomainService.handleDeployment(input.executionContext(), result, input.deploy());
        return new Output(result);
    }
}
