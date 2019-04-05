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

import io.gravitee.definition.model.EndpointType;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.endpoint.factory.template.EndpointContext;
import io.gravitee.gateway.http.endpoint.HttpEndpointFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpEndpointFactoryTest {

    @Mock
    private HttpEndpoint endpoint;
    
	@Mock
	ApplicationContext appContext;
	
	@Mock 
	ClassLoader classLoader;

    private HttpEndpointFactory factory = new HttpEndpointFactory();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ApplicationContext appContext = mock(ApplicationContext.class);
        AutowireCapableBeanFactory autowireCapableBeanFactory = mock(AutowireCapableBeanFactory.class);

        when(appContext.getAutowireCapableBeanFactory()).thenReturn(autowireCapableBeanFactory);
        doAnswer(invocation -> invocation.getArguments()[0]).when(autowireCapableBeanFactory).autowireBean(any());
        
        when(appContext.getClassLoader()).thenReturn(classLoader);

        factory.setApplicationContext(appContext);
    }

    @Test
    public void shouldSupportHttpEndpoint() {
        when(endpoint.getType()).thenReturn(EndpointType.HTTP);

        boolean supports = factory.support(endpoint.getType());
        Assert.assertTrue(supports);
    }

    @Test
    public void shouldResolveHttpEndpoint_nullContext() {
        when(endpoint.getTarget()).thenReturn("http://mydomain");
        when(endpoint.getType()).thenReturn(EndpointType.HTTP);

        io.gravitee.gateway.http.endpoint.HttpEndpoint endpoint = factory.create(this.endpoint, null);
        Assert.assertNotNull(endpoint);
    }

    @Test
    public void shouldResolveHttpEndpoint_emptyContext() {
        when(endpoint.getTarget()).thenReturn("http://mydomain");
        when(endpoint.getType()).thenReturn(EndpointType.HTTP);

        EndpointContext context = new EndpointContext();

        io.gravitee.gateway.http.endpoint.HttpEndpoint endpoint = factory.create(this.endpoint, context);
        Assert.assertNotNull(endpoint);
    }

    @Test
    public void shouldResolveHttpEndpoint_targetVariable() {
        HttpEndpoint endpointDef = new HttpEndpoint("default", "{#properties['my_property']}");

        EndpointContext context = new EndpointContext();

        final Map<String, Object> properties = new HashMap<>();
        properties.put("my_property", "http://localhost:8082");
        context.setProperties(properties);

        io.gravitee.gateway.http.endpoint.HttpEndpoint endpoint = factory.create(endpointDef, context);
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(properties.get("my_property") + "/", endpoint.target());
    }
    
    @Test
	public void shouldResolveHttpEndpoint_dictionaryVariable() throws ClassNotFoundException {
		final String dictionaryVariableName = "dictionaries";
    	final String dictionaryKey = "my_server_config";
		final String dictionaryEntryKey = "url";
		final String dictionaryEntryValue = "https://my-server.company";
		final String dictionaryExpression = String.format("{#%s['%s']['%s']}", dictionaryVariableName, dictionaryKey, dictionaryEntryKey);
		
		HttpEndpoint endpointUsingDictionary = new HttpEndpoint("default", dictionaryExpression);
		
		doReturn(TemplateVariableProvider.class).when(classLoader).loadClass(any(String.class));
		
		when(appContext.getBean(TemplateVariableProvider.class)).thenReturn(new TemplateVariableProvider() {

			@Override
			public void provide(TemplateContext context) {
				Map<String, Map<String, String>> values = new HashMap<>();
				Map<String, String> value = new HashMap<String, String>();
				value.put(dictionaryEntryKey, dictionaryEntryValue);
				values.put(dictionaryKey, value);
				context.setVariable("dictionaries", values);
			}
		});

		EndpointContext context = new EndpointContext();
		io.gravitee.gateway.http.endpoint.HttpEndpoint httpEndpoint = factory.create(endpointUsingDictionary, context);
		Assert.assertNotNull(httpEndpoint);
		Assert.assertEquals(dictionaryEntryValue + "/", httpEndpoint.target());
	}
}
