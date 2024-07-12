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
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;

@UseCase
public class VerifyApiPathsUseCase {

    private final VerifyApiPathDomainService verifyApiPathDomainService;

    public VerifyApiPathsUseCase(VerifyApiPathDomainService verifyApiPathDomainService) {
        this.verifyApiPathDomainService = verifyApiPathDomainService;
    }

    public Response execute(Request request) throws InvalidPathsException {
        var validationResult = verifyApiPathDomainService.validateAndSanitize(
            new VerifyApiPathDomainService.Input(GraviteeContext.getExecutionContext().getEnvironmentId(), request.apiId, request.paths)
        );

        validationResult
            .severe()
            .ifPresent(errors -> {
                throw new InvalidPathsException(errors.iterator().next().getMessage());
            });

        return validationResult.value().map(sanitized -> new Response(sanitized.paths())).orElseGet(() -> new Response(List.of()));
    }

    public record Request(String apiId, List<Path> paths) {}

    public record Response(List<Path> paths) {}
}
