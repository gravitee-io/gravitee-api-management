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
package io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidKeyException;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.resolver.PublicKeyResolver;
import java.security.interfaces.ECPublicKey;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ECJWKSourceResolver<C extends SecurityContext> implements JWKSourceResolver<C> {

    private final JWK jwk;
    private static final Logger LOGGER = LoggerFactory.getLogger(ECJWKSourceResolver.class);

    private ECJWKSourceResolver(String publicKey) {
        if (publicKey == null) {
            throw new InvalidKeyException("Public key can't be null");
        }

        ECPublicKey ecPublicKey = null;

        try {
            JWK key = JWK.parseFromPEMEncodedObjects(publicKey);
            ecPublicKey = ((ECKey) key).toECPublicKey();
        } catch (JOSEException e) {
            LOGGER.error("unable to parse public key {}", e.getMessage());
        }

        this.jwk = ecPublicKey != null ? new ECKey.Builder(Curve.forECParameterSpec(ecPublicKey.getParams()), ecPublicKey).build() : null;
    }

    public ECJWKSourceResolver(PublicKeyResolver publicKeyResolver) {
        this(publicKeyResolver.resolve());
    }

    @Override
    public JWKSource<C> resolve() {
        try {
            return new ImmutableJWKSet<>(new JWKSet(jwk));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
