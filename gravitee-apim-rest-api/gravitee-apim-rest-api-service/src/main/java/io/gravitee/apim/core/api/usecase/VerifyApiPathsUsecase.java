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
package io.gravitee.apim.core.api.usecase;

import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;

public class VerifyApiPathsUsecase {

    private final VerifyApiPathDomainService verifyApiPathDomainService;

    public VerifyApiPathsUsecase(VerifyApiPathDomainService verifyApiPathDomainService) {
        this.verifyApiPathDomainService = verifyApiPathDomainService;
    }

    public Response execute(Request request) {
        return new Response(verifyApiPathDomainService.verifyApiPaths(GraviteeContext.getExecutionContext(), request.apiId, request.paths));
    }

    public record Request(String apiId, List<Path> paths) {}

    public record Response(List<Path> paths) {}
}
