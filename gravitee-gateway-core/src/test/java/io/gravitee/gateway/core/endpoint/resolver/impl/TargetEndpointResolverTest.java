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
import io.gravitee.gateway.core.endpoint.ref.EndpointReference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.gateway.core.endpoint.resolver.EndpointResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @Mock
    private ReferenceRegister referenceRegister;

    @Mock
    private GroupManager groupManager;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_selectFirstEndpoint() {
        final String targetUri = "http://host:8080/test";

        when(executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT)).thenReturn(targetUri);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.name()).thenReturn("endpoint");
        when(endpoint.target()).thenReturn("http://endpoint:8080/test");
        when(endpoint.available()).thenReturn(true);

        when(referenceRegister.referencesByType(EndpointReference.class))
                .thenAnswer(invocation -> Collections.singleton(new EndpointReference(endpoint)));

        EndpointResolver.ResolvedEndpoint resolvedEndpoint = resolver.resolve(serverRequest, executionContext);

        Assert.assertEquals(targetUri, resolvedEndpoint.getUri());
    }

    @Test
    public void shouldResolveUserDefinedEndpoint_withEncodedTargetURI() {
        final String targetUri = "http://host:8080/test toto  tété/titi";
        final String expectedTargetUri = "http://host:8080/test%20toto%20%20t%C3%A9t%C3%A9/titi";

        when(executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT)).thenReturn(targetUri);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.name()).thenReturn("endpoint");
        when(endpoint.target()).thenReturn("http://host:8080/test");
        when(endpoint.available()).thenReturn(true);

        when(referenceRegister.referencesByType(EndpointReference.class))
                .thenAnswer(invocation -> Collections.singleton(new EndpointReference(endpoint)));

        EndpointResolver.ResolvedEndpoint resolvedEndpoint = resolver.resolve(serverRequest, executionContext);

        Assert.assertEquals(expectedTargetUri, resolvedEndpoint.getUri());
    }
}
