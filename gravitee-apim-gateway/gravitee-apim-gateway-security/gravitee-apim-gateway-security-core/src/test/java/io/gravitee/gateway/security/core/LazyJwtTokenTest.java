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
package io.gravitee.gateway.security.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.nimbusds.jose.Header;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.util.Map;
import net.minidev.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class LazyJwtTokenTest {

    public static final String MY_JWT_TOKEN = "myJwtToken";

    @Mock
    JWT jwt;

    MockedStatic<JWTParser> mockedStaticJwtParser;

    @Before
    public void setUp() throws ParseException {
        mockedStaticJwtParser = mockStatic(JWTParser.class);
        mockedStaticJwtParser.when(() -> JWTParser.parse(MY_JWT_TOKEN)).thenReturn(jwt);
    }

    @After
    public void tearDown() throws Exception {
        mockedStaticJwtParser.close();
    }

    @Test
    public void getClaims_should_return_null_when_jwtToken_claimsSet_is_null() throws ParseException {
        LazyJwtToken token = new LazyJwtToken(MY_JWT_TOKEN);

        when(jwt.getJWTClaimsSet()).thenReturn(null);

        assertNull(token.getClaims());
    }

    @Test
    public void getClaims_should_return_claims_from_jwtToken_claimSet() throws ParseException {
        LazyJwtToken token = new LazyJwtToken(MY_JWT_TOKEN);

        when(jwt.getJWTClaimsSet()).thenReturn(new JWTClaimsSet.Builder().claim("claim1", "value1").claim("claim2", "value3").build());

        Map<String, Object> resultClaims = token.getClaims();
        assertEquals(Map.of("claim1", "value1", "claim2", "value3"), resultClaims);
    }

    @Test
    public void getClaims_twice_should_parse_token_only_once() throws ParseException {
        LazyJwtToken token = new LazyJwtToken(MY_JWT_TOKEN);

        Map<String, Object> resultClaims1 = token.getClaims();
        Map<String, Object> resultClaims2 = token.getClaims();

        assertSame(resultClaims2, resultClaims1);
        mockedStaticJwtParser.verify(() -> JWTParser.parse(MY_JWT_TOKEN), times(1));
        JWTParser.parse(any());
    }

    @Test
    public void getHeaders_should_return_null_when_jwtToken_header_is_null() throws ParseException {
        LazyJwtToken token = new LazyJwtToken(MY_JWT_TOKEN);

        when(jwt.getHeader()).thenReturn(null);

        assertNull(token.getHeaders());
    }

    @Test
    public void getHeaders_should_return_headers_from_jwtToken_header() throws ParseException {
        LazyJwtToken token = new LazyJwtToken(MY_JWT_TOKEN);

        JSONObject jsonObject = Mockito.mock(JSONObject.class);
        Header header = Mockito.mock(Header.class);
        when(header.toJSONObject()).thenReturn(jsonObject);
        when(jwt.getHeader()).thenReturn(header);

        assertSame(token.getHeaders(), jsonObject);
    }
}
