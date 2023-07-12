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
package io.gravitee.rest.api.services.dynamicproperties.provider.http;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProvider;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpProviderTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private HttpDynamicPropertyProviderConfiguration providerConfiguration;
    private HttpProvider provider;

    @Mock
    private HttpClientService httpClientService;

    @Before
    public void setUp() {
        providerConfiguration = new HttpDynamicPropertyProviderConfiguration();

        when(httpClientService.createHttpClient(anyString(), anyBoolean())).thenReturn(Vertx.vertx().createHttpClient());
    }

    @Test
    public void shouldGetProperties() throws IOException {
        providerConfiguration.setUrl("http://localhost:" + wireMockRule.port() + "/success");
        providerConfiguration.setSpecification(getSimpleJoltSpecification());
        providerConfiguration.setMethod(HttpMethod.GET);
        setUpProvider();

        Collection<DynamicProperty> dynamicProperties = provider.get().blockingGet();

        assertThat(dynamicProperties).contains(new DynamicProperty("name", "Elysee"), new DynamicProperty("country", "FRANCE"));
    }

    @Test
    public void shouldGetPropertiesFromPOST() throws IOException {
        providerConfiguration.setUrl("http://localhost:" + wireMockRule.port() + "/success_post");
        providerConfiguration.setSpecification(getSimpleJoltSpecification());
        providerConfiguration.setMethod(HttpMethod.POST);
        providerConfiguration.setBody("{}");
        providerConfiguration.setHeaders(
            List.of(new HttpHeader("Content-Type", "application/json"), new HttpHeader("X-Gravitee-Header", "value"))
        );
        setUpProvider();

        Collection<DynamicProperty> dynamicProperties = provider.get().blockingGet();

        assertThat(dynamicProperties).contains(new DynamicProperty("name", "Elysee"), new DynamicProperty("country", "FRANCE"));
    }

    @Test
    public void shouldGetNullPropertiesBecauseHttpError() throws IOException {
        providerConfiguration.setUrl("http://localhost:" + wireMockRule.port() + "/error");
        providerConfiguration.setSpecification(getSimpleJoltSpecification());
        providerConfiguration.setMethod(HttpMethod.GET);
        setUpProvider();

        Collection<DynamicProperty> dynamicProperties = provider.get().blockingGet();

        assertThat(dynamicProperties).isNull();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowAnErrorWhenCallingUnknownUri() throws IOException {
        providerConfiguration.setUrl("http://unknown_host:" + wireMockRule.port());
        providerConfiguration.setSpecification(getSimpleJoltSpecification());
        providerConfiguration.setMethod(HttpMethod.GET);
        setUpProvider();

        Collection<DynamicProperty> dynamicProperties = provider.get().blockingGet();

        assertThat(dynamicProperties).isNull();
    }

    private String getSimpleJoltSpecification() throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream("/jolt/specification-key-value-simple.json"), Charset.defaultCharset());
    }

    private void setUpProvider() {
        DynamicPropertyService dynamicPropertyService = new DynamicPropertyService();
        dynamicPropertyService.setProvider(DynamicPropertyProvider.HTTP);
        dynamicPropertyService.setConfiguration(providerConfiguration);

        provider = new HttpProvider(dynamicPropertyService);
        provider.setHttpClientService(httpClientService);
        provider.setExecutor(Executors.newSingleThreadExecutor());
    }
}
