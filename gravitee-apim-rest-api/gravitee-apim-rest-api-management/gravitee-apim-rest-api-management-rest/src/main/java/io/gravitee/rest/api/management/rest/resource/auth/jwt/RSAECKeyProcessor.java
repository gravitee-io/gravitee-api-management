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
package io.gravitee.rest.api.management.rest.resource.auth.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RSAECKeyProcessor<C extends SecurityContext> extends AbstractKeyProcessor<C> {

    public RSAECKeyProcessor(DefaultJWTClaimsVerifier<C> claimsVerifier) {
        super(claimsVerifier);
    }

    @Override
    JWSKeySelector<C> jwsKeySelector(JWKSource<C> jwkSource, Algorithm signature) {
        return new JWSVerificationKeySelector<>(signature.getAlgorithm(), jwkSource) {
            @Override
            protected JWKMatcher createJWKMatcher(final JWSHeader jwsHeader) {
                JWSAlgorithm algorithm = jwsHeader.getAlgorithm();
                if (!isAllowed(algorithm)) {
                    // Unexpected JWS alg
                    return null;
                } else if (JWSAlgorithm.Family.RSA.contains(algorithm) || JWSAlgorithm.Family.EC.contains(algorithm)) {
                    // RSA or EC key matcher
                    return new JWKMatcher.Builder()
                        .keyType(KeyType.forAlgorithm(algorithm))
                        .keyUses(KeyUse.SIGNATURE, null)
                        .algorithms(algorithm, null)
                        .x509CertSHA256Thumbprint(jwsHeader.getX509CertSHA256Thumbprint())
                        .build();
                } else {
                    return null; // Unsupported algorithm
                }
            }
        };
    }
}
