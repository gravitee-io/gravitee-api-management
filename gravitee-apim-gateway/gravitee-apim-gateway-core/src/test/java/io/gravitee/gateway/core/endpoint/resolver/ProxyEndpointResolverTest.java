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
package io.gravitee.gateway.core.endpoint.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class ProxyEndpointResolverTest {

    private ProxyEndpointResolver resolver;

    @Spy
    private final ReferenceRegister referenceRegister = new DefaultReferenceRegister();

    @Mock(strictness = Mock.Strictness.LENIENT)
    private GroupManager groupManager;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private LoadBalancedEndpointGroup loadBalancedEndpointGroup;

    @BeforeEach
    public void setUp() {
        resolver = new ProxyEndpointResolver(referenceRegister, groupManager);

        LoadBalancedEndpointGroup group = mock(LoadBalancedEndpointGroup.class);
        when(group.getName()).thenReturn("default-group");
        referenceRegister.add(new GroupReference(group));
    }

    @Test
    public void shouldResolveUserDefinedEndpoint() {
        var endpointName = "my-endpoint";
        var expectedUri = "http://endpoint:8080/test";

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.name()).thenReturn(endpointName);
        when(endpoint.target()).thenReturn(expectedUri);
        referenceRegister.add(new EndpointReference(endpoint));

        ProxyEndpoint proxyEndpoint = resolver.resolve(endpointName);
        assertThat(proxyEndpoint).isNotNull();

        ProxyRequest proxyRequest = proxyEndpoint.createProxyRequest(mock((Request.class)));
        assertThat(proxyRequest.uri()).isEqualTo(expectedUri);
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withPath() {
        var endpointName = "my-endpoint";
        var endpointTarget = "http://endpoint:8080/test";

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.name()).thenReturn(endpointName);
        when(endpoint.target()).thenReturn(endpointTarget);
        referenceRegister.add(new EndpointReference(endpoint));

        ProxyEndpoint proxyEndpoint = resolver.resolve(endpointName + ":/echo");
        assertThat(proxyEndpoint).isNotNull();

        ProxyRequest proxyRequest = proxyEndpoint.createProxyRequest(mock((Request.class)));
        assertThat(proxyRequest.uri()).isEqualTo(endpointTarget + "/echo");
    }

    @Test
    // : is forbidden thanks to https://github.com/gravitee-io/issues/issues/1939
    public void shouldResolveUserDefinedEndpoint_withPointsInName() {
        String requestEndpoint = "lo:cal:";
        String endpointName = "lo:cal";

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.name()).thenReturn(endpointName);
        referenceRegister.add(new EndpointReference(endpoint));

        ProxyEndpoint connectorEndpoint = resolver.resolve(requestEndpoint);

        assertThat(connectorEndpoint).isNull();
        verify(endpoint, never()).target();
        verify(endpoint, never()).available();

        verify(groupManager, never()).getDefault();
        verify(loadBalancedEndpointGroup, never()).next();
    }

    @ParameterizedTest
    @CsvSource(
        delimiterString = "|",
        textBlock = """
        http://endpoint:8080/test/myendpoint|/myendpoint|endpoint|http://endpoint:8080/test
        http://endpoint:8080/test/myendpoint|/myendpoint|endpoint|http://endpoint:8080/test/
        http://host:8080/test|local:|local|http://host:8080/test
        http://host:8080/test/|local:|local|http://host:8080/test/
        http://host:8080/test|lo/cal:|lo/cal|http://host:8080/test
        http://host:8080/test|lo(cal:|lo(cal|http://host:8080/test
        http://host:8080/test/|http://host:8080/test/|endpoint|http://endpoint:8080/test/
        http://host:8080/test|http://host:8080/test|endpoint|http://endpoint:8080/test
        http://host:8080/test(q=1)|local:(q=1)|local|http://host:8080/test
        http://host:8080/test/(q=1)|local:/(q=1)|local|http://host:8080/test
        http://host:8080/test|lo cal:|lo cal|http://host:8080/test
        http://host:8080/test|lo-cal:|lo-cal|http://host:8080/test
        http://host:8080/method|local:/method|local|http://host:8080/
        http://host:8080/method/|local:/method/|local|http://host:8080/
        http://host:8080/test toto  tété/titi|local:test toto  tété/titi|local|http://host:8080/
        http://host:8080/test|consul#endpoint_id:|consul#endpoint_id|http://host:8080/test
        http://host:8080/foo%2f%3fbar|http://host:8080/foo%2f%3fbar|endpoint|http://host:8080/test
        http://host:8080/foo:|endpoint:/foo:|endpoint|http://host:8080/
        wss://host:8080/test/|wss://host:8080/test/|endpoint|wss://endpoint:8080/test/
        http://host:8080/test toto  tété/titi|http://host:8080/test toto  tété/titi|endpoint|http://host:8080/test
        http://host:8080/test/ toto  tété/titi|http://host:8080/test/ toto  tété/titi|endpoint|http://host:8080/test
        """
    )
    void resolveUserDefinedEndpoint(String expectedURI, String requestEndpoint, String endpointName, String endpointTarget) {
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.name()).thenReturn(endpointName);
        when(endpoint.target()).thenReturn(endpointTarget);
        referenceRegister.add(new EndpointReference(endpoint));

        when(groupManager.getDefault()).thenReturn(loadBalancedEndpointGroup);
        when(loadBalancedEndpointGroup.next()).thenReturn(endpoint);

        ProxyEndpoint proxyEndpoint = resolver.resolve(requestEndpoint);
        assertThat(proxyEndpoint).isNotNull();

        ProxyRequest proxyRequest = proxyEndpoint.createProxyRequest(mock((Request.class)));
        assertThat(proxyRequest.uri()).isEqualTo(expectedURI);
    }
}
