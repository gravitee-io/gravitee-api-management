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
package io.gravitee.rest.api.services.dynamicproperties;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProvider;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.event.ApiEvent;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicPropertiesServiceTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Mock
    private EventManager eventManager;

    @Mock
    private ApiService apiService;

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private Vertx vertx;

    @Mock
    private Node node;

    @Mock
    private HttpDynamicPropertyProviderConfiguration providerConfiguration;

    @InjectMocks
    private DynamicPropertiesService cut;

    public void before() {
        cut.handlers.clear();
    }

    @Test
    public void shouldStartHandlerWhenDeployApi() throws Exception {
        final ApiEntity apiEntity = createApiEntity();

        when(providerConfiguration.getUrl()).thenReturn("http://localhost:" + wireMockRule.port() + "/success");
        when(providerConfiguration.getMethod()).thenReturn(HttpMethod.GET);
        when(providerConfiguration.getSpecification())
            .thenReturn(IOUtils.toString(read("/jolt/specification-value-as-key.json"), Charset.defaultCharset()));
        when(httpClientService.createHttpClient(anyString(), anyBoolean())).thenReturn(Vertx.vertx().createHttpClient());
        cut.onEvent(new SimpleEvent<>(ApiEvent.DEPLOY, apiEntity));

        assertFalse(cut.handlers.isEmpty());
    }

    @Test
    public void shouldNotStartTimerWhenDeployStoppedApi() {
        final ApiEntity apiEntity = createApiEntity();
        apiEntity.setState(Lifecycle.State.STOPPED);

        cut.onEvent(new SimpleEvent<>(ApiEvent.DEPLOY, apiEntity));

        verifyNoInteractions(apiService);
        verifyNoInteractions(vertx);
        assertTrue(cut.handlers.isEmpty());
    }

    @Test
    public void shouldStopTimerWhenUndeployApi() {
        final ApiEntity apiEntity = createApiEntity();
        apiEntity.setState(Lifecycle.State.STOPPED);

        cut.onEvent(new SimpleEvent<>(ApiEvent.UNDEPLOY, apiEntity));

        verifyNoInteractions(apiService);
        verifyNoInteractions(vertx);
        assertTrue(cut.handlers.isEmpty());
    }

    @Test
    public void shouldRestartWhenUpdateApiAndTimerAlreadyRunning() throws Exception {
        final ApiEntity previous = createApiEntity();
        previous.getServices().put(DynamicPropertyService.class, mock(DynamicPropertyService.class));

        final ApiEntity apiEntity = createApiEntity();

        CronHandler cronHandler = mock(CronHandler.class);

        cut.handlers.put(previous, cronHandler);

        when(providerConfiguration.getUrl()).thenReturn("http://localhost:" + wireMockRule.port() + "/success");
        when(providerConfiguration.getMethod()).thenReturn(HttpMethod.GET);
        when(providerConfiguration.getSpecification())
            .thenReturn(IOUtils.toString(read("/jolt/specification-value-as-key.json"), Charset.defaultCharset()));
        when(httpClientService.createHttpClient(anyString(), anyBoolean())).thenReturn(Vertx.vertx().createHttpClient());
        cut.onEvent(new SimpleEvent<>(ApiEvent.UPDATE, apiEntity));

        verify(cronHandler).cancel();
        assertNotEquals(cronHandler, cut.handlers.get(previous));
    }

    @Test
    public void shouldJustStartWhenUpdateApiAndNoTimerRunning() throws Exception {
        final ApiEntity apiEntity = createApiEntity();

        when(providerConfiguration.getUrl()).thenReturn("http://localhost:" + wireMockRule.port() + "/success");
        when(providerConfiguration.getMethod()).thenReturn(HttpMethod.GET);
        when(providerConfiguration.getSpecification())
            .thenReturn(IOUtils.toString(read("/jolt/specification-value-as-key.json"), Charset.defaultCharset()));
        when(httpClientService.createHttpClient(anyString(), anyBoolean())).thenReturn(Vertx.vertx().createHttpClient());
        cut.onEvent(new SimpleEvent<>(ApiEvent.UPDATE, apiEntity));

        assertNotNull(cut.handlers.get(apiEntity));
    }

    @Test
    public void shouldDoNothingWhenUpdateApiAndNoChanges() throws Exception {
        final ApiEntity previous = createApiEntity();
        final ApiEntity apiEntity = createApiEntity();

        cut.handlers.put(previous, mock(CronHandler.class));
        cut.onEvent(new SimpleEvent<>(ApiEvent.UPDATE, apiEntity));

        verifyNoInteractions(apiService);
        verifyNoInteractions(vertx);
        assertFalse(cut.handlers.isEmpty());
    }

    private ApiEntity createApiEntity() {
        final ApiEntity apiEntity = new ApiEntity();
        final Services services = new Services();
        final DynamicPropertyService dynamicPropertyService = new DynamicPropertyService();

        apiEntity.setId("api#1");
        apiEntity.setState(Lifecycle.State.STARTED);
        apiEntity.setServices(services);

        services.put(DynamicPropertyService.class, dynamicPropertyService);

        dynamicPropertyService.setConfiguration(providerConfiguration);
        dynamicPropertyService.setProvider(DynamicPropertyProvider.HTTP);
        dynamicPropertyService.setEnabled(true);
        dynamicPropertyService.setSchedule("*/60 * * * * *");

        return apiEntity;
    }

    private InputStream read(String resource) throws IOException {
        return this.getClass().getResourceAsStream(resource);
    }
}
