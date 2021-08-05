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
package io.gravitee.rest.api.services.dynamicproperties.provider.http;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.rest.api.services.dynamicproperties.provider.http.mapper.JoltMapper;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpProviderTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Mock
    private DynamicPropertyService dynamicPropertyService;

    @Mock
    private HttpDynamicPropertyProviderConfiguration providerConfiguration;

    @Mock
    private JoltMapper mapper;

    @Mock
    private Node node;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldGetProperties() throws IOException {
        when(dynamicPropertyService.getConfiguration()).thenReturn(providerConfiguration);
        when(providerConfiguration.getUrl()).thenReturn("http://localhost:" + wireMockRule.port() + "/success");
        when(providerConfiguration.getSpecification())
            .thenReturn(IOUtils.toString(read("/jolt/specification.json"), Charset.defaultCharset()));
        when(providerConfiguration.getMethod()).thenReturn(HttpMethod.GET);

        HttpProvider provider = new HttpProvider(dynamicPropertyService);
        provider.setMapper(mapper);
        provider.setVertx(Vertx.vertx());

        CompletableFuture<Collection<DynamicProperty>> future = provider.get();
        Collection<DynamicProperty> dynamicProperties = future.join();

        assertNotNull(dynamicProperties);

        verify(mapper, times(1)).map(anyString());
    }

    @Test
    public void shouldGetPropertiesFromPOST() throws IOException {
        when(dynamicPropertyService.getConfiguration()).thenReturn(providerConfiguration);
        when(providerConfiguration.getUrl()).thenReturn("http://localhost:" + wireMockRule.port() + "/success_post");
        when(providerConfiguration.getSpecification())
            .thenReturn(IOUtils.toString(read("/jolt/specification.json"), Charset.defaultCharset()));
        when(providerConfiguration.getMethod()).thenReturn(HttpMethod.GET);
        when(providerConfiguration.getBody()).thenReturn("{}");

        HttpProvider provider = new HttpProvider(dynamicPropertyService);
        provider.setMapper(mapper);
        provider.setVertx(Vertx.vertx());

        CompletableFuture<Collection<DynamicProperty>> future = provider.get();
        Collection<DynamicProperty> dynamicProperties = future.join();

        assertNotNull(dynamicProperties);

        verify(mapper, times(1)).map(anyString());
    }

    @Test
    public void shouldGetNullPropertiesBecauseHttpError() throws IOException {
        when(dynamicPropertyService.getConfiguration()).thenReturn(providerConfiguration);
        when(providerConfiguration.getUrl()).thenReturn("http://localhost:" + wireMockRule.port() + "/error");
        when(providerConfiguration.getSpecification())
            .thenReturn(IOUtils.toString(read("/jolt/specification.json"), Charset.defaultCharset()));
        when(providerConfiguration.getMethod()).thenReturn(HttpMethod.GET);

        HttpProvider provider = new HttpProvider(dynamicPropertyService);
        provider.setMapper(mapper);
        provider.setVertx(Vertx.vertx());

        CompletableFuture<Collection<DynamicProperty>> future = provider.get();
        Collection<DynamicProperty> dynamicProperties = future.join();

        assertNull(dynamicProperties);

        verify(mapper, never()).map(anyString());
    }

    @Test(expected = CompletionException.class)
    public void shouldCallUnknownUri() throws IOException {
        when(dynamicPropertyService.getConfiguration()).thenReturn(providerConfiguration);
        when(providerConfiguration.getUrl()).thenReturn("http://unknown_host:" + wireMockRule.port());
        when(providerConfiguration.getSpecification())
            .thenReturn(IOUtils.toString(read("/jolt/specification.json"), Charset.defaultCharset()));
        when(providerConfiguration.getMethod()).thenReturn(HttpMethod.GET);

        HttpProvider provider = new HttpProvider(dynamicPropertyService);
        provider.setMapper(mapper);
        provider.setVertx(Vertx.vertx());

        CompletableFuture<Collection<DynamicProperty>> future = provider.get();
        future.join();
    }

    private InputStream read(String resource) throws IOException {
        return this.getClass().getResourceAsStream(resource);
    }
}
