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
package io.gravitee.apim.infra.domain_service.integration;

import io.gravitee.exchange.controller.websocket.auth.WebSocketControllerAuthentication;
import io.gravitee.integration.controller.command.IntegrationCommandContext;
import io.gravitee.repository.management.model.Token;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationWebsocketControllerAuthentication implements WebSocketControllerAuthentication<IntegrationCommandContext> {

    public static final String AUTHORIZATION_HEADER = HttpHeaderNames.AUTHORIZATION.toString();
    public static final String AUTHORIZATION_HEADER_BEARER = "bearer";
    private final TokenService tokenService;
    private final UserService userService;
    private final EnvironmentService environmentService;
    private final PermissionService permissionService;

    @Override
    public IntegrationCommandContext authenticate(final HttpServerRequest httpServerRequest) {
        String header = httpServerRequest.headers().get(AUTHORIZATION_HEADER);
        if (header != null) {
            final String tokenValue = header.substring(AUTHORIZATION_HEADER_BEARER.length()).trim();
            try {
                final Token token = tokenService.findByToken(tokenValue);

                final UserEntity user = userService.findById(token.getReferenceId());
                Set<String> authorizedEnvironments = environmentService
                    .findByOrganization(user.getOrganizationId())
                    .stream()
                    .filter(environmentEntity -> {
                        ExecutionContext executionContext = new ExecutionContext(environmentEntity);
                        return permissionService.hasPermission(
                            executionContext,
                            user.getId(),
                            RolePermission.ENVIRONMENT_INTEGRATION,
                            executionContext.getEnvironmentId(),
                            RolePermissionAction.CREATE
                        );
                    })
                    .map(EnvironmentEntity::getId)
                    .collect(Collectors.toSet());

                return new IntegrationCommandContext(!authorizedEnvironments.isEmpty(), authorizedEnvironments);
            } catch (Exception e) {
                log.warn("Unable to authenticate incoming websocket controller request");
            }
        }
        log.warn("No authentication header in the incoming websocket controller request");
        return new IntegrationCommandContext(false, Set.of());
    }
}
