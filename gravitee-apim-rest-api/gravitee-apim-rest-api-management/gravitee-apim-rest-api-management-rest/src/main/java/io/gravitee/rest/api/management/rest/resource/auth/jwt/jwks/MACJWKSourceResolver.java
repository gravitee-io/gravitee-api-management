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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidKeyException;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.resolver.PublicKeyResolver;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MACJWKSourceResolver<C extends SecurityContext> implements JWKSourceResolver<C> {

    private final JWK jwk;

    private MACJWKSourceResolver(String secretKey) {
        if (secretKey == null) {
            throw new InvalidKeyException("Verification key can't be null");
        }

        this.jwk = new OctetSequenceKey.Builder(secretKey.getBytes()).build();
    }

    public MACJWKSourceResolver(PublicKeyResolver publicKeyResolver) {
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
