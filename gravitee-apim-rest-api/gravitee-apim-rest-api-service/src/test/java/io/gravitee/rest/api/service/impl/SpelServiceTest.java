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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.service.configuration.spel.SpelService;
import io.gravitee.rest.api.service.impl.configuration.spel.SpelServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SpelServiceTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    private final SpelService spelService = new SpelServiceImpl(objectMapper);

    @Test
    public void shouldGetGrammar() throws JsonProcessingException {
        final JsonNode grammar = spelService.getGrammar();
        assertThat(grammar).isNotNull();

        String expectedJson =
            "{\"request\":{\"content\":{\"_type\":\"String\"},\"contextPath\":{\"_type\":\"String\"},\"headers\":{\"_type\":\"HttpHeaders\"},\"id\":{\"_type\":\"String\"},\"localAddress\":{\"_type\":\"String\"},\"params\":{\"_type\":\"MultiValueMap\"},\"path\":{\"_type\":\"String\"},\"pathInfo\":{\"_type\":\"String\"},\"pathInfos\":{\"_type\":\"String[]\"},\"pathParams\":{\"_type\":\"MultiValueMap\"},\"paths\":{\"_type\":\"String[]\"},\"remoteAddress\":{\"_type\":\"String\"},\"scheme\":{\"_type\":\"String\"},\"ssl\":{\"client\":{\"attributes\":{\"_type\":\"MultiValueMap\"},\"businessCategory\":{\"_type\":\"String\"},\"c\":{\"_type\":\"String\"},\"cn\":{\"_type\":\"String\"},\"countryOfCitizenship\":{\"_type\":\"String\"},\"countryOfResidence\":{\"_type\":\"String\"},\"dateOfBirth\":{\"_type\":\"String\"},\"dc\":{\"_type\":\"String\"},\"defined\":{\"_type\":\"boolean\"},\"description\":{\"_type\":\"String\"},\"dmdName\":{\"_type\":\"String\"},\"dn\":{\"_type\":\"String\"},\"dnQualifier\":{\"_type\":\"String\"},\"e\":{\"_type\":\"String\"},\"emailAddress\":{\"_type\":\"String\"},\"gender\":{\"_type\":\"String\"},\"generation\":{\"_type\":\"String\"},\"givenname\":{\"_type\":\"String\"},\"initials\":{\"_type\":\"String\"},\"l\":{\"_type\":\"String\"},\"name\":{\"_type\":\"String\"},\"nameAtBirth\":{\"_type\":\"String\"},\"o\":{\"_type\":\"String\"},\"organizationIdentifier\":{\"_type\":\"String\"},\"ou\":{\"_type\":\"String\"},\"placeOfBirth\":{\"_type\":\"String\"},\"postalAddress\":{\"_type\":\"String\"},\"postalCode\":{\"_type\":\"String\"},\"pseudonym\":{\"_type\":\"String\"},\"role\":{\"_type\":\"String\"},\"serialnumber\":{\"_type\":\"String\"},\"st\":{\"_type\":\"String\"},\"street\":{\"_type\":\"String\"},\"surname\":{\"_type\":\"String\"},\"t\":{\"_type\":\"String\"},\"telephoneNumber\":{\"_type\":\"String\"},\"uid\":{\"_type\":\"String\"},\"uniqueIdentifier\":{\"_type\":\"String\"},\"unstructuredAddress\":{\"_type\":\"String\"}},\"clientHost\":{\"_type\":\"String\"},\"clientPort\":{\"_type\":\"Integer\"},\"server\":{\"attributes\":{\"_type\":\"MultiValueMap\"},\"businessCategory\":{\"_type\":\"String\"},\"c\":{\"_type\":\"String\"},\"cn\":{\"_type\":\"String\"},\"countryOfCitizenship\":{\"_type\":\"String\"},\"countryOfResidence\":{\"_type\":\"String\"},\"dateOfBirth\":{\"_type\":\"String\"},\"dc\":{\"_type\":\"String\"},\"defined\":{\"_type\":\"boolean\"},\"description\":{\"_type\":\"String\"},\"dmdName\":{\"_type\":\"String\"},\"dn\":{\"_type\":\"String\"},\"dnQualifier\":{\"_type\":\"String\"},\"e\":{\"_type\":\"String\"},\"emailAddress\":{\"_type\":\"String\"},\"gender\":{\"_type\":\"String\"},\"generation\":{\"_type\":\"String\"},\"givenname\":{\"_type\":\"String\"},\"initials\":{\"_type\":\"String\"},\"l\":{\"_type\":\"String\"},\"name\":{\"_type\":\"String\"},\"nameAtBirth\":{\"_type\":\"String\"},\"o\":{\"_type\":\"String\"},\"organizationIdentifier\":{\"_type\":\"String\"},\"ou\":{\"_type\":\"String\"},\"placeOfBirth\":{\"_type\":\"String\"},\"postalAddress\":{\"_type\":\"String\"},\"postalCode\":{\"_type\":\"String\"},\"pseudonym\":{\"_type\":\"String\"},\"role\":{\"_type\":\"String\"},\"serialnumber\":{\"_type\":\"String\"},\"st\":{\"_type\":\"String\"},\"street\":{\"_type\":\"String\"},\"surname\":{\"_type\":\"String\"},\"t\":{\"_type\":\"String\"},\"telephoneNumber\":{\"_type\":\"String\"},\"uid\":{\"_type\":\"String\"},\"uniqueIdentifier\":{\"_type\":\"String\"},\"unstructuredAddress\":{\"_type\":\"String\"}}},\"timestamp\":{\"_type\":\"long\"},\"transactionId\":{\"_type\":\"String\"},\"uri\":{\"_type\":\"String\"},\"version\":{\"_type\":\"String\"}},\"endpoints\":{\"_type\":\"String[]\"},\"response\":{\"headers\":{\"_type\":\"HttpHeaders\"},\"content\":{\"_type\":\"String\"},\"status\":{\"_type\":\"int\"}},\"context\":{\"attributes\":{\"context-path\":{\"_type\":\"String\"},\"application\":{\"_type\":\"String\"},\"api-key\":{\"_type\":\"String\"},\"user-id\":{\"_type\":\"String\"},\"api\":{\"_type\":\"String\"},\"plan\":{\"_type\":\"String\"},\"resolved-path\":{\"_type\":\"String\"}}},\"_enums\":{\"HttpHeaders\":[\"Accept\",\"Accept-Charset\",\"Accept-Encoding\",\"Accept-Language\",\"Accept-Ranges\",\"Access-Control-Allow-Credentials\",\"Access-Control-Allow-Headers\",\"Access-Control-Allow-Methods\",\"Access-Control-Allow-Origin\",\"Access-Control-Expose-Headers\",\"Access-Control-Max-Age\",\"Access-Control-Request-Headers\",\"Access-Control-Request-Method\",\"Age\",\"Allow\",\"Authorization\",\"Cache-Control\",\"Connection\",\"Content-Disposition\",\"Content-Encoding\",\"Content-ID\",\"Content-Language\",\"Content-Length\",\"Content-Location\",\"Content-MD5\",\"Content-Range\",\"Content-Type\",\"Cookie\",\"Date\",\"ETag\",\"Expires\",\"Expect\",\"Forwarded\",\"From\",\"Host\",\"If-Match\",\"If-Modified-Since\",\"If-None-Match\",\"If-Unmodified-Since\",\"Keep-Alive\",\"Last-Modified\",\"Location\",\"Link\",\"Max-Forwards\",\"MIME-Version\",\"Origin\",\"Pragma\",\"Proxy-Authenticate\",\"Proxy-Authorization\",\"Proxy-Connection\",\"Range\",\"Referer\",\"Retry-After\",\"Server\",\"Set-Cookie\",\"Set-Cookie2\",\"TE\",\"Trailer\",\"Transfer-Encoding\",\"Upgrade\",\"User-Agent\",\"Vary\",\"Via\",\"Warning\",\"WWW-Authenticate\",\"X-Forwarded-For\",\"X-Forwarded-Proto\",\"X-Forwarded-Server\",\"X-Forwarded-Host\",\"X-Forwarded-Port\",\"X-Forwarded-Prefix\"]},\"properties\":{\"_type\":\"String[]\"},\"dictionaries\":{\"_type\":\"String[]\"},\"_types\":{\"MultiValueMap\":{\"methods\":[]},\"HttpHeaders\":{\"methods\":[]},\"Map\":{\"methods\":[]},\"Boolean\":{\"methods\":[]},\"Integer\":{\"methods\":[]},\"Long\":{\"methods\":[]},\"Math\":{\"methods\":[]},\"Object\":{\"methods\":[]},\"List\":{\"methods\":[]},\"Collection\":{\"methods\":[]},\"Set\":{\"methods\":[]},\"String\":{\"methods\":[]},\"String[]\":{\"methods\":[]}}}";

        assertThat(grammar).isEqualTo(objectMapper.readTree(expectedJson));
    }
}
