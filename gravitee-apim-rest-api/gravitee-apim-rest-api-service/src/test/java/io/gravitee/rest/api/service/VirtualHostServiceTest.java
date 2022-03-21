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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiContextPathAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.InvalidVirtualHostException;
import io.gravitee.rest.api.service.impl.VirtualHostServiceImpl;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

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

    @Mock
    private EnvironmentService environmentService;

    @Before
    public void init() {
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        when(environmentService.findById(any())).thenReturn(mock(EnvironmentEntity.class));
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldSucceed_create_noApi() {
        when(apiRepository.search(null)).thenReturn(Collections.emptyList());

        virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(new VirtualHost("/context"))
        );
    }

    @Test
    public void shouldSucceed_create_noMatchingPath() {
        Api api1 = createMock("mock1", "/existing");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(new VirtualHost("/context"))
        );
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldFail_create_existingPath() {
        Api api1 = createMock("mock1", "/context");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(new VirtualHost("/context"))
        );
    }

    @Test
    public void shouldFail_create_sameBasePath() {
        Api api1 = createMock("mock1", "/context2");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(new VirtualHost("/context"))
        );
    }

    @Test
    public void shouldFail_create_sameBasePath2() {
        Api api1 = createMock("mock1", "/context");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(new VirtualHost("/context2"))
        );
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldFail_create_existingPath_trailingSlash() {
        Api api1 = createMock("mock1", "/context");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(new VirtualHost("/context/"))
        );
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldFail_create_existingPath_trailingSlash2() {
        Api api1 = createMock("mock1", "/context/");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(new VirtualHost("/context"))
        );
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldFail_create_existingSubPath() {
        Api api1 = createMock("mock1", "/context/subpath");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(new VirtualHost("/context"))
        );
    }

    @Test
    public void shouldSucceed_create_virtualHostWithSamePath() {
        Api api1 = createMock("mock1", "/context", "api.gravitee.io");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(new VirtualHost("/context"))
        );
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldSucceed_create_sameVirtualHostAndSamePath() {
        Api api1 = createMock("mock1", "/context", "api.gravitee.io");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(new VirtualHost("api.gravitee.io", "/context"))
        );
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldSucceed_create_sameVirtualHostAndSameSubPath() {
        Api api1 = createMock("mock1", "/context", "api.gravitee.io");
        when(apiRepository.search(null)).thenReturn(Collections.singletonList(api1));

        virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(new VirtualHost("api.gravitee.io", "/context/subpath"))
        );
    }

    @Test
    public void validate_hostEqualsToDomainConstraint() {
        VirtualHost vhost = getValidVirtualHost();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setDomainRestrictions(Collections.singletonList(vhost.getHost()));
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        virtualHostService.sanitizeAndValidate(GraviteeContext.getExecutionContext(), Collections.singletonList(vhost));
    }

    @Test
    public void validate_hostSubDomainOfDomainConstraint() {
        VirtualHost vhost = getValidVirtualHost();
        String domainConstraint = vhost.getHost();
        vhost.setHost("level2.level1." + domainConstraint);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setDomainRestrictions(Collections.singletonList(domainConstraint));
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        virtualHostService.sanitizeAndValidate(GraviteeContext.getExecutionContext(), Collections.singletonList(vhost));
    }

    @Test
    public void validate_hostSubDomainOfOneOfDomainConstraints() {
        VirtualHost vhost = getValidVirtualHost();
        String domainConstraint = vhost.getHost();
        vhost.setHost("level2.level1." + domainConstraint);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setDomainRestrictions(Arrays.asList("test.gravitee.io", "other.gravitee.io", domainConstraint));
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        virtualHostService.sanitizeAndValidate(GraviteeContext.getExecutionContext(), Collections.singletonList(vhost));
    }

    @Test(expected = InvalidVirtualHostException.class)
    public void validate_notASubDomain() {
        VirtualHost vhost = getValidVirtualHost();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setDomainRestrictions(Arrays.asList("test.gravitee.io", "other.gravitee.io"));
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        virtualHostService.sanitizeAndValidate(GraviteeContext.getExecutionContext(), Collections.singletonList(vhost));
    }

    @Test
    public void validate_nullIsNotASubDomain() {
        VirtualHost vhost = new VirtualHost();
        vhost.setHost(null);
        vhost.setPath("/validVhostPath");

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setDomainRestrictions(Arrays.asList("test.gravitee.io", "other.gravitee.io"));
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        final Collection<VirtualHost> virtualHosts = virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(vhost)
        );
        Assert.assertEquals(1, virtualHosts.size());
        Assert.assertEquals("test.gravitee.io", virtualHosts.iterator().next().getHost());
    }

    private Api createMock(String api, String path) {
        return createMock(api, path, null);
    }

    private Api createMock(String api, String path, String host) {
        Api api1 = mock(Api.class);
        when(api1.getId()).thenReturn(api);
        when(api1.getEnvironmentId()).thenReturn("DEFAULT");
        if (host == null) {
            when(api1.getDefinition())
                .thenReturn("{\"id\": \"" + api + "\",\"name\": \"API 1\",\"proxy\": {\"context_path\": \"" + path + "\"}}");
        } else {
            when(api1.getDefinition())
                .thenReturn(
                    "{\"id\": \"" +
                    api +
                    "\",\"name\": \"API 1\",\"proxy\": {\"virtual_hosts\": [{\"host\": \"api.gravitee.io\", \"path\": \"" +
                    path +
                    "\"}]}}"
                );
        }

        return api1;
    }

    private VirtualHost getValidVirtualHost() {
        VirtualHost vhost = new VirtualHost();
        vhost.setHost("valid.host.gravitee.io");
        vhost.setPath("/validVhostPath");
        return vhost;
    }
}
