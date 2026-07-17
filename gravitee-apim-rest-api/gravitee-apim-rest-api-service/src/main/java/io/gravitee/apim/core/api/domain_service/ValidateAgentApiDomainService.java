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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.definition.model.v4.agent.AgentApi;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * Agents share no endpoint, flow or plan validation with an http v4 API, so only context-path uniqueness is checked:
 * that is the one thing they compete for on the gateway.
 */
@DomainService
@AllArgsConstructor
public class ValidateAgentApiDomainService {

    private final VerifyApiPathDomainService verifyApiPathDomainService;

    public Api validateAndSanitize(final Api api, final String environmentId) {
        if (!(api.getApiDefinitionValue() instanceof AgentApi agent)) {
            return api;
        }

        agent
            .getListeners()
            .stream()
            .filter(HttpListener.class::isInstance)
            .map(HttpListener.class::cast)
            .forEach(listener -> validateAndSanitize(listener, api.getId(), environmentId));

        return api;
    }

    /**
     * Mirrors {@code ListenerValidationServiceImpl.validateAndSanitizeHttpListener}, per listener so the sanitized
     * paths can be written back: the verifier is what raises {@code overrideAccess} on restricted-domain
     * environments, and dropping its output would leave agents configured differently from an http v4 API there.
     */
    private void validateAndSanitize(final HttpListener listener, final String apiId, final String environmentId) {
        var paths = ApiPathExtractor.extractPathsFromV4Listeners(List.of(listener));
        // A composable-only agent is reached by id from a workflow and never published on the gateway, so the http v4
        // rule requiring at least one path per listener must not reject it.
        if (paths.isEmpty()) {
            return;
        }

        var result = verifyApiPathDomainService.validateAndSanitize(new VerifyApiPathDomainService.Input(environmentId, apiId, paths));

        result
            .severe()
            .ifPresent(errors -> {
                throw new InvalidPathsException(errors.iterator().next().getMessage());
            });

        listener.setPaths(
            result.map(VerifyApiPathDomainService.Input::paths).value().stream().flatMap(List::stream).map(this::toDefinitionPath).toList()
        );
    }

    private io.gravitee.definition.model.v4.listener.http.Path toDefinitionPath(final Path path) {
        return new io.gravitee.definition.model.v4.listener.http.Path(path.getHost(), path.getPath(), path.isOverrideAccess());
    }
}
