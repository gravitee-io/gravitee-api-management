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
package io.gravitee.gateway.security.core;

import static org.mockito.Mockito.when;

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TokenExtractorTest {

    @Mock
    private Request request;

    @Test
    public void shouldNotExtract_noAuthorizationHeader() {
        HttpHeaders headers = HttpHeaders.create();
        when(request.headers()).thenReturn(headers);
        when(request.parameters()).thenReturn(new LinkedMultiValueMap<>());

        String token = TokenExtractor.extract(request);

        Assert.assertNull(token);
    }

    @Test
    public void shouldNotExtract_unknownAuthorizationHeader() {
        String jwt = "dummy-token";

        HttpHeaders headers = HttpHeaders.create();
        headers.add(HttpHeaderNames.AUTHORIZATION, "Basic " + jwt);
        when(request.headers()).thenReturn(headers);

        String token = TokenExtractor.extract(request);

        Assert.assertNull(token);
    }

    @Test
    public void shouldExtract_bearerAuthorizationHeader_noValue() {
        HttpHeaders headers = HttpHeaders.create();
        headers.add(HttpHeaderNames.AUTHORIZATION, TokenExtractor.BEARER);
        when(request.headers()).thenReturn(headers);

        String token = TokenExtractor.extract(request);

        Assert.assertEquals("", token);
    }

    @Test
    public void shouldExtract_fromHeader() {
        String jwt = "dummy-token";

        HttpHeaders headers = HttpHeaders.create();
        headers.add(HttpHeaderNames.AUTHORIZATION, TokenExtractor.BEARER + ' ' + jwt);
        when(request.headers()).thenReturn(headers);

        String token = TokenExtractor.extract(request);

        Assert.assertNotNull(token);
        Assert.assertEquals(jwt, token);
    }

    @Test
    public void shouldExtract_fromQueryParameter() {
        String jwt = "dummy-token";

        HttpHeaders headers = HttpHeaders.create();
        when(request.headers()).thenReturn(headers);

        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add(TokenExtractor.ACCESS_TOKEN, jwt);
        when(request.parameters()).thenReturn(parameters);

        String token = TokenExtractor.extract(request);

        Assert.assertNotNull(token);
        Assert.assertEquals(jwt, token);
    }
}
