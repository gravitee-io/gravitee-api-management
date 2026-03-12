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
package io.gravitee.apim.integration.tests.plan.basicauth;

import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.api.policy.http.HttpSecurityPolicy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Test policy that extracts username from Basic Auth header for plan-based Basic Auth integration tests.
 * Mirrors the behavior of BasicAuthPolicy from the gateway tests SDK.
 *
 * @author GraviteeSource Team
 */
public class BasicAuthTestPolicy implements HttpSecurityPolicy {

    public static final String BASIC_AUTH_TOKEN_TYPE = "BASIC_AUTH";

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
            String encoded = authHeader.substring("Basic ".length());
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
        return Completable.complete();
    }
}
