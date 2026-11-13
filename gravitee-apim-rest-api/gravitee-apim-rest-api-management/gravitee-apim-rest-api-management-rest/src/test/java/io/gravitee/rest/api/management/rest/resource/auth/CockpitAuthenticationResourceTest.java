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
package io.gravitee.rest.api.management.rest.resource.auth;

import static io.gravitee.rest.api.management.rest.resource.auth.CockpitAuthenticationResource.ENVIRONMENT_CLAIM;
import static io.gravitee.rest.api.management.rest.resource.auth.CockpitAuthenticationResource.KID;
import static io.gravitee.rest.api.management.rest.resource.auth.CockpitAuthenticationResource.ORG_CLAIM;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.gravitee.common.http.HttpStatusCode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import java.security.KeyPairGenerator;
import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 *
 * Usefull to validate retro-compatibility after rebranding
 */
public class CockpitAuthenticationResourceTest extends AbstractCloudAuthenticationResourceTest {

    private static final String PATH = "auth/cockpit";

    @Override
    protected String getPropertyPrefix() {
        return "cockpit";
    }

    @Override
    protected String getKeystorePropertyPrefix() {
        return "cockpit";
    }

    @Test
    public void shouldRejectBlankTokenAsBadRequest() {
        final Response response = rootTarget(PATH).queryParam("token", "   ").request().get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldRejectFormPostWithoutTokenAsBadRequest() {
        final Response response = rootTarget(PATH).request().post(Entity.form(new Form()));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldRejectTokenSignedWithAnotherRsaKeyAsUnauthorized() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject("test-user-id")
            .claim(ORG_CLAIM, "organization-id")
            .claim(ENVIRONMENT_CLAIM, "environment-id")
            .expirationTime(new Date(System.currentTimeMillis() + 10_000))
            .build();
        SignedJWT foreignJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS512).keyID(KID).build(), claims);
        foreignJwt.sign(new RSASSASigner(generator.generateKeyPair().getPrivate()));

        final Response response = rootTarget(PATH).queryParam("token", foreignJwt.serialize()).request().get();
        assertEquals(HttpStatusCode.UNAUTHORIZED_401, response.getStatus());
    }
}
