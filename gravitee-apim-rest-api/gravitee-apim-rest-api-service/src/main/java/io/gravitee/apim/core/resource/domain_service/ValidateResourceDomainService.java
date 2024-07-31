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
package io.gravitee.apim.core.resource.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.resource.Resource;
import java.util.List;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
public interface ValidateResourceDomainService extends Validator<ValidateResourceDomainService.Input> {
    record Input(String environmentId, List<Resource> resources) implements Validator.Input {
        public Input sanitized(List<Resource> sanitizedResources) {
            return new Input(environmentId, sanitizedResources);
        }
    }

    Result<Input> validateAndSanitize(Input input);
}
