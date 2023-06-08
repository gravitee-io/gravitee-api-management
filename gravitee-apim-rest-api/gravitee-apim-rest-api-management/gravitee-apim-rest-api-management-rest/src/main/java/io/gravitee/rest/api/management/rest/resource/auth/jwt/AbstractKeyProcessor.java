/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource.auth.jwt;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.alg.Signature;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidTokenException;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks.JWKSourceResolver;

import java.util.concurrent.CompletableFuture;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractKeyProcessor<C extends SecurityContext> implements KeyProcessor {

    private JWKSourceResolver<C> jwkSourceResolver;

    protected final DefaultJWTClaimsVerifier<C> claimsVerifier;

    protected AbstractKeyProcessor(DefaultJWTClaimsVerifier<C> claimsVerifier) {
        this.claimsVerifier = claimsVerifier;
    }

    @Override
    public CompletableFuture<JWTClaimsSet> process(Signature signature, String token) {
        return jwkSourceResolver
            .resolve()
            .thenCompose(jwkSource -> {
                ConfigurableJWTProcessor<C> jwtProcessor = new DefaultJWTProcessor<>();
                jwtProcessor.setJWTClaimsSetVerifier(claimsVerifier);
                jwtProcessor.setJWSKeySelector(jwsKeySelector(jwkSource, signature));

                try {
                    return CompletableFuture.completedFuture(jwtProcessor.process(token, null));
                } catch (Exception ex) {
                    throw new InvalidTokenException(ex);
                }
            });
    }

    public void setJwkSourceResolver(JWKSourceResolver<C> jwkSourceResolver) {
        this.jwkSourceResolver = jwkSourceResolver;
    }

    abstract JWSKeySelector<C> jwsKeySelector(JWKSource<C> jwkSource, Signature signature);
}
