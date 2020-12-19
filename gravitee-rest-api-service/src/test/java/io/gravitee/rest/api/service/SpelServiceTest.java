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
package io.gravitee.rest.api.service;


import io.gravitee.el.spel.context.SecuredMethodResolver;
import io.gravitee.el.spel.context.SecuredResolver;
import io.gravitee.rest.api.service.configuration.spel.SpelService;
import io.gravitee.rest.api.service.impl.configuration.spel.SpelServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SpelServiceTest {

    @InjectMocks
    private SpelService spelService = new SpelServiceImpl();

    @Mock
    SecuredMethodResolver securedMethodResolver = new SecuredMethodResolver();

    @Before
    public void init() {
        when(securedMethodResolver.getMethods(any())).thenReturn(new Method[]{});
    }

    @Test
    public void shouldGetGrammar() {
        assertNotNull(spelService.getGrammar());
        assertEquals("{\"request\":{\"headers\":{\"_type\":\"HttpHeaders\"},\"method\":{\"_type\":\"String\"},\"scheme\":{\"_type\":\"String\"},\"pathParams\":{\"_type\":\"MultiValueMap\"},\"pathInfos\":{\"_type\":\"String[]\"},\"contextPath\":{\"_type\":\"String\"},\"params\":{\"_type\":\"MultiValueMap\"},\"uri\":{\"_type\":\"String\"},\"version\":{\"_type\":\"String\"},\"content\":{\"_type\":\"String\"},\"transactionId\":{\"_type\":\"String\"},\"path\":{\"_type\":\"String\"},\"localAddress\":{\"_type\":\"String\"},\"paths\":{\"_type\":\"String[]\"},\"id\":{\"_type\":\"String\"},\"pathInfo\":{\"_type\":\"String\"},\"remoteAddress\":{\"_type\":\"String\"},\"timestamp\":{\"_type\":\"long\"}},\"endpoints\":{\"_type\":\"String[]\"},\"response\":{\"headers\":{\"_type\":\"HttpHeaders\"},\"content\":{\"_type\":\"String\"},\"status\":{\"_type\":\"int\"}},\"context\":{\"attributes\":{\"context-path\":{\"_type\":\"String\"},\"application\":{\"_type\":\"String\"},\"api-key\":{\"_type\":\"String\"},\"user-id\":{\"_type\":\"String\"},\"api\":{\"_type\":\"String\"},\"plan\":{\"_type\":\"String\"},\"resolved-path\":{\"_type\":\"String\"}}},\"_enums\":{\"HttpHeaders\":[\"Accept\",\"Accept-Charset\",\"Accept-Encoding\",\"Accept-Language\",\"Accept-Ranges\",\"Access-Control-Allow-Credentials\",\"Access-Control-Allow-Headers\",\"Access-Control-Allow-Methods\",\"Access-Control-Allow-Origin\",\"Access-Control-Expose-Headers\",\"Access-Control-Max-Age\",\"Access-Control-Request-Headers\",\"Access-Control-Request-Method\",\"Age\",\"Allow\",\"Authorization\",\"Cache-Control\",\"Connection\",\"Content-Disposition\",\"Content-Encoding\",\"Content-ID\",\"Content-Language\",\"Content-Length\",\"Content-Location\",\"Content-MD5\",\"Content-Range\",\"Content-Type\",\"Cookie\",\"Date\",\"ETag\",\"Expires\",\"Expect\",\"Forwarded\",\"From\",\"Host\",\"If-Match\",\"If-Modified-Since\",\"If-None-Match\",\"If-Unmodified-Since\",\"Keep-Alive\",\"Last-Modified\",\"Location\",\"Link\",\"Max-Forwards\",\"MIME-Version\",\"Origin\",\"Pragma\",\"Proxy-Authenticate\",\"Proxy-Authorization\",\"Proxy-Connection\",\"Range\",\"Referer\",\"Retry-After\",\"Server\",\"Set-Cookie\",\"Set-Cookie2\",\"TE\",\"Trailer\",\"Transfer-Encoding\",\"Upgrade\",\"User-Agent\",\"Vary\",\"Via\",\"Warning\",\"WWW-Authenticate\",\"X-Forwarded-For\",\"X-Forwarded-Proto\",\"X-Forwarded-Server\",\"X-Forwarded-Host\"]},\"_types\":{\"HttpHeaders\":{\"methods\":[]},\"Set\":{\"methods\":[]},\"String\":{\"methods\":[]},\"String[]\":{\"methods\":[]},\"Math\":{\"methods\":[]},\"Integer\":{\"methods\":[]},\"Long\":{\"methods\":[]},\"Collection\":{\"methods\":[]},\"Object\":{\"methods\":[]},\"List\":{\"methods\":[]},\"Boolean\":{\"methods\":[]},\"Map\":{\"methods\":[]},\"MultiValueMap\":{\"methods\":[]}},\"properties\":{\"_type\":\"String[]\"},\"dictionaries\":{\"_type\":\"String[]\"}}",
            spelService.getGrammar().toJSONString());
    }


}
