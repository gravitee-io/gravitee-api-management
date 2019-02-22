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
package io.gravitee.gateway.core.endpoint.resolver.impl;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.core.endpoint.GroupManager;
import io.gravitee.gateway.core.endpoint.lifecycle.LoadBalancedEndpointGroup;
import io.gravitee.gateway.core.endpoint.ref.EndpointReference;
import io.gravitee.gateway.core.endpoint.ref.GroupReference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.gateway.core.endpoint.ref.impl.DefaultReferenceRegister;
import io.gravitee.gateway.core.endpoint.resolver.EndpointResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TargetEndpointResolverTest {

    @InjectMocks
    private TargetEndpointResolver resolver;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Request serverRequest;

    @Spy
    private ReferenceRegister referenceRegister = new DefaultReferenceRegister();

    @Mock
    private GroupManager groupManager;

    @Mock
    private LoadBalancedEndpointGroup loadBalancedEndpointGroup;

    @Before
    public void setUp() {
        initMocks(this);

        LoadBalancedEndpointGroup group = mock(LoadBalancedEndpointGroup.class);
        when(group.getName()).thenReturn("default-group");
        referenceRegister.add(new GroupReference(group));
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_selectFirstEndpoint() {
        resolveUserDefinedEndpoint(
                "http://host:8080/test",
                "http://host:8080/test",
                "endpoint",
                "http://endpoint:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withEncodedTargetURI() {
        resolveUserDefinedEndpoint(
                "http://host:8080/test%20toto%20%20t%C3%A9t%C3%A9/titi",
                "http://host:8080/test toto  tété/titi",
                "endpoint",
                "http://host:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_startingWithSlash() {
        resolveUserDefinedEndpoint(
                "http://endpoint:8080/test/myendpoint",
                "/myendpoint",
                "endpoint",
                "http://endpoint:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withDynamicRouting() {
        resolveUserDefinedEndpoint(
                "http://host:8080/test",
                "endpoint:local:",
                "local",
                "http://host:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withSlashInName() {
        resolveUserDefinedEndpoint(
                "http://host:8080/test",
                "endpoint:lo/cal:",
                "lo/cal",
                "http://host:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withParenthesisInName() {
        resolveUserDefinedEndpoint(
                "http://host:8080/test",
                "endpoint:lo(cal:",
                "lo(cal",
                "http://host:8080/test"
        );
    }

    @Test
    @Ignore
    public void shouldResolveUserDefinedEndpoint_withPointsInName() {
        resolveUserDefinedEndpoint(
                "http://host:8080/test",
                "endpoint:lo:cal:",
                "lo:cal",
                "http://host:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withParenthesisInQuery() {
        resolveUserDefinedEndpoint(
                "http://host:8080/test(q=1)",
                "endpoint:local:(q=1)",
                "local",
                "http://host:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withSlashParenthesisInQuery() {
        resolveUserDefinedEndpoint(
                "http://host:8080/test/(q=1)",
                "endpoint:local:/(q=1)",
                "local",
                "http://host:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withSpacesInName() {
        resolveUserDefinedEndpoint(
                "http://host:8080/test",
                "endpoint:lo cal:",
                "lo cal",
                "http://host:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withDashInName() {
        resolveUserDefinedEndpoint(
                "http://host:8080/test",
                "endpoint:lo-cal:",
                "lo-cal",
                "http://host:8080/test"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withSlashInTargetAndPath() {
        resolveUserDefinedEndpoint(
                "http://host:8080/method",
                "endpoint:local:/method",
                "local",
                "http://host:8080/"
        );
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withPrefixAndEncodedTargetURI() {
        resolveUserDefinedEndpoint(
                "http://host:8080/test%20toto%20%20t%C3%A9t%C3%A9/titi",
                "endpoint:local:test toto  tété/titi",
                "local",
                "http://host:8080/"
        );
    }

    private void resolveUserDefinedEndpoint(String expectedURI, String requestEndpoint, String endpointName, String endpointTarget) {
        when(executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT)).thenReturn(requestEndpoint);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.name()).thenReturn(endpointName);
        when(endpoint.target()).thenReturn(endpointTarget);
        when(endpoint.available()).thenReturn(true);
        referenceRegister.add(new EndpointReference(endpoint));

        when(groupManager.getDefault()).thenReturn(loadBalancedEndpointGroup);
        when(loadBalancedEndpointGroup.next()).thenReturn(endpoint);

        EndpointResolver.ResolvedEndpoint resolvedEndpoint = resolver.resolve(serverRequest, executionContext);

        Assert.assertNotNull(resolvedEndpoint);
        Assert.assertEquals(expectedURI, resolvedEndpoint.getUri());
    }
}