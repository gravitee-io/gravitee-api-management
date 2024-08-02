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
package io.gravitee.apim.infra.domain_service.resource;

import static io.gravitee.apim.core.utils.CollectionUtils.isEmpty;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.service.v4.validation.ResourcesValidationService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ValidateResourceDomainServiceLegacyWrapper implements ValidateResourceDomainService {

    private final ResourcesValidationService resourcesValidationService;

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        if (isEmpty(input.resources())) {
            log.debug("no resource to resolve");
            return Result.ofValue(input);
        }
        log.debug("validating resources");
        var errors = new ArrayList<Error>();

        // To make sure all resources are validated at once
        for (Resource resource : input.resources()) {
            try {
                resourcesValidationService.validateAndSanitize(List.of(resource));
            } catch (Exception e) {
                errors.add(Error.severe("Resource [%s] configuration is not valid", resource.getName()));
            }
        }

        return Result.ofBoth(input.sanitized(input.resources()), errors);
    }
}
