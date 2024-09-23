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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProvider;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
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
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
public class DynamicPropertiesServiceTest {

    private static final String ORGANIZATION_ID = "d7794b03-cda5-47c3-a9d4-3960380edb3a";
    private static final String ENVIRONMENT_ID = "a445364b-9573-44dd-a89e-6920d41b1dcd";

    @Mock
    private ApiService apiService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Vertx vertx;

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
    @BeforeEach
    public void beforeEach() {
        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        lenient().when(environmentService.findById(environment.getId())).thenReturn(environment);
        lenient().doCallRealMethod().when(apiConverter).toApiEntity(any(), any());
        lenient()
            .when(objectMapper.readValue(any(String.class), (Class<Object>) any()))
            .thenAnswer(i -> graviteeMapper.readValue((String) i.getArgument(0), (Class<io.gravitee.definition.model.Api>) i.getArgument(1))
            );
    }

    @Test
    public void should_start_scheduler_when_deploy_api() throws Exception {
        // Used by the Serializer when converting the Api to ApiEntity
        HttpDynamicPropertyProviderConfiguration providerConfiguration = new HttpDynamicPropertyProviderConfiguration();
        providerConfiguration.setUrl("http://localhost:8080/success");
        providerConfiguration.setSpecification(IOUtils.toString(read("/jolt/specification-value-as-key.json"), Charset.defaultCharset()));
        final Api api = createApi(providerConfiguration);

        cut.onEvent(new SimpleEvent<>(ApiEvent.DEPLOY, api));

        assertThat(cut.schedulers).isNotEmpty();
    }

    @Test
    public void should_not_start_scheduler_when_deploy_stopped_api() {
        final Api api = createApi(new HttpDynamicPropertyProviderConfiguration());
        api.setLifecycleState(LifecycleState.STOPPED);

        cut.onEvent(new SimpleEvent<>(ApiEvent.DEPLOY, api));

        verifyNoInteractions(apiService);
        verifyNoInteractions(vertx);
        assertThat(cut.schedulers).isEmpty();
    }

    @Test
    public void should_stop_scheduler_when_undeploy_api() {
        final Api api = createApi(new HttpDynamicPropertyProviderConfiguration());
        api.setLifecycleState(LifecycleState.STOPPED);

        when(categoryMapper.toCategoryKey(anyString(), any())).thenReturn(Set.of());

        cut.onEvent(new SimpleEvent<>(ApiEvent.UNDEPLOY, api));

        verifyNoInteractions(apiService);
        verifyNoInteractions(vertx);
        assertThat(cut.schedulers).isEmpty();
    }

    @Test
    public void should_restart_when_update_api_and_scheduler_already_running() throws Exception {
        HttpDynamicPropertyProviderConfiguration providerConfiguration = new HttpDynamicPropertyProviderConfiguration();
        providerConfiguration.setUrl("http://localhost:8080/success");
        providerConfiguration.setSpecification(IOUtils.toString(read("/jolt/specification-value-as-key.json"), Charset.defaultCharset()));
        final ApiEntity previous = createApiEntity(providerConfiguration);
        previous.getServices().put(DynamicPropertyService.class, mock(DynamicPropertyService.class));

        DynamicPropertyScheduler scheduler = mock(DynamicPropertyScheduler.class);

        cut.schedulers.put(previous, scheduler);

        // Used by the Serializer when converting the Api to ApiEntity
        final Api api = createApi(providerConfiguration);

        cut.onEvent(new SimpleEvent<>(ApiEvent.UPDATE, api));

        verify(scheduler).cancel();
        assertThat(scheduler).isNotEqualTo(cut.schedulers.get(previous));
    }

    @Test
    public void should_just_start_when_update_api_and_no_scheduler_running() throws Exception {
        // Used by the Serializer when converting the Api to ApiEntity
        HttpDynamicPropertyProviderConfiguration providerConfiguration = new HttpDynamicPropertyProviderConfiguration();
        providerConfiguration.setUrl("http://localhost:8080/success");
        providerConfiguration.setSpecification(IOUtils.toString(read("/jolt/specification-value-as-key.json"), Charset.defaultCharset()));
        final Api api = createApi(providerConfiguration);
        cut.onEvent(new SimpleEvent<>(ApiEvent.UPDATE, api));

        assertThat(cut.schedulers).anySatisfy(((apiEntity, scheduler) -> assertThat(apiEntity.getId()).isEqualTo(api.getId())));
    }

    @Test
    public void should_do_nothing_when_update_api_and_no_changes() throws Exception {
        HttpDynamicPropertyProviderConfiguration providerConfiguration = new HttpDynamicPropertyProviderConfiguration();
        providerConfiguration.setUrl("http://localhost:8080/success");
        final String joltSpec = IOUtils.toString(read("/jolt/specification-value-as-key.json"), Charset.defaultCharset());
        providerConfiguration.setSpecification(joltSpec);

        final ApiEntity previous = createApiEntity(providerConfiguration);
        final DynamicPropertyService previousService = previous.getServices().getDynamicPropertyService();
        final HttpDynamicPropertyProviderConfiguration previousConfiguration = new HttpDynamicPropertyProviderConfiguration();
        previousConfiguration.setUrl("http://localhost:8080/success");
        previousConfiguration.setSpecification(joltSpec);
        previousService.setConfiguration(previousConfiguration);

        // Used by the Serializer when converting the Api to ApiEntity
        final Api api = createApi(providerConfiguration);

        cut.schedulers.put(previous, mock(DynamicPropertyScheduler.class));
        cut.onEvent(new SimpleEvent<>(ApiEvent.UPDATE, api));

        verifyNoInteractions(apiService);
        verifyNoInteractions(vertx);
        assertThat(cut.schedulers).isNotEmpty();
    }

    @Test
    public void should_stop_when_update_api_and_scheduler_already_running() throws Exception {
        HttpDynamicPropertyProviderConfiguration providerConfiguration = new HttpDynamicPropertyProviderConfiguration();
        final ApiEntity previous = createApiEntity(providerConfiguration);
        final Api api = createApi(providerConfiguration);
        final io.gravitee.definition.model.Api apiDefinition = graviteeMapper.readValue(
            api.getDefinition(),
            io.gravitee.definition.model.Api.class
        );
        apiDefinition.getServices().getDynamicPropertyService().setEnabled(false);
        api.setDefinition(graviteeMapper.writeValueAsString(apiDefinition));

        DynamicPropertyScheduler scheduler = mock(DynamicPropertyScheduler.class);

        cut.schedulers.put(previous, scheduler);

        cut.onEvent(new SimpleEvent<>(ApiEvent.UPDATE, api));

        verifyNoInteractions(apiService);
        verifyNoInteractions(vertx);
        assertThat(cut.schedulers).isEmpty();
    }

    @SneakyThrows
    private Api createApi(HttpDynamicPropertyProviderConfiguration providerConfiguration) {
        final Api api = new Api();
        api.setEnvironmentId(ENVIRONMENT_ID);
        api.setId("api#1");
        api.setLifecycleState(LifecycleState.STARTED);
        final Services services = new Services();
        final DynamicPropertyService dynamicPropertyService = new DynamicPropertyService();

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

    private ApiEntity createApiEntity(HttpDynamicPropertyProviderConfiguration providerConfiguration) {
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

    private InputStream read(String resource) {
        return this.getClass().getResourceAsStream(resource);
    }
}
