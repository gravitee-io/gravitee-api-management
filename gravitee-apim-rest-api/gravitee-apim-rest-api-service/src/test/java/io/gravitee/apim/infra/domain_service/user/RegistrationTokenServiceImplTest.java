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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import io.gravitee.rest.api.service.common.JWTHelper;
import io.gravitee.rest.api.service.common.JWTHelper.DefaultValues;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RegistrationTokenServiceImplTest {

    private static final String JWT_SECRET = "my-test-secret-at-least-256-bits-long-padding-here";
    private static final String USER_EMAIL = "user@example.com";

    private MockEnvironment environment;
    private RegistrationTokenServiceImpl cut;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        environment.setProperty("jwt.secret", JWT_SECRET);
        cut = new RegistrationTokenServiceImpl(environment);
    }

    private String buildToken(String action, String email, String subject) {
        var builder = JWT.create()
            .withIssuer(DefaultValues.DEFAULT_JWT_ISSUER)
            .withClaim(JWTHelper.Claims.ACTION, action)
            .withClaim(JWTHelper.Claims.EMAIL, email);
        if (subject != null) {
            builder = builder.withSubject(subject);
        }
        return builder.sign(Algorithm.HMAC256(JWT_SECRET));
    }

    @Nested
    class Decode {

        @Test
        void should_decode_user_registration_token() {
            var token = buildToken(JWTHelper.ACTION.USER_REGISTRATION.name(), USER_EMAIL, null);

            var decoded = cut.decode(token);

            assertThat(decoded.action()).isEqualTo(JWTHelper.ACTION.USER_REGISTRATION.name());
            assertThat(decoded.email()).isEqualTo(USER_EMAIL);
            assertThat(decoded.subject()).isEqualTo(Optional.empty());
        }

        @Test
        void should_decode_group_invitation_token_with_subject() {
            var token = buildToken(JWTHelper.ACTION.GROUP_INVITATION.name(), USER_EMAIL, "existing-user-id");

            var decoded = cut.decode(token);

            assertThat(decoded.action()).isEqualTo(JWTHelper.ACTION.GROUP_INVITATION.name());
            assertThat(decoded.email()).isEqualTo(USER_EMAIL);
            assertThat(decoded.subject()).isEqualTo(Optional.of("existing-user-id"));
        }

        @Test
        void should_decode_reset_password_token() {
            var token = buildToken(JWTHelper.ACTION.RESET_PASSWORD.name(), USER_EMAIL, null);

            var decoded = cut.decode(token);

            assertThat(decoded.action()).isEqualTo(JWTHelper.ACTION.RESET_PASSWORD.name());
        }

        @Test
        void should_use_custom_issuer_from_environment() {
            environment.setProperty("jwt.issuer", "custom-issuer");
            var token = JWT.create()
                .withIssuer("custom-issuer")
                .withClaim(JWTHelper.Claims.ACTION, JWTHelper.ACTION.USER_REGISTRATION.name())
                .withClaim(JWTHelper.Claims.EMAIL, USER_EMAIL)
                .sign(Algorithm.HMAC256(JWT_SECRET));

            var decoded = cut.decode(token);

            assertThat(decoded.email()).isEqualTo(USER_EMAIL);
        }

        @Test
        void should_throw_when_jwt_secret_is_missing() {
            environment = new MockEnvironment();
            cut = new RegistrationTokenServiceImpl(environment);

            assertThatThrownBy(() -> cut.decode("any-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT secret is mandatory");
        }

        @Test
        void should_throw_when_token_signature_is_invalid() {
            var tokenWithWrongSecret = JWT.create()
                .withIssuer(DefaultValues.DEFAULT_JWT_ISSUER)
                .withClaim(JWTHelper.Claims.ACTION, JWTHelper.ACTION.USER_REGISTRATION.name())
                .withClaim(JWTHelper.Claims.EMAIL, USER_EMAIL)
                .sign(Algorithm.HMAC256("wrong-secret-also-needs-to-be-long-enough-here"));

            assertThatThrownBy(() -> cut.decode(tokenWithWrongSecret)).isInstanceOf(JWTVerificationException.class);
        }

        @Test
        void should_throw_when_issuer_does_not_match() {
            var tokenWithWrongIssuer = JWT.create()
                .withIssuer("wrong-issuer")
                .withClaim(JWTHelper.Claims.ACTION, JWTHelper.ACTION.USER_REGISTRATION.name())
                .withClaim(JWTHelper.Claims.EMAIL, USER_EMAIL)
                .sign(Algorithm.HMAC256(JWT_SECRET));

            assertThatThrownBy(() -> cut.decode(tokenWithWrongIssuer)).isInstanceOf(JWTVerificationException.class);
        }
    }
}
