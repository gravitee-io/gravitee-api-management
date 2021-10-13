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
package io.gravitee.gateway.core.endpoint.resolver;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.endpoint.resolver.ProxyEndpoint;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.core.endpoint.GroupManager;
import io.gravitee.gateway.core.endpoint.lifecycle.LoadBalancedEndpointGroup;
import io.gravitee.gateway.core.endpoint.ref.EndpointReference;
import io.gravitee.gateway.core.endpoint.ref.GroupReference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.gateway.core.endpoint.ref.impl.DefaultReferenceRegister;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ProxyEndpointResolverTest {

    private ProxyEndpointResolver resolver;

    @Spy
    private final ReferenceRegister referenceRegister = new DefaultReferenceRegister();

    @Mock
    private GroupManager groupManager;

    @Mock
    private LoadBalancedEndpointGroup loadBalancedEndpointGroup;

    @Before
    public void setUp() {
        initMocks(this);

        resolver = new ProxyEndpointResolver(referenceRegister, groupManager);

        LoadBalancedEndpointGroup group = mock(LoadBalancedEndpointGroup.class);
        when(group.getName()).thenReturn("default-group");
        referenceRegister.add(new GroupReference(group));
    }

    @Test
    public void shouldResolveUserDefinedEndpointAndKeepLastSlash_selectFirstEndpoint() {
        resolveUserDefinedEndpoint("http://host:8080/test/", "http://host:8080/test/", "endpoint", "http://endpoint:8080/test/");
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_selectFirstEndpoint() {
        resolveUserDefinedEndpoint("http://host:8080/test", "http://host:8080/test", "endpoint", "http://endpoint:8080/test");
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withEncodedTargetURI() {
        resolveUserDefinedEndpoint(
            "http://host:8080/test toto  tété/titi",
            "http://host:8080/test toto  tété/titi",
            "endpoint",
            "http://host:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpointAndKeepLastSlash_withEncodedTargetURI() {
        resolveUserDefinedEndpoint(
            "http://host:8080/test/ toto  tété/titi",
            "http://host:8080/test/ toto  tété/titi",
            "endpoint",
            "http://host:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_startingWithSlash() {
        resolveUserDefinedEndpoint("http://endpoint:8080/test/myendpoint", "/myendpoint", "endpoint", "http://endpoint:8080/test");
    }

    @Test
    public void shouldResolveUserDefinedEndpointAndKeepLastSlash_startingWithSlash() {
        resolveUserDefinedEndpoint("http://endpoint:8080/test/myendpoint", "/myendpoint", "endpoint", "http://endpoint:8080/test/");
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withDynamicRouting() {
        resolveUserDefinedEndpoint("http://host:8080/test", "local:", "local", "http://host:8080/test");
    }

    @Test
    public void shouldResolveUserDefinedEndpointAndKeepLastSlash_withDynamicRouting() {
        resolveUserDefinedEndpoint("http://host:8080/test/", "local:", "local", "http://host:8080/test/");
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withSlashInName() {
        resolveUserDefinedEndpoint("http://host:8080/test", "lo/cal:", "lo/cal", "http://host:8080/test");
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withParenthesisInName() {
        resolveUserDefinedEndpoint("http://host:8080/test", "lo(cal:", "lo(cal", "http://host:8080/test");
    }

    @Test
    // : is forbidden thanks to https://github.com/gravitee-io/issues/issues/1939
    public void shouldResolveUserDefinedEndpoint_withPointsInName() {
        String expectedURI = "http://host:8080/test";
        String requestEndpoint = "lo:cal:";
        String endpointName = "lo:cal";
        String endpointTarget = "http://host:8080/test";

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.name()).thenReturn(endpointName);
        referenceRegister.add(new EndpointReference(endpoint));

        ProxyEndpoint connectorEndpoint = resolver.resolve(requestEndpoint);

        Assert.assertNull(connectorEndpoint);
        verify(endpoint, never()).target();
        verify(endpoint, never()).available();

        verify(groupManager, never()).getDefault();
        verify(loadBalancedEndpointGroup, never()).next();
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withParenthesisInQuery() {
        resolveUserDefinedEndpoint("http://host:8080/test(q=1)", "local:(q=1)", "local", "http://host:8080/test");
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withSlashParenthesisInQuery() {
        resolveUserDefinedEndpoint("http://host:8080/test/(q=1)", "local:/(q=1)", "local", "http://host:8080/test");
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withSpacesInName() {
        resolveUserDefinedEndpoint("http://host:8080/test", "lo cal:", "lo cal", "http://host:8080/test");
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withDashInName() {
        resolveUserDefinedEndpoint("http://host:8080/test", "lo-cal:", "lo-cal", "http://host:8080/test");
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withSlashInTargetAndPath() {
        resolveUserDefinedEndpoint("http://host:8080/method", "local:/method", "local", "http://host:8080/");
    }

    @Test
    public void shouldResolveUserDefinedEndpointAndKeepLastSlash_withSlashInTargetAndPath() {
        resolveUserDefinedEndpoint("http://host:8080/method/", "local:/method/", "local", "http://host:8080/");
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withPrefixAndEncodedTargetURI() {
        resolveUserDefinedEndpoint("http://host:8080/test toto  tété/titi", "local:test toto  tété/titi", "local", "http://host:8080/");
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withEndpointDiscoveryName() {
        resolveUserDefinedEndpoint("http://host:8080/test", "consul#endpoint_id:", "consul#endpoint_id", "http://host:8080/test");
    }

    @Test
    public void shouldUseTheRawPath_withEncodedTargetURI() {
        resolveUserDefinedEndpoint("http://host:8080/foo%2f%3fbar", "http://host:8080/foo%2f%3fbar", "endpoint", "http://host:8080/test");
    }

    @Test
    public void shouldResolveEndpoint_withColonInPath() {
        resolveUserDefinedEndpoint("http://host:8080/foo:", "endpoint:/foo:", "endpoint", "http://host:8080/");
    }

    /*
    @Test
    public void shouldResolveUserDefinedEndpoint_withQueryParamsInTarget() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("endpointParam", "v");
        resolveUserDefinedEndpoint(
                "http://host:8080/test",
                parameters,
                "local:",
                "local",
                "http://host:8080/test?endpointParam=v"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withQueryParamsInDynRout() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("dynroutParam", "v");
        resolveUserDefinedEndpoint(
                "http://host:8080/test",
                parameters,
                "local:?dynroutParam=v",
                "local",
                "http://host:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withQueryParamsEverywhere() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("dynroutParam", "v");
        parameters.add("endpointParam", "v");
        resolveUserDefinedEndpoint(
                "http://host:8080/test",
                parameters,
                "local:?dynroutParam=v",
                "local",
                "http://host:8080/test?endpointParam=v"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withQueryParamsEverywhereAnPath() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("dynroutParam", "v");
        parameters.add("endpointParam", "v");
        resolveUserDefinedEndpoint(
                "http://host:8080/test/my/path",
                parameters,
                "local:/my/path?dynroutParam=v",
                "local",
                "http://host:8080/test?endpointParam=v"
        );
    }
    */

    @Test
    public void shouldResolveUserDefinedEndpointAndKeepLastSlash_selectFirstEndpoint_wss() {
        resolveUserDefinedEndpoint("wss://host:8080/test/", "wss://host:8080/test/", "endpoint", "wss://endpoint:8080/test/");
    }

    private void resolveUserDefinedEndpoint(String expectedURI, String requestEndpoint, String endpointName, String endpointTarget) {
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.name()).thenReturn(endpointName);
        when(endpoint.target()).thenReturn(endpointTarget);
        referenceRegister.add(new EndpointReference(endpoint));

        when(groupManager.getDefault()).thenReturn(loadBalancedEndpointGroup);
        when(loadBalancedEndpointGroup.next()).thenReturn(endpoint);

        ProxyEndpoint proxyEndpoint = resolver.resolve(requestEndpoint);
        Assert.assertNotNull(proxyEndpoint);

        ProxyRequest proxyRequest = proxyEndpoint.createProxyRequest(mock((Request.class)));
        Assert.assertEquals(expectedURI, proxyRequest.uri());
    }
}
