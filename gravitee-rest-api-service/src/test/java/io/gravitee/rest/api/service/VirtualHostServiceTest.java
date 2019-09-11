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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.service.exceptions.ApiContextPathAlreadyExistsException;
import io.gravitee.rest.api.service.impl.VirtualHostServiceImpl;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class VirtualHostServiceTest {

    @InjectMocks
    private VirtualHostService virtualHostService = new VirtualHostServiceImpl();

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiRepository apiRepository;

    @Test
    public void shouldSucceed_create_noApi() {
        when(apiRepository.search(null)).thenReturn(Collections.emptyList());

        virtualHostService.validate(Collections.singletonList(new VirtualHost("/context")));
    }

    @Test
    public void shouldSucceed_create_noMatchingPath() {
        Api api1 = createMock("mock1", "/existing");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.validate(Collections.singletonList(new VirtualHost("/context")));
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldFail_create_existingPath() {
        Api api1 = createMock("mock1", "/context");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.validate(Collections.singletonList(new VirtualHost("/context")));
    }

    @Test
    public void shouldFail_create_sameBasePath() {
        Api api1 = createMock("mock1", "/context2");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.validate(Collections.singletonList(new VirtualHost("/context")));
    }

    @Test
    public void shouldFail_create_sameBasePath2() {
        Api api1 = createMock("mock1", "/context");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.validate(Collections.singletonList(new VirtualHost("/context2")));
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldFail_create_existingPath_trailingSlash() {
        Api api1 = createMock("mock1", "/context");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.validate(Collections.singletonList(new VirtualHost("/context/")));
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldFail_create_existingPath_trailingSlash2() {
        Api api1 = createMock("mock1", "/context/");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.validate(Collections.singletonList(new VirtualHost("/context")));
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldFail_create_existingSubPath() {
        Api api1 = createMock("mock1", "/context/subpath");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.validate(Collections.singletonList(new VirtualHost("/context")));
    }

    @Test
    public void shouldSucceed_create_virtualHostWithSamePath() {
        Api api1 = createMock("mock1", "/context", "api.gravitee.io");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.validate(Collections.singletonList(new VirtualHost("/context")));
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldSucceed_create_sameVirtualHostAndSamePath() {
        Api api1 = createMock("mock1", "/context", "api.gravitee.io");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.validate(Collections.singletonList(new VirtualHost("api.gravitee.io", "/context")));
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldSucceed_create_sameVirtualHostAndSameSubPath() {
        Api api1 = createMock("mock1", "/context", "api.gravitee.io");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.validate(Collections.singletonList(new VirtualHost("api.gravitee.io", "/context/subpath")));
    }

    private Api createMock(String api, String path) {
        return createMock(api, path, null);
    }

    private Api createMock(String api, String path, String host) {
        Api api1 = mock(Api.class);
        when(api1.getId()).thenReturn(api);
        if (host == null) {
            when(api1.getDefinition()).thenReturn("{\"id\": \"" + api + "\",\"name\": \"API 1\",\"proxy\": {\"context_path\": \"" + path + "\"}}");
        } else {
            when(api1.getDefinition()).thenReturn("{\"id\": \"" + api + "\",\"name\": \"API 1\",\"proxy\": {\"virtual_hosts\": [{\"host\": \"api.gravitee.io\", \"path\": \"" + path + "\"}]}}");
        }

        return api1;
    }
}
