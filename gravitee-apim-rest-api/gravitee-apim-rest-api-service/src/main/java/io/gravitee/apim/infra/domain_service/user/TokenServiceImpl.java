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
package io.gravitee.apim.infra.domain_service.user;

import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.gravitee.apim.core.user.model.DecodedToken;
import io.gravitee.apim.core.user.service_provider.TokenService;
import io.gravitee.rest.api.service.common.JWTHelper.Claims;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class TokenServiceImpl implements TokenService {

    private final Environment environment;

    public TokenServiceImpl(Environment environment) {
        this.environment = environment;
    }

    @Override
    public DecodedToken decode(String token) {
        var jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is mandatory");
        }

        var algorithm = Algorithm.HMAC256(jwtSecret);
        var verifier = JWT.require(algorithm).withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER)).build();
        var jwt = verifier.verify(token);

        return new DecodedToken(
            jwt.getClaim(Claims.ACTION).asString(),
            jwt.getClaim(Claims.EMAIL).asString(),
            Optional.ofNullable(jwt.getSubject())
        );
    }
}
