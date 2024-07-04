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
package io.gravitee.rest.api.services.dynamicproperties;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProvider;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.event.ApiEvent;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicPropertiesServiceTest {

    private static final String ORGANIZATION_ID = "d7794b03-cda5-47c3-a9d4-3960380edb3a";
    private static final String ENVIRONMENT_ID = "a445364b-9573-44dd-a89e-6920d41b1dcd";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Mock
    private EventManager eventManager;

    @Mock
    private ApiService apiService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private Vertx vertx;

    @Mock
    private Node node;

    @Mock
    private HttpDynamicPropertyProviderConfiguration providerConfiguration;

    @Mock
    private Executor executor;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ObjectMapper objectMapper = Mockito.spy(new GraviteeMapper());

    @InjectMocks
    private ApiConverter apiConverter = Mockito.spy(
        new ApiConverter(
            objectMapper,
            mock(PlanService.class),
            mock(FlowService.class),
            categoryMapper,
            mock(ParameterService.class),
            mock(WorkflowService.class)
        )
    );

    @InjectMocks
    private DynamicPropertiesService cut;

    private GraviteeMapper graviteeMapper = new GraviteeMapper();

    @SneakyThrows
    @Before
    public void before() {
        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        when(environmentService.findById(environment.getId())).thenReturn(environment);
        doCallRealMethod().when(apiConverter).toApiEntity(any(), any());
        when(objectMapper.readValue(any(String.class), (Class<Object>) any()))
            .thenAnswer(i -> graviteeMapper.readValue((String) i.getArgument(0), (Class<io.gravitee.definition.model.Api>) i.getArgument(1))
            );
    }

    @Test
    public void shouldStartHandlerWhenDeployApi() throws Exception {
        // Used by the Serializer when converting the Api to ApiEntity
        when(providerConfiguration.getUrl()).thenReturn("http://localhost:" + wireMockRule.port() + "/success");
        when(providerConfiguration.getMethod()).thenReturn(HttpMethod.GET);
        when(providerConfiguration.getSpecification())
            .thenReturn(IOUtils.toString(read("/jolt/specification-value-as-key.json"), Charset.defaultCharset()));
        final Api api = createApi();

        when(httpClientService.createHttpClient(anyString(), anyBoolean())).thenReturn(Vertx.vertx().createHttpClient());
        cut.onEvent(new SimpleEvent<>(ApiEvent.DEPLOY, api));

        assertThat(cut.handlers).isNotEmpty();
    }

    @Test
    public void shouldNotStartTimerWhenDeployStoppedApi() {
        final Api api = createApi();
        api.setLifecycleState(LifecycleState.STOPPED);

        cut.onEvent(new SimpleEvent<>(ApiEvent.DEPLOY, api));

        verifyNoInteractions(apiService);
        verifyNoInteractions(vertx);
        assertThat(cut.handlers).isEmpty();
    }

    @Test
    public void shouldStopTimerWhenUndeployApi() {
        final Api api = createApi();
        api.setLifecycleState(LifecycleState.STOPPED);

        when(categoryMapper.toCategoryKey(anyString(), any())).thenReturn(Set.of());

        cut.onEvent(new SimpleEvent<>(ApiEvent.UNDEPLOY, api));

        verifyNoInteractions(apiService);
        verifyNoInteractions(vertx);
        assertThat(cut.handlers).isEmpty();
    }

    @Test
    public void shouldRestartWhenUpdateApiAndTimerAlreadyRunning() throws Exception {
        final ApiEntity previous = createApiEntity();
        previous.getServices().put(DynamicPropertyService.class, mock(DynamicPropertyService.class));

        CronHandler cronHandler = mock(CronHandler.class);

        cut.handlers.put(previous, cronHandler);

        // Used by the Serializer when converting the Api to ApiEntity
        when(providerConfiguration.getUrl()).thenReturn("http://localhost:" + wireMockRule.port() + "/success");
        when(providerConfiguration.getMethod()).thenReturn(HttpMethod.GET);
        when(providerConfiguration.getSpecification())
            .thenReturn(IOUtils.toString(read("/jolt/specification-value-as-key.json"), Charset.defaultCharset()));
        final Api api = createApi();

        when(httpClientService.createHttpClient(anyString(), anyBoolean())).thenReturn(Vertx.vertx().createHttpClient());
        cut.onEvent(new SimpleEvent<>(ApiEvent.UPDATE, api));

        verify(cronHandler).cancel();
        assertThat(cronHandler).isNotEqualTo(cut.handlers.get(previous));
    }

    @Test
    public void shouldJustStartWhenUpdateApiAndNoTimerRunning() throws Exception {
        // Used by the Serializer when converting the Api to ApiEntity
        when(providerConfiguration.getUrl()).thenReturn("http://localhost:" + wireMockRule.port() + "/success");
        when(providerConfiguration.getMethod()).thenReturn(HttpMethod.GET);
        when(providerConfiguration.getSpecification())
            .thenReturn(IOUtils.toString(read("/jolt/specification-value-as-key.json"), Charset.defaultCharset()));
        final Api api = createApi();
        when(httpClientService.createHttpClient(anyString(), anyBoolean())).thenReturn(Vertx.vertx().createHttpClient());
        cut.onEvent(new SimpleEvent<>(ApiEvent.UPDATE, api));

        assertThat(cut.handlers).anySatisfy(((apiEntity, cronHandler) -> assertThat(apiEntity.getId()).isEqualTo(api.getId())));
    }

    @Test
    public void shouldDoNothingWhenUpdateApiAndNoChanges() throws Exception {
        final String joltSpec = IOUtils.toString(read("/jolt/specification-value-as-key.json"), Charset.defaultCharset());
        final ApiEntity previous = createApiEntity();
        final DynamicPropertyService previousService = previous.getServices().getDynamicPropertyService();
        final HttpDynamicPropertyProviderConfiguration previousConfiguration = new HttpDynamicPropertyProviderConfiguration();
        previousConfiguration.setMethod(HttpMethod.GET);
        previousConfiguration.setUrl("http://localhost:" + wireMockRule.port() + "/success");
        previousConfiguration.setSpecification(joltSpec);
        previousService.setConfiguration(previousConfiguration);

        // Used by the Serializer when converting the Api to ApiEntity
        when(providerConfiguration.getUrl()).thenReturn("http://localhost:" + wireMockRule.port() + "/success");
        when(providerConfiguration.getMethod()).thenReturn(HttpMethod.GET);
        when(providerConfiguration.getSpecification()).thenReturn(joltSpec);

        final Api api = createApi();

        cut.handlers.put(previous, mock(CronHandler.class));
        cut.onEvent(new SimpleEvent<>(ApiEvent.UPDATE, api));

        verifyNoInteractions(apiService);
        verifyNoInteractions(vertx);
        assertThat(cut.handlers).isNotEmpty();
    }

    @Test
    public void shouldStopWhenUpdateApiAndTimerAlreadyRunning() throws Exception {
        final ApiEntity previous = createApiEntity();
        final Api api = createApi();
        final io.gravitee.definition.model.Api apiDefinition = graviteeMapper.readValue(
            api.getDefinition(),
            io.gravitee.definition.model.Api.class
        );
        apiDefinition.getServices().getDynamicPropertyService().setEnabled(false);
        api.setDefinition(graviteeMapper.writeValueAsString(apiDefinition));

        CronHandler cronHandler = mock(CronHandler.class);

        cut.handlers.put(previous, cronHandler);

        cut.onEvent(new SimpleEvent<>(ApiEvent.UPDATE, api));

        verifyNoInteractions(apiService);
        verifyNoInteractions(vertx);
        assertThat(cut.handlers).isEmpty();
    }

    @SneakyThrows
    private Api createApi() {
        final Api api = new Api();
        api.setEnvironmentId(ENVIRONMENT_ID);
        final Services services = new Services();
        final DynamicPropertyService dynamicPropertyService = new DynamicPropertyService();

        api.setId("api#1");
        api.setLifecycleState(LifecycleState.STARTED);
        final io.gravitee.definition.model.Api definition = fakeDefinition();
        definition.setServices(services);

        services.put(DynamicPropertyService.class, dynamicPropertyService);

        dynamicPropertyService.setConfiguration(providerConfiguration);
        dynamicPropertyService.setProvider(DynamicPropertyProvider.HTTP);
        dynamicPropertyService.setEnabled(true);
        dynamicPropertyService.setSchedule("*/60 * * * * *");

        api.setDefinition(graviteeMapper.writeValueAsString(definition));
        return api;
    }

    @NotNull
    private static io.gravitee.definition.model.Api fakeDefinition() {
        final io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        final Proxy proxy = new Proxy();
        proxy.setVirtualHosts(List.of(new VirtualHost("/path")));
        definition.setProxy(proxy);
        return definition;
    }

    private ApiEntity createApiEntity() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setEnvironmentId(ENVIRONMENT_ID);
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
