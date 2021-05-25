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
package io.gravite.gateway.http.endpoint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.EndpointType;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.core.endpoint.ManagedEndpoint;
import io.gravitee.gateway.core.endpoint.factory.template.EndpointContext;
import io.gravitee.gateway.http.endpoint.HttpEndpointFactory;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ManagedEndpointFactoryTest {

    @Mock
    private HttpEndpoint endpoint;

    private HttpEndpointFactory factory = new HttpEndpointFactory();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ApplicationContext context = mock(ApplicationContext.class);
        AutowireCapableBeanFactory autowireCapableBeanFactory = mock(AutowireCapableBeanFactory.class);

        when(context.getAutowireCapableBeanFactory()).thenReturn(autowireCapableBeanFactory);
        doAnswer(invocation -> invocation.getArguments()[0]).when(autowireCapableBeanFactory).autowireBean(any());

        factory.setApplicationContext(context);
    }

    @Test
    public void shouldSupportHttpEndpoint() {
        when(endpoint.getType()).thenReturn(EndpointType.HTTP);

        boolean supports = factory.support(endpoint);
        Assert.assertTrue(supports);
    }

    @Test
    public void shouldResolveHttpEndpoint_nullContext() {
        when(endpoint.getTarget()).thenReturn("http://mydomain");
        when(endpoint.getType()).thenReturn(EndpointType.HTTP);

        ManagedEndpoint endpoint = factory.create(this.endpoint, null);
        Assert.assertNotNull(endpoint);
    }

    @Test
    public void shouldResolveHttpEndpoint_emptyContext() {
        when(endpoint.getTarget()).thenReturn("http://mydomain");
        when(endpoint.getType()).thenReturn(EndpointType.HTTP);

        EndpointContext context = new EndpointContext();

        ManagedEndpoint endpoint = factory.create(this.endpoint, context);
        Assert.assertNotNull(endpoint);
    }

    @Test
    public void shouldResolveHttpEndpoint_targetVariable() {
        HttpEndpoint endpointDef = new HttpEndpoint("default", "{#properties['my_property']}");

        EndpointContext context = new EndpointContext();

        final Map<String, Object> properties = new HashMap<>();
        properties.put("my_property", "http://localhost:8082");
        context.setProperties(properties);

        ManagedEndpoint endpoint = factory.create(endpointDef, context);
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(properties.get("my_property") + "/", endpoint.target());
    }

    @Test
    public void shouldCreateEndpoint_grpcProtocol() {
        HttpEndpoint endpointDef = new HttpEndpoint("default", "grpc://localhost:9876/service");

        ManagedEndpoint endpoint = factory.create(endpointDef, null);
        Assert.assertNotNull(endpoint);
        Assert.assertEquals("grpc://localhost:9876/service", endpoint.target());
    }

    @Test
    public void shouldCreateEndpoint_wsProtocol() {
        HttpEndpoint endpointDef = new HttpEndpoint("default", "ws://localhost:9876/service");

        ManagedEndpoint endpoint = factory.create(endpointDef, null);
        Assert.assertNotNull(endpoint);
        Assert.assertEquals("ws://localhost:9876/service", endpoint.target());
    }

    @Test
    public void shouldCreateEndpoint_encodedEndpoint() {
        HttpEndpoint endpointDef = new HttpEndpoint("default", "http://toto:4567/my_path?test%7Cpipe");

        ManagedEndpoint endpoint = factory.create(endpointDef, null);
        Assert.assertNotNull(endpoint);
        Assert.assertEquals("http://toto:4567/my_path?test%7Cpipe", endpoint.target());
    }
}
