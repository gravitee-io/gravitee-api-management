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
package io.gravitee.apim.core.application_dictionary.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.application_dictionary.crud_service.ApplicationDictionaryCrudService;
import io.gravitee.apim.core.application_dictionary.model.Dictionary;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class UpdateApplicationDictionaryUseCase {

    private final ApplicationCrudService applicationCrudService;
    private final ApplicationDictionaryCrudService applicationDictionaryCrudService;

    public Output execute(Input input) {
        log.debug("Find existing dictionary for application {}", input.applicationId());
        var dictionary = applicationDictionaryCrudService.findById(input.applicationId());

        if (!input.enabled() && dictionary.isEmpty()) {
            log.debug("Dictionary for application {} is already disabled", input.applicationId());
            return new Output(false, null);
        }

        if (!input.enabled()) {
            log.debug("Disable dictionary for application {}", input.applicationId());
            return disable(input.executionContext(), dictionary.get());
        }

        if (dictionary.isEmpty()) {
            log.debug("Create dictionary for application {}", input.applicationId());
            return create(input.executionContext(), input.applicationId(), input.dictionary);
        }

        return update(input.executionContext(), dictionary.get(), input.dictionary);
    }

    private Output update(ExecutionContext executionContext, Dictionary existing, Dictionary updated) {
        if (!updated.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("Dictionary id cannot be changed");
        }

        if (!Objects.equals(updated.getName(), existing.getName())) {
            throw new IllegalArgumentException("Dictionary name cannot be changed");
        }

        updated = applicationDictionaryCrudService.update(executionContext, updated);
        return new Output(true, updated);
    }

    private Output create(ExecutionContext executionContext, String applicationId, Dictionary dictionary) {
        var application = applicationCrudService.findById(applicationId, executionContext.getEnvironmentId());

        if (application == null) {
            throw new IllegalArgumentException("Application not found");
        }

        Dictionary created = applicationDictionaryCrudService.create(executionContext, dictionary);
        return new Output(true, created);
    }

    private Output disable(ExecutionContext executionContext, Dictionary dictionary) {
        applicationDictionaryCrudService.delete(executionContext, dictionary);
        return new Output(false, null);
    }

    public record Input(boolean enabled, ExecutionContext executionContext, Dictionary dictionary, String applicationId) {}

    public record Output(boolean enabled, Dictionary dictionary) {}
}
