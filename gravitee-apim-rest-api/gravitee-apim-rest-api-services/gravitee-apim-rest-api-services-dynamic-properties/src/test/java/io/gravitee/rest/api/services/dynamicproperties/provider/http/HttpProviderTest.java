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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProvider;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith({ MockitoExtension.class, VertxExtension.class })
public class HttpProviderTest {

    @RegisterExtension
    private final WireMockExtension wiremock = WireMockExtension
        .newInstance()
        .options(WireMockConfiguration.wireMockConfig().dynamicPort())
        .build();

    private HttpDynamicPropertyProviderConfiguration providerConfiguration;
    private HttpProvider provider;

    @Mock
    private HttpClientService httpClientService;

    @BeforeEach
    public void setUp(Vertx vertx) {
        providerConfiguration = new HttpDynamicPropertyProviderConfiguration();
        when(httpClientService.createHttpClient(anyString(), anyBoolean())).thenReturn(vertx.createHttpClient());
    }

    @Test
    public void should_get_properties() throws IOException {
        providerConfiguration.setUrl("http://localhost:" + wiremock.getPort() + "/success");
        providerConfiguration.setSpecification(getSimpleJoltSpecification());
        providerConfiguration.setMethod(HttpMethod.GET);
        setUpProvider();

        provider
            .get()
            .subscribeOn(Schedulers.io())
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValue(dynamicProperties -> {
                assertThat(dynamicProperties).contains(new DynamicProperty("name", "Elysee"), new DynamicProperty("country", "FRANCE"));
                return true;
            });
    }

    @Test
    public void should_get_properties_from_post() throws IOException {
        providerConfiguration.setUrl("http://localhost:" + wiremock.getPort() + "/success_post");
        providerConfiguration.setSpecification(getSimpleJoltSpecification());
        providerConfiguration.setMethod(HttpMethod.POST);
        providerConfiguration.setBody("{}");
        providerConfiguration.setHeaders(
            List.of(new HttpHeader("Content-Type", "application/json"), new HttpHeader("X-Gravitee-Header", "value"))
        );
        setUpProvider();

        provider
            .get()
            .subscribeOn(Schedulers.io())
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValue(dynamicProperties -> {
                assertThat(dynamicProperties).contains(new DynamicProperty("name", "Elysee"), new DynamicProperty("country", "FRANCE"));
                return true;
            });
    }

    @Test
    public void should_get_null_properties_because_http_error() throws IOException {
        providerConfiguration.setUrl("http://localhost:" + wiremock.getPort() + "/error");
        providerConfiguration.setSpecification(getSimpleJoltSpecification());
        providerConfiguration.setMethod(HttpMethod.GET);
        setUpProvider();

        provider.get().subscribeOn(Schedulers.io()).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
    }

    @Test
    public void should_throw_an_error_when_calling_unknown_uri() throws IOException {
        providerConfiguration.setUrl("http://unknown_host:" + wiremock.getPort());
        providerConfiguration.setSpecification(getSimpleJoltSpecification());
        providerConfiguration.setMethod(HttpMethod.GET);
        setUpProvider();

        provider.get().subscribeOn(Schedulers.io()).test().awaitDone(5, TimeUnit.SECONDS).assertError(UnknownHostException.class);
    }

    private String getSimpleJoltSpecification() throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream("/jolt/specification-key-value-simple.json"), Charset.defaultCharset());
    }

    private void setUpProvider() {
        DynamicPropertyService dynamicPropertyService = new DynamicPropertyService();
        dynamicPropertyService.setProvider(DynamicPropertyProvider.HTTP);
        dynamicPropertyService.setConfiguration(providerConfiguration);

        provider = new HttpProvider(dynamicPropertyService.getConfiguration(), httpClientService, null);
    }
}
