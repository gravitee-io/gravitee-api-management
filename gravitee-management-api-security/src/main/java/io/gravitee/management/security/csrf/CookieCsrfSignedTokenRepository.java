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
package io.gravitee.management.security.csrf;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import io.gravitee.management.security.cookies.CookieGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.util.Date;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CookieCsrfSignedTokenRepository implements InitializingBean, CsrfTokenRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(CookieCsrfSignedTokenRepository.class);

    private static final String DEFAULT_CSRF_COOKIE_NAME = "XSRF-TOKEN";

    private static final String DEFAULT_CSRF_PARAMETER_NAME = "_csrf";

    private static final String DEFAULT_CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    private final static String DEFAULT_JWT_ISSUER = "gravitee-management-auth";

    @Autowired
    private CookieGenerator cookieGenerator;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer:" + DEFAULT_JWT_ISSUER + "}")
    private String issuer;

    private JWSSigner signer;
    private JWSVerifier verifier;

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .issueTime(new Date())
                    .build();

            JWSObject jwsObject = new JWSObject(new JWSHeader((JWSAlgorithm.HS256)), new Payload(claims.toJSONObject()));
            jwsObject.sign(signer);

            return new DefaultCsrfToken(DEFAULT_CSRF_HEADER_NAME, DEFAULT_CSRF_PARAMETER_NAME, jwsObject.serialize());
        } catch (JOSEException ex) {
            LOGGER.error("Unable to generate CSRF token", ex);
            return null;
        }
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request,
                          HttpServletResponse response) {
        String tokenValue = token == null ? "" : token.getToken();

        Cookie cookie = cookieGenerator.generate(DEFAULT_CSRF_COOKIE_NAME, tokenValue, false);

        response.addCookie(cookie);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, DEFAULT_CSRF_COOKIE_NAME);
        if (cookie == null) {
            return null;
        }
        String token = cookie.getValue();
        if (!StringUtils.hasLength(token)) {
            return null;
        }

        try {
            JWSObject jws = JWSObject.parse(token);
            if (jws.verify(verifier)) {
                return new DefaultCsrfToken(DEFAULT_CSRF_HEADER_NAME, DEFAULT_CSRF_PARAMETER_NAME, token);
            }
        } catch (ParseException | JOSEException ex) {
            LOGGER.error("Unable to verify CSRF token", ex);
        }

        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Add padding if necessary
        // HS256 need, at least, 32 ascii characters
        secret = org.apache.commons.lang3.StringUtils.leftPad(secret, 32, '0');

        signer = new MACSigner(secret);
        verifier = new MACVerifier(secret);
    }
}
