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
package io.gravitee.integration.controller.websocket.auth;

import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.exchange.controller.websocket.auth.WebSocketControllerAuthentication;
import io.gravitee.integration.controller.command.IntegrationCommandContext;
import io.gravitee.rest.api.service.TokenService;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("integrationWebsocketControllerAuthentication")
@RequiredArgsConstructor
public class IntegrationWebsocketControllerAuthentication implements WebSocketControllerAuthentication<IntegrationCommandContext> {

    static final String AUTHORIZATION_HEADER = HttpHeaderNames.AUTHORIZATION.toString();
    static final String ORGANIZATION_HEADER = "X-Gravitee-Organization-Id";
    static final String AUTHORIZATION_HEADER_BEARER = "bearer";

    private final TokenService tokenService;
    private final UserCrudService userCrudService;
    private final LicenseDomainService licenseDomainService;

    @Override
    public IntegrationCommandContext authenticate(final HttpServerRequest httpServerRequest) {
        var headers = httpServerRequest.headers();
        var tokenValue = Optional
            .ofNullable(headers.get(AUTHORIZATION_HEADER))
            .map(authorizationHeader -> authorizationHeader.substring(AUTHORIZATION_HEADER_BEARER.length()).trim());

        if (tokenValue.isPresent()) {
            try {
                var token = tokenService.findByToken(tokenValue.get());

                return userCrudService
                    .findBaseUserById(token.getReferenceId())
                    .map(BaseUserEntity::getOrganizationId)
                    .filter(licenseDomainService::isFederationFeatureAllowed)
                    .map(organizationId -> new IntegrationCommandContext(true, organizationId))
                    .orElse(new IntegrationCommandContext(false));
            } catch (Exception e) {
                log.warn("Unable to authenticate incoming websocket controller request");
            }
        }

        log.warn("No authentication authorizationHeader in the incoming websocket controller request");
        return new IntegrationCommandContext(false);
    }
}
