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
package io.gravitee.management.rest.resource.auth;

import com.auth0.jwt.JWTSigner;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.common.JWTHelper;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.gravitee.management.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EXPIRE_AFTER;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractAuthenticationResource {

    @Autowired
    protected Environment environment;

    @Autowired
    protected UserService userService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String CLIENT_ID_KEY = "client_id", REDIRECT_URI_KEY = "redirect_uri",
            CLIENT_SECRET = "client_secret", CODE_KEY = "code", GRANT_TYPE_KEY = "grant_type",
            AUTH_CODE = "authorization_code";

    protected Map<String, Object> getResponseEntity(final Response response) throws IOException {
        return getEntity((getResponseEntityAsString(response)));
    }

    protected String getResponseEntityAsString(final Response response) throws IOException {
        return response.readEntity(String.class);
    }

    protected Map<String, Object> getEntity(final String response) throws IOException {
        return MAPPER.readValue(response, new TypeReference<Map<String, Object>>() {});
    }

    protected Response connectUser(String username) {
        UserEntity user = userService.connect(username);

        // JWT signer
        final Map<String, Object> claims = new HashMap<>();

        claims.put(JWTHelper.Claims.ISSUER, environment.getProperty("jwt.issuer", JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER));
        claims.put(JWTHelper.Claims.SUBJECT, user.getUsername());
//        claims.put(JWTHelper.Claims.PERMISSIONS, Collections.singleton(new SimpleGrantedAuthority(Role.USER.toString())));
        claims.put(JWTHelper.Claims.EMAIL, user.getEmail());
        claims.put(JWTHelper.Claims.FIRSTNAME, user.getFirstname());
        claims.put(JWTHelper.Claims.LASTNAME, user.getLastname());

        final JWTSigner.Options options = new JWTSigner.Options();
        options.setExpirySeconds(environment.getProperty("jwt.expire-after", Integer.class, DEFAULT_JWT_EXPIRE_AFTER));
        options.setIssuedAt(true);
        options.setJwtId(true);

        return Response.ok()
                .entity(user)
                .cookie(new NewCookie(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + new JWTSigner(
                                environment.getProperty("jwt.secret")
                        ).sign(claims, options),
                        environment.getProperty("jwt.cookie-path", "/"),
                        environment.getProperty("jwt.cookie-domain"),
                        "",
                        environment.getProperty("jwt.expire-after", Integer.class, DEFAULT_JWT_EXPIRE_AFTER),
                        environment.getProperty("jwt.cookie-secure", Boolean.class, false),
                        true))
                .build();
    }

    public static class Payload {
        @NotBlank
        String clientId;

        @NotBlank
        String redirectUri;

        @NotBlank
        String code;

        String state;

        public String getClientId() {
            return clientId;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public String getCode() {
            return code;
        }

        public String getState() {
            return state;
        }
    }
}
