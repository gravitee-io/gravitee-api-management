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
package io.gravitee.policy.basicauth;

import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_API;

import io.gravitee.gateway.handlers.api.services.basicauth.BasicAuthCacheService;
import io.gravitee.gateway.handlers.api.services.basicauth.BasicAuthCredential;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.api.policy.http.HttpSecurityPolicy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Basic Auth security policy that validates HTTP Basic Authentication credentials
 * against the Gateway's BasicAuthCacheService.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class BasicAuthPolicy implements HttpSecurityPolicy {

    public static final String BASIC_AUTH_TOKEN_TYPE = "BASIC_AUTH";
    private static final String GATEWAY_BASIC_AUTH_INVALID = "GATEWAY_BASIC_AUTH_INVALID";
    private static final String WWW_AUTHENTICATE_BASIC = "Basic realm=\"gravitee.io\"";
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Override
    public String id() {
        return "basic-auth";
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    public boolean requireSubscription() {
        return true;
    }

    @Override
    public Maybe<SecurityToken> extractSecurityToken(HttpPlainExecutionContext ctx) {
        String authHeader = ctx.request().headers().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return Maybe.empty();
        }

        try {
            String encoded = authHeader.substring("Basic ".length()).trim();
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            if (parts.length != 2) {
                return Maybe.empty();
            }

            String username = parts[0];
            return Maybe.just(SecurityToken.builder().tokenType(BASIC_AUTH_TOKEN_TYPE).tokenValue(username).build());
        } catch (Exception e) {
            return Maybe.empty();
        }
    }

    @Override
    public Completable onRequest(HttpPlainExecutionContext ctx) {
        return Completable.defer(() -> {
            SecurityToken securityToken = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SECURITY_TOKEN);
            if (securityToken == null || !BASIC_AUTH_TOKEN_TYPE.equals(securityToken.getTokenType())) {
                return Completable.complete();
            }

            String api = ctx.getAttribute(ATTR_API);
            String username = securityToken.getTokenValue();

            BasicAuthCacheService basicAuthCacheService = ctx.getComponent(BasicAuthCacheService.class);
            if (basicAuthCacheService == null) {
                ctx.withLogger(log).warn("BasicAuthCacheService not available in execution context");
                return ctx.interruptWith(
                    new ExecutionFailure(401).key(GATEWAY_BASIC_AUTH_INVALID).message("Invalid Basic Auth credentials")
                );
            }

            Optional<BasicAuthCredential> credentialOpt = basicAuthCacheService.getByApiAndUsername(api, username);
            if (credentialOpt.isPresent()) {
                return validatePassword(ctx, credentialOpt.get());
            }
            return ctx.interruptWith(
                new ExecutionFailure(401).key(GATEWAY_BASIC_AUTH_INVALID).message("Invalid Basic Auth credentials")
            );
        });
    }

    private Completable validatePassword(HttpPlainExecutionContext ctx, BasicAuthCredential credential) {
        String authHeader = ctx.request().headers().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ctx.interruptWith(
                new ExecutionFailure(401).key(GATEWAY_BASIC_AUTH_INVALID).message("Invalid Basic Auth credentials")
            );
        }

        try {
            String encoded = authHeader.substring("Basic ".length()).trim();
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            if (parts.length != 2) {
                return ctx.interruptWith(
                    new ExecutionFailure(401).key(GATEWAY_BASIC_AUTH_INVALID).message("Invalid Basic Auth credentials")
                );
            }

            String providedPassword = parts[1];
            String storedHash = credential.getPassword();

            if (PASSWORD_ENCODER.matches(providedPassword, storedHash)) {
                return Completable.complete();
            }

            return ctx.interruptWith(
                new ExecutionFailure(401).key(GATEWAY_BASIC_AUTH_INVALID).message("Invalid Basic Auth credentials")
            );
        } catch (Exception e) {
            ctx.withLogger(log).debug("Failed to validate Basic Auth password", e);
            return ctx.interruptWith(
                new ExecutionFailure(401).key(GATEWAY_BASIC_AUTH_INVALID).message("Invalid Basic Auth credentials")
            );
        }
    }

    @Override
    public Single<Boolean> wwwAuthenticate(HttpPlainExecutionContext ctx) {
        return Single.fromCallable(() -> {
            ctx.response().headers().set("WWW-Authenticate", WWW_AUTHENTICATE_BASIC);
            return true;
        });
    }
}
