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

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidKeyException;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.resolver.PublicKeyResolver;
import java.net.URL;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RemoteJWKSourceResolver<C extends SecurityContext> implements JWKSourceResolver<C> {

    private final String url;

    public RemoteJWKSourceResolver(PublicKeyResolver publicKeyResolver) {
        this.url = publicKeyResolver.resolve();
        if (this.url == null) {
            throw new InvalidKeyException("JWKS URL can not be null");
        }
    }

    @Override
    public JWKSource<C> resolve() {
        try {
            return new RemoteJWKSet<>(new URL(url));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
